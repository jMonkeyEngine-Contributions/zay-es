/*
 * $Id: RemoteEntityData.java 1581 2015-03-01 07:30:19Z PSpeed42@gmail.com $
 *
 * Copyright (c) 2013 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.simsilica.es.client;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jme3.network.Client;
import com.jme3.network.Message;

import com.simsilica.es.*;
import com.simsilica.es.base.DefaultEntity;
import com.simsilica.es.base.DefaultEntitySet;
import com.simsilica.es.base.DefaultWatchedEntity;

import com.simsilica.es.net.*;
import com.simsilica.es.net.EntityDataMessage.ComponentData;


/**
 *  An implementation of the EntityData interface that communicates
 *  with a remote server to provide entity and component access.
 *  This EntityData implementation is read only.  Any methods that
 *  modify entities will throw UnsupportedOperationException.
 *
 *  <p>Note: EntitySets returned by this implementation will behave
 *  slightly different than from their local counterparts.  It is
 *  still 100% within the contract of EnitySet and properly written
 *  code won't have an issue.  In a local-access situation, retrieving
 *  the EntitySet will also populate it.  In this implementation, the
 *  data arrives asynchronously as if entities have been added during
 *  applyChanges().
 *  </p>
 *
 *  @author    Paul Speed
 */
public class RemoteEntityData implements EntityData {

    static Logger log = LoggerFactory.getLogger(RemoteEntityData.class);

    /**
     *  Keeps track of the next ID for a remote entity set.
     */
    private static final AtomicInteger nextSetId = new AtomicInteger();

    /**
     *  Keeps track of the next ID for a remote watched entity.
     */
    private static final AtomicInteger nextWatchId = new AtomicInteger();

    /**
     *  Keeps track of the next ID used for requests that return
     *  results... especially when the caller will be waiting for them.
     */
    private static final AtomicInteger nextRequestId = new AtomicInteger();

    private final Client client;
    private final int channel;

    /**
     *  Track the time of the last EntityChange message we've
     *  received.  When mining the EntitySets for cached components,
     *  this is the value that will be used for data currency when
     *  the component value is pulled from the unprocessed change
     *  sets.
     */
    private volatile long lastChangeReceived;

    /**
     *  Holds the blocked requests that are pending.
     */
    private final Map<Integer,PendingRequest> pendingRequests = new ConcurrentHashMap<>();

    /**
     *  The active EntitySets that have been requested by the user
     *  but not yet released.  Incoming changes and updates are applied
     *  to these sets by setId.
     */
    private final Map<Integer,RemoteEntitySet> activeSets = new ConcurrentHashMap<>();

    /**
     *  The active watched entities.  We don't support 'observability' on the remote
     *  entity data at this point and I'd rather not make that decisions in haste.
     *  But WatchedEntities will need to be updated just the same.
     */
    private final Map<Integer,RemoteWatchedEntity> watchedEntities = new ConcurrentHashMap<>();

    private final ObjectMessageDelegator<Client> messageHandler;

    private final RemoteStringIndex strings = new RemoteStringIndex(this);

    /**
     *  Creates a new RemoteEntityData instance that will communicate
     *  over the specified client and channel to provide remote
     *  EntityData access.
     */
    public RemoteEntityData( Client client, int channel ) {
        this.client = client;
        this.channel = channel;
        this.messageHandler = new ObjectMessageDelegator<>(new EntityMessageHandler(), true);
        client.addMessageListener(messageHandler, messageHandler.getMessageTypes());
    }

    @Override
    public <T extends EntityComponent> T getComponent( EntityId entityId, Class<T> type ) {
        if( log.isTraceEnabled() ) {
            log.trace("getComponent(" + entityId + ", " + type + ")");
        }

        // New note: 2015/12/28
        // I'm seeing this called a lot to fill out an entity that shares
        // a changing component with a different view.  For example, position + model
        // and position + avatar.  As the player-character walks around, the client
        // is constantly retrieving the model info to try to fill out the rest of the
        // entity.  But it will never get that information.
        // Given the new approach to network synching, I think that the client
        // shouldn't even try to complete the entity.  It would already have been
        // sent the elevant information it it were an add.  We potentially need to
        // implement a RemoteEntitySet from scratch that is a thinner/dumber client.

//System.out.println("RemoteEntityData.getComponent(" + entityId + ", " + type + ")");
        // This call can happen quite frequently as part of change processing
        // and in some cases it's wasteful.  For example, two EntitySets with
        // Position and ModelType components but one is filtering for a specific
        // ModelType.  Any Position updates that come in will cause it to try
        // and create an entity, fill out the missing ModelType component, and
        // then ultimately reject the entity and move on... only to do the
        // same thing again the next time the position changes.
        //
        // One way around this is to first consult our existing sets to
        // see if they have the component for that entity.  The down side
        // here is that with a naive implementation we could be looking
        // at relatively stale data.  An EntitySet won't really know the
        // latest component value unless it's processed its updates.
        //
        // It's not enough to go through the applied components, the change
        // sets have to be checked as well... otherwise state becomes inconsistent.
        // To continue the above example, if the first EntitySet hasn't processed
        // its changes yet then it might have an old ModelType.  The second
        // set would base its decision on old data and that decision wouldn't
        // get readdressed when the first set finally processes its changes.
        //
        // Still, going through existing EntitySets and their change sets
        // is still bound to be more efficient than going back to the server.
        // The number of entity sets on a client shouldn't be particularly
        // large and most can trivially reject the request based on component
        // type alone.
        long latest = 0;
        T fromCache = null;
        for( RemoteEntitySet set : activeSets.values() ) {
            if( !set.hasType(type) ) {
                continue;
            }

            T value = set.checkChangeQueue(entityId, type);
            long updateTime = set.lastUpdate;
            if( value != null ) {
                updateTime = lastChangeReceived;
            } else {
                Entity e = set.getEntity(entityId);
                if( e == null ) {
                    continue;
                }
                value = e.get(type);
            }

            // If we found a value then see if it is more recent then
            // any previous value.
            if( updateTime > latest ) {
                latest = updateTime;
                fromCache = value;
                if( log.isTraceEnabled() ) {
                    log.trace("Found cached component from:" + updateTime + " in set for:" + Arrays.asList(set.getTypes()));
                }
            }
        }
        if( fromCache != null ) {
            return fromCache;
        }

        // I raise the log level here because this might be an indication
        // of a performance problem slipping through. (see above)  Unless
        // the user is calling it directly then... no issue.
        if( log.isDebugEnabled() ) {
            log.debug("Retrieving component from server for:" + entityId + " type:" + type);
        }
        Entity entity = getEntity( entityId, type );
        return entity.get(type);
    }

    @Override
    public Entity getEntity( EntityId entityId, Class... types ) {
        if( log.isTraceEnabled() ) {
            log.trace("getEntity(" + entityId + ", " + Arrays.asList(types) + ")", new Throwable());
        }
//log.info("getEntity(" + entityId + ", " + Arrays.asList(types) + ")", new Throwable());
        // Ignore caching for the moment...

        // Need to fetch the entity
        int id = nextRequestId.getAndIncrement();
        GetComponentsMessage msg = new GetComponentsMessage(id, entityId, types);
        msg.setReliable(true);

        // Setup the 'pending' request tracking so that we
        // can wait for the response.  Just in case, always make sure to
        // do this before sending the message.
        PendingEntityRequest request = new PendingEntityRequest(msg);
        pendingRequests.put(id, request);

        // Now send the message.
        client.send(channel, msg);

        Entity result;
        try {
            // Wait for the response
            result = request.getResult();
        } catch( InterruptedException e ) {
            throw new RuntimeException("Interrupted waiting for entity data.", e);
        }

        if( log.isTraceEnabled() ) {
            log.trace("result:" + result);
        }
        return result;
    }

    @Override
    public EntityId findEntity( ComponentFilter filter, Class... types ) {
        if( log.isTraceEnabled() ) {
            log.trace("findEntity(" + filter + ", " + Arrays.asList(types) + ")");
        }
        return findEntity(new EntityCriteria().set(filter, types));
    }

    @Override
    public EntityId findEntity( EntityCriteria criteria ) {
        if( log.isTraceEnabled() ) {
            log.trace("findEntity(" + criteria + ")");
        }
        // Need to fetch the entity
        int id = nextRequestId.getAndIncrement();
        FindEntityMessage msg = new FindEntityMessage(id, criteria);
        msg.setReliable(true);

        // Setup the 'pending' request tracking so that we
        // can wait for the response.  Just in case, always make sure to
        // do this before sending the message.
        PendingEntityIdsRequest request = new PendingEntityIdsRequest(msg);
        pendingRequests.put(id, request);

        // Now send the message.
        client.send(channel, msg);

        EntityId[] result;
        try {
            // Wait for the response
            result = request.getResult();
        } catch( InterruptedException e ) {
            throw new RuntimeException("Interrupted waiting for entity data.", e);
        }

        if( log.isTraceEnabled() ) {
            EntityId returning = (result != null && result.length > 0) ? result[0] : null;
            log.trace("result:" + returning);
        }
        return (result != null && result.length > 0) ? result[0] : null;
    }

    @Override
    public Set<EntityId> findEntities( ComponentFilter filter, Class... types ) {
        if( log.isTraceEnabled() ) {
            log.trace("findEntities(" + filter + ", " + Arrays.asList(types) + ")");
        }
        return findEntities(new EntityCriteria().set(filter, types));
    }

    @Override
    public Set<EntityId> findEntities( EntityCriteria criteria ) {
        // Need to fetch the entity
        int id = nextRequestId.getAndIncrement();
        FindEntitiesMessage msg = new FindEntitiesMessage(id, criteria);
        msg.setReliable(true);

        // Setup the 'pending' request tracking so that we
        // can wait for the response.  Just in case, always make sure to
        // do this before sending the message.
        PendingEntityIdsRequest request = new PendingEntityIdsRequest(msg);
        pendingRequests.put(id, request);

        // Now send the message.
        client.send(channel, msg);

        Set<EntityId> result = new HashSet<>();
        try {
            // Wait for the response
            EntityId[] ids = request.getResult();
            if( ids != null ) {
                result.addAll(Arrays.asList(ids));
            }
        } catch( InterruptedException e ) {
            throw new RuntimeException("Interrupted waiting for entity data.", e);
        }

        if( log.isTraceEnabled() ) {
            log.trace("result:" + result);
        }
        return result;
    }

    @Override
    public Query createQuery( EntityCriteria criteria ) {
        return new Query() {
                @Override
                public Set<EntityId> execute() {
                    return findEntities(criteria);
                }

                @Override
                public EntityId findFirst() {
                    return findEntity(criteria);
                }
            };
    }

    @Override
    public <T extends EntityComponent> Query createQuery( ComponentFilter<T> filter, Class<T> type ) {
        return new Query() {
                @Override
                public Set<EntityId> execute() {
                    return findEntities(filter, type);
                }

                @Override
                public EntityId findFirst() {
                    return findEntity(filter, type);
                }
            };
    }

    @Override
    @SuppressWarnings("unchecked")
    public EntitySet getEntities( Class... types ) {
        return getEntities(new EntityCriteria().add(types));
    }

    @Override
    @SuppressWarnings("unchecked")
    public EntitySet getEntities( ComponentFilter filter, Class... types ) {
        return getEntities(new EntityCriteria().set(filter, types));
    }

    @Override
    public EntitySet getEntities( EntityCriteria criteria ) {
        if( log.isTraceEnabled() ) {
            log.trace("getEntities(" + criteria + ")");
        }

        int id = nextSetId.getAndIncrement();
        RemoteEntitySet result = new RemoteEntitySet(id, criteria);

        // Make sure we register the entity set before sending the message...
        activeSets.put(id, result);

        // Send a message to ask the server to start watching this
        // for us... and to send an initial data set asynchronously.
        Message m = new GetEntitySetMessage(id, criteria);
        m.setReliable(true);
        client.send(channel, m);

        if( log.isTraceEnabled() ) {
            log.trace("result:" + result);
        }
        return result;
    }

    @Override
    public WatchedEntity watchEntity( EntityId entityId, Class... types ) {

        // Need to fetch the entity
        int watchId = nextWatchId.getAndIncrement();
        int msgId = nextRequestId.getAndIncrement();
        WatchEntityMessage msg = new WatchEntityMessage(msgId, watchId, entityId, types);
        msg.setReliable(true);

        // Setup the 'pending' request tracking so that we
        // can wait for the response.  Just in case, always make sure to
        // do this before sending the message.
        PendingWatchEntityRequest request = new PendingWatchEntityRequest(msg);
        pendingRequests.put(msgId, request);

        // Now send the message.
        client.send(channel, msg);

        WatchedEntity result;
        try {
            // Wait for the response
            result = request.getResult();
        } catch( InterruptedException e ) {
            throw new RuntimeException("Interrupted waiting for watched entity data.", e);
        }

        if( log.isTraceEnabled() ) {
            log.trace("result:" + result);
        }
        return result;
    }

    @Override
    public void close() {
        client.removeMessageListener(messageHandler, messageHandler.getMessageTypes());
    }

    protected StringIdMessage getStringResponse( StringIdMessage msg ) {
        int id = msg.getRequestId();
        msg.setReliable(true);

        // Setup response tracking
        PendingStringRequest request = new PendingStringRequest(msg);
        pendingRequests.put(id, request);

        // Now we can send
        client.send(channel, msg);

        try {
            return request.getResult();
        } catch( InterruptedException e ) {
            throw new RuntimeException("Interrupted waiting for string data.", e);
        }
    }

    protected Integer getStringId( String s ) {
        return getStringResponse(new StringIdMessage(nextRequestId.getAndIncrement(), s)).getId();
    }

    protected String getString( int id ) {
        return getStringResponse(new StringIdMessage(nextRequestId.getAndIncrement(), id)).getString();
    }

    @Override
    public StringIndex getStrings() {
        return strings;
    }

    @Override
    public EntityId createEntity() {
        throw new UnsupportedOperationException("RemoteEntityData is read-only.");
    }

    @Override
    public void removeEntity( EntityId entityId ) {
        throw new UnsupportedOperationException("RemoteEntityData is read-only.");
    }

    @Override
    public void setComponent( EntityId entityId, EntityComponent component ) {
        throw new UnsupportedOperationException("RemoteEntityData is read-only.");
    }

    @Override
    public void setComponents( EntityId entityId, EntityComponent... components ) {
        throw new UnsupportedOperationException("RemoteEntityData is read-only.");
    }

    @Override
    public boolean removeComponent( EntityId entityId, Class type ) {
        throw new UnsupportedOperationException("RemoteEntityData is read-only.");
    }

    @Override
    public void removeComponents( EntityId entityId, Class... types ) {
        throw new UnsupportedOperationException("RemoteEntityData is read-only.");
    }

    protected void entityChange( EntityChange change ) {

        for( RemoteEntitySet set : activeSets.values() ) {
            set.entityChange(change);
        }

        for( RemoteWatchedEntity e : watchedEntities.values() ) {
            e.addChange(change);
        }
    }

    private class RemoteEntitySet extends DefaultEntitySet {

        private final int setId;
        private final ConcurrentLinkedQueue<DefaultEntity> directAdds
                    = new ConcurrentLinkedQueue<>();
        private long lastUpdate;
        private String error;

        public RemoteEntitySet( int setId, ComponentFilter filter, Class<? extends EntityComponent>[] types ) {
            this(setId, new EntityCriteria().set(filter, types));
        }

        public RemoteEntitySet( int setId, EntityCriteria criteria ) {
            super(RemoteEntityData.this, criteria);
            this.setId = setId;
        }

        protected void setError( String error ) {
            this.error = error;
        }

        protected void checkError() {
            if( error != null ) {
                String msg = "Server error received:\n" + error;
                error = null;
                throw new IllegalStateException(msg);
            }
        }

        @Override
        public void release() {
            if( isReleased() ) {
                return;
            }
            super.release();

            if( log.isDebugEnabled() ) {
                log.debug("Releasing set:" + setId );
            }

            activeSets.remove(setId);

            if( client.isConnected() ) {
                ReleaseEntitySetMessage msg = new ReleaseEntitySetMessage(setId);
                client.send(channel, msg);
            }
        }

        @Override
        public String debugId() {
            return "RemoteEntitySet@" + setId;
        }

        @Override
        protected void loadEntities( boolean reload ) {
            // Entities will come in asynchronously.
            checkError();
        }

        @Override
        protected void filtersChanged() {
            checkError();
            // Need to send a message to the server to let it
            // know our interests have changed.
            Message m = new ResetEntitySetFilterMessage(setId, getCriteria());
            m.setReliable(true);

            if( log.isDebugEnabled() ) {
                log.debug( "Sending filter reset:" + m );
            }

            client.send(channel, m);
        }

        @Override
        protected boolean applyChanges( Set<EntityChange> updates, boolean clearChangeSets ) {
            checkError();

            if( super.applyChanges(updates, clearChangeSets) ) {
                lastUpdate = System.nanoTime();
                return true;
            }
            return false;
        }

        @Override
        protected boolean buildTransactionChanges( Set<EntityChange> updates ) {
            boolean directMods = false;

            // We could potentially avoid this if an added entity sent
            // a full change set.  This is probably more efficient, though.
            if( !directAdds.isEmpty() ) {
                // Add them all
                while( !directAdds.isEmpty() ) {
                    DefaultEntity d = directAdds.poll();
                    // Stick them in the transaction
                    transaction.directAdd(d);
                    directMods = true;
                }
            }

            // Then process the transaction normally
            if( super.buildTransactionChanges(updates) )
                return true;
            return directMods;
        }

        protected void directAdd( DefaultEntity e ) {
            directAdds.add(e);
        }

        @Override
        protected boolean completeEntity( DefaultEntity e ) {

            // In a remote situation, the server is (at least now)
            // always sending us what we need.  If the entity was
            // newly added to this set then it sent us the full
            // entity.  Else we've gotten every change we needed
            // to keep it relevant.
            //
            // So all we need to do is check it for completion and
            // not bother retrieving the values.

            ComponentFilter[] filters = getFilters();
            EntityComponent[] array = e.getComponents();
            for( int i = 0; i < array.length; i++ ) {
                if( array[i] == null || array[i] == REMOVED_COMPONENT ) {
                    if( log.isTraceEnabled() ) {
                        // Logging this because if the assumptions above ever
                        // prove false then it would be nice to have some trace
                        // logging to remind me of the optimization.  (It's a big
                        // huge optimization so it's definitely worth having.)
                        log.trace("Entity is missing type " + getTypes()[i] + " so is not complete for this set.");
                    }
                    return false;
                }
                ComponentFilter filter = filters[i];
                // Check added by PR #18
                // https://github.com/jMonkeyEngine-Contributions/zay-es/pull/18
                // Actually, the check used to be against the remove 'mainFilter' but
                // I've kept the comment as reference to why the filter evaluation
                // was added in case we remember why it was missing originally.
                if( filter != null && !filter.evaluate(array[i]) ) {
                    // This component does not match the filter in place so the entity is not
                    // really part of the set.
                    return false;
                }
            }

            return true;
        }

        @Override
        protected void entityChange( EntityChange change ) {
            lastChangeReceived = System.nanoTime();
            super.entityChange(change);
        }

        // Overridden only to provide outer class local access for debugging.
        @Override
        protected Class<? extends EntityComponent>[] getTypes() {
            return super.getTypes();
        }

        protected <T extends EntityComponent> T checkChangeQueue( EntityId id, Class<T> type ) {
            // We will go through all of them because we want the latest
            // value.
            T result = null;
            for( EntityChange change : getChangeQueue() ) {
                if( type != change.getComponentType() ) {
                    continue;
                }
                if( !Objects.equals(id, change.getEntityId() ) ) {
                    continue;
                }
                result = type.cast(change.getComponent());
            }
            return result;
        }
    }

    private class RemoteWatchedEntity extends DefaultWatchedEntity {

        private final int watchId;

        public RemoteWatchedEntity( EntityData ed, int watchId, EntityId id,
                                    EntityComponent[] components, Class<EntityComponent>[] types ) {
            super(ed, id, components, types);
            this.watchId = watchId;
            watchedEntities.put(watchId, this);
        }

        @Override
        public void release() {
            if( isReleased() ) {
                return;
            }
            super.release();

            if( log.isDebugEnabled() ) {
                log.debug("Releasing watched entity:" + watchId );
            }

            watchedEntities.remove(watchId);

            if( client.isConnected() ) {
                ReleaseWatchedEntityMessage msg = new ReleaseWatchedEntityMessage(watchId);
                client.send(channel, msg);
            }
        }

        @Override
        protected void addChange( EntityChange change ) {
            super.addChange(change);
        }
    }

    private class EntityMessageHandler {

        @SuppressWarnings("unchecked")
        public void entityComponents( ResultComponentsMessage msg ) {
            if( log.isTraceEnabled() ) {
                log.trace("entityComponents(" + msg + ")");
            }
            PendingRequest request = pendingRequests.remove(msg.getRequestId());
            if( request == null ) {
                log.error("Received component data but no request is pending, id:" + msg.getRequestId());
                return;
            }

            request.dataReceived(msg);
        }

        public void entityData( EntityDataMessage msg ) {
            if( log.isTraceEnabled() ) {
                log.trace("entityData(" + msg + ")");
            }
            RemoteEntitySet set = activeSets.get(msg.getSetId());
            if( set == null ) {
                // Probably it was released before we got this message... ships
                // passing in the night.  Just in case, we'll log a warning at
                // least.
                log.warn("Set not found for ID:" + msg.getSetId() + "  May have been released.");
                return;
            }

            for( ComponentData d : msg.getData() ) {
                if( log.isTraceEnabled() ) {
                    log.trace("ComponentData for:" + msg.getSetId() + " :" + d);
                }
                DefaultEntity e = new DefaultEntity(RemoteEntityData.this,
                                                    d.getEntityId(),
                                                    d.getComponents(),
                                                    set.getTypes());
                set.directAdd(e);
            }
        }

        public void componentChange( ComponentChangeMessage msg ) {
            if( log.isTraceEnabled() ) {
                log.trace("componentChange(" + msg + ")");
            }
            for( EntityChange c : msg.getData() ) {
                entityChange(c);
            }
        }

        @SuppressWarnings("unchecked")
        public void entityIds( EntityIdsMessage msg ) {
            if( log.isTraceEnabled() ) {
                log.trace("entityIds(" + msg + ")");
            }
            PendingRequest request = pendingRequests.remove(msg.getRequestId());
            if( request == null ) {
                log.error("Received result entity IDs but no request is pending, id:" + msg.getRequestId());
                return;
            }

            request.dataReceived(msg);
        }

        @SuppressWarnings("unchecked")
        public void stringId( StringIdMessage msg ) {
            if( log.isTraceEnabled() ) {
                log.trace("stringId(" + msg + ")");
            }
            PendingRequest request = pendingRequests.remove(msg.getRequestId());
            if( request == null ) {
                log.error("Received result string ID message but no request is pending, id:" + msg.getRequestId());
                return;
            }

            request.dataReceived(msg);
        }

        public void entitySetError( EntitySetErrorMessage msg ) {
            if( log.isTraceEnabled() ) {
                log.trace("entitySetError(" + msg + ")");
            }
            RemoteEntitySet set = activeSets.get(msg.getSetId());
            if( set == null ) {
                // Probably it was released before we got this message... ships
                // passing in the night.  Just in case, we'll log a warning at
                // least.
                log.warn("Set not found for ID:" + msg.getSetId() + "  May have been released.");
                return;
            }
            set.setError(msg.getError());
        }
    }

    protected abstract class PendingRequest<M, T> {
        protected Message request;
        private final AtomicReference<T> result = new AtomicReference<>();
        private final CountDownLatch received = new CountDownLatch(1);

        protected PendingRequest( Message request ) {
            this.request = request;
        }

        public boolean isDone() {
            return result.get() != null;
        }

        public void close() {
            received.countDown();
        }

        protected void setResult( T val ) {
            result.set(val);
            received.countDown();
        }

        public abstract void dataReceived( M m );

        public T getResult() throws InterruptedException {
            received.await();
            return result.get();
        }

        @Override
        public String toString() {
            return "PendingRequest[" + request + "]";
        }
    }

    protected class PendingEntityRequest extends PendingRequest<ResultComponentsMessage, Entity> {

        public PendingEntityRequest( GetComponentsMessage request )
        {
            super( request );
        }

        @Override
        public void dataReceived( ResultComponentsMessage m ) {
            Entity e = new DefaultEntity(RemoteEntityData.this, m.getEntityId(), m.getComponents(),
                                         ((GetComponentsMessage)request).getComponentTypes());
            setResult(e);
        }
    }

    protected class PendingWatchEntityRequest extends PendingRequest<ResultComponentsMessage, WatchedEntity> {

        public PendingWatchEntityRequest( WatchEntityMessage request ) {
            super( request );
        }

        @Override
        @SuppressWarnings("unchecked")
        public void dataReceived( ResultComponentsMessage m ) {

            WatchedEntity e = new RemoteWatchedEntity(RemoteEntityData.this,
                                                      ((WatchEntityMessage)request).getWatchId(),
                                                      m.getEntityId(),
                                                      m.getComponents(),
                                                      ((WatchEntityMessage)request).getComponentTypes());
            setResult(e);
        }
    }

    protected class PendingEntityIdsRequest extends PendingRequest<EntityIdsMessage, EntityId[]> {

        public PendingEntityIdsRequest( Message request ) {
            super( request );
        }

        @Override
        public void dataReceived( EntityIdsMessage m ) {
            setResult( m.getIds() );
        }
    }

    protected class PendingStringRequest extends PendingRequest<StringIdMessage, StringIdMessage> {
        public PendingStringRequest( Message request ) {
            super(request);
        }

        @Override
        public void dataReceived( StringIdMessage m ) {
            setResult(m);
        }
    }
}
