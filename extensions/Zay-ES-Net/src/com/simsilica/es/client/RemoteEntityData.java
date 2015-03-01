/*
 * $Id$
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

import com.google.common.base.Objects;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.simsilica.es.ComponentFilter;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityChange;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.StringIndex;
import com.simsilica.es.base.DefaultEntity;
import com.simsilica.es.base.DefaultEntitySet;
import com.simsilica.es.net.ComponentChangeMessage;
import com.simsilica.es.net.EntityDataMessage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.simsilica.es.net.EntityDataMessage.ComponentData;
import com.simsilica.es.net.EntityIdsMessage;
import com.simsilica.es.net.FindEntitiesMessage;
import com.simsilica.es.net.FindEntityMessage;
import com.simsilica.es.net.GetComponentsMessage;
import com.simsilica.es.net.GetEntitySetMessage;
import com.simsilica.es.net.ObjectMessageDelegator;
import com.simsilica.es.net.ReleaseEntitySetMessage;
import com.simsilica.es.net.ResetEntitySetFilterMessage;
import com.simsilica.es.net.ResultComponentsMessage;
import com.simsilica.es.net.StringIdMessage;


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
    private static AtomicInteger nextSetId = new AtomicInteger();

    /**
     *  Keeps track of the next ID used for requests that return
     *  results... especially when the caller will be waiting for them.
     */
    private static AtomicInteger nextRequestId = new AtomicInteger();

    private Client client;
    private int channel;
    
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
    private Map<Integer,PendingRequest> pendingRequests = new ConcurrentHashMap<Integer,PendingRequest>();
    
    /**
     *  The active EntitySets that have been requested by the user
     *  but not yet released.  Incoming changes and updates are applied
     *  to these sets by setId.
     */
    private Map<Integer,RemoteEntitySet> activeSets = new ConcurrentHashMap<Integer,RemoteEntitySet>();

    private ObjectMessageDelegator messageHandler; 

    private RemoteStringIndex strings = new RemoteStringIndex(this);

    /**
     *  Creates a new RemoteEntityData instance that will communicate
     *  over the specified client and channel to provide remote
     *  EntityData access.
     */
    public RemoteEntityData( Client client, int channel ) {
        this.client = client;
        this.channel = channel;
        this.messageHandler = new ObjectMessageDelegator(new EntityMessageHandler(), true);
        client.addMessageListener(messageHandler, messageHandler.getMessageTypes());
    }
 
    @Override
    public <T extends EntityComponent> T getComponent( EntityId entityId, Class<T> type ) {
        if( log.isTraceEnabled() ) {
            log.trace("getComponent(" + entityId + ", " + type + ")");
        }

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
            log.trace("getEntity(" + entityId + ", " + Arrays.asList(types) + ")");
        }

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
        // Need to fetch the entity
        int id = nextRequestId.getAndIncrement();
        FindEntityMessage msg = new FindEntityMessage(id, filter, types);
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
        
        // Need to fetch the entity
        int id = nextRequestId.getAndIncrement();
        FindEntitiesMessage msg = new FindEntitiesMessage(id, filter, types);
        msg.setReliable(true);
        
        // Setup the 'pending' request tracking so that we
        // can wait for the response.  Just in case, always make sure to
        // do this before sending the message.
        PendingEntityIdsRequest request = new PendingEntityIdsRequest(msg); 
        pendingRequests.put(id, request);
        
        // Now send the message.
        client.send(channel, msg);
 
        Set<EntityId> result = new HashSet<EntityId>();               
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
    public EntitySet getEntities( Class... types ) {
        return getEntities(null, types);
    }

    @Override
    public EntitySet getEntities( ComponentFilter filter, Class... types ) {
        
        if( log.isTraceEnabled() ) {
            log.trace("getEntities(" + filter + ", " + Arrays.asList(types) + ")");
        }

        int id = nextSetId.getAndIncrement();
        RemoteEntitySet result = new RemoteEntitySet(id, filter, types);

        // Make sure we register the entity set before sending the message...
        activeSets.put(id, result);
        
        // Send a message to ask the server to start watching this
        // for us... and to send an initial data set asynchronously.                  
        Message m = new GetEntitySetMessage( id, filter, types );
        m.setReliable(true);
        client.send(channel, m);
 
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
 
    protected void entityChange( EntityChange change ) {
    
        for( RemoteEntitySet set : activeSets.values() ) {
            set.entityChange(change);
        }       
    }
    
    private class RemoteEntitySet extends DefaultEntitySet {
    
        private int setId;
        private ConcurrentLinkedQueue<DefaultEntity> directAdds 
                    = new ConcurrentLinkedQueue<DefaultEntity>();
        private long lastUpdate;                    

        public RemoteEntitySet( int setId, ComponentFilter filter, Class[] types ) {
            super(RemoteEntityData.this, filter, types);
            this.setId = setId;
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
        }
        
        @Override
        public void resetFilter( ComponentFilter filter ) { 
            super.resetFilter(filter);
 
            // Need to send a message to the server to let it
            // know our interests have changed.
            Message m = new ResetEntitySetFilterMessage(setId, filter);
            m.setReliable(true);
            
            if( log.isDebugEnabled() )
                log.debug( "Sending filter reset:" + m );
                            
            client.send(channel, m);            
        }

        @Override 
        protected boolean applyChanges( Set<EntityChange> updates, boolean clearChangeSets ) {
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
        protected void entityChange( EntityChange change ) {
            lastChangeReceived = System.nanoTime();
            super.entityChange(change);
        }
        
        @Override
        protected Class[] getTypes() {
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
                if( !Objects.equal(id, change.getEntityId() ) ) {
                    continue;
                }
                result = (T)change.getComponent();
            }
            return result;
        }
    }
    
    private class EntityMessageHandler {

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
    }
    
    protected abstract class PendingRequest<M, T> {
        protected Message request;
        private AtomicReference<T> result = new AtomicReference<T>();
        private CountDownLatch received = new CountDownLatch(1);
 
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
