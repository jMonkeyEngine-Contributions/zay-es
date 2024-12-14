/*
 * $Id$
 *
 * Copyright (c) 2011-2013 jMonkeyEngine
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

package com.simsilica.es.base;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import com.simsilica.es.*;


/**
 *  A set of entities that possess certain components with
 *  automatic updates as the entity components change.
 *
 *  @author    Paul Speed
 */
public class DefaultEntitySet extends AbstractSet<Entity>
                              implements EntitySet {

    static Logger log = LoggerFactory.getLogger(DefaultEntitySet.class);

    /**
     *  Constant used during transaction processing to mark
     *  removed components in already loaded entities.  This distinguishes
     *  them from components that have yet to be filled in and prevents
     *  the values from being re-retrieved during transaction finalization.
     */
    protected static final RemovedComponent REMOVED_COMPONENT = new RemovedComponent();

    // Concurrent hash map because the change set accumulation
    // checks the map for entity ID existence.
    private final Map<EntityId,Entity> entities = new HashMap<>();

    private final ConcurrentLinkedQueue<EntityChange> changes
                    = new ConcurrentLinkedQueue<>();

    private final EntityData ed;

    private final Class<? extends EntityComponent>[] types;
    private final EntityCriteria criteria;

    // Some micro-optimizations can be made if we keep an array of
    // filters that matches the array of types.  (When iterating over
    // entity values, we already know which filter to grab.)
    private ComponentFilter[] filters;
    private boolean filtersChanged = false;

    protected Transaction transaction = new Transaction();
    private final Set<Entity> addedEntities = new HashSet<>();
    private final Set<Entity> changedEntities = new HashSet<>();
    private final Set<Entity> removedEntities = new HashSet<>();

    private boolean released = false;


    public DefaultEntitySet( EntityData ed, ComponentFilter filter, Class<? extends EntityComponent>[] types ) {
        this(ed, new EntityCriteria().set(filter, types));
    }

    public DefaultEntitySet( EntityData ed, EntityCriteria criteria ) {
        this.ed = ed;
        this.criteria = criteria.clone();
        this.types = criteria.toTypeArray();
        this.filters = criteria.toFilterArray();
    }

    protected EntityCriteria getCriteria() {
        return criteria;
    }

    protected Class<? extends EntityComponent>[] getTypes() {
        return types;
    }

    protected ComponentFilter[] getFilters() {
        return filters;
    }

    public String debugId() {
        return "EntitySet@" + System.identityHashCode(this);
    }

    /**
     *  Called to have the entity set load its initial set of
     *  data.  This is called during creation (but not construction)
     *  and when the filter is reset.
     */
    protected void loadEntities( boolean reload ) {

        //Set<EntityId> idSet = ed.findEntities(mainFilter, types);
        Set<EntityId> idSet = ed.findEntities(criteria);
        if( idSet.isEmpty() ) {
            return;
        }

        // Note: we do a full component loop here just to
        // reuse the EntityComponent[] buffer.  We could have
        // just as easily called ed.getEntity() for each ID.

        // Now we have the info needed to build the entity set
        EntityComponent[] buffer = new EntityComponent[types.length];
        for( EntityId id : idSet ) {
            // If we already have the entity then it is not a new
            // add and we'll ignore it.  This means that some entities
            // may have newer info than others but we will get their
            // event soon enough.
            // We include this for the reload after a filter change.
            if( reload && containsId(id) ) {
                continue;
            }

            for( int i = 0; i < buffer.length; i++ ) {
                buffer[i] = ed.getComponent(id, types[i]);
            }

            // Now create the entity
            DefaultEntity e = new DefaultEntity(ed, id, buffer.clone(), types);
            if( add(e) && reload ) {
                addedEntities.add(e);
            }
        }

        // I had a big long comment in AbstractEntityData worrying
        // about threading and stuff.  But the EntityChange events
        // are getting queued and as long as they aren't rejected
        // out of hand (which would be a bug) then we don't care if
        // they come in while we build the entity set.
    }

    /**
     *  Removes entities from the set that no longer match the
     *  set's criteria.  This will update the removedEntities
     *  tracking.
     */
    protected void purgeEntities() {

        for( Iterator<Entity> it = iterator(); it.hasNext(); ) {
            Entity e = it.next();
            if( !entityMatches(e) ) {
                it.remove();
                removedEntities.add(e);
            }
        }
    }

    /**
     *  Swaps out the current main filter for a new one.  Returns
     *  true if the entity set was changed during this process.
     *  This is similar to applyChanges() and any current pending
     *  changes will be applied during the filter transition.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void resetFilter( ComponentFilter filter ) {

        // General logic:
        // -remove the entities that don't match the filter
        // -find the entities that do and add them if they
        //  weren't already added.
        // -make sure that this is reflected in the change
        //  sets.


        // The problem with treating this like applyChanges() is
        // that it makes for two different code paths, for one.
        // For another, it makes the HostedEntityData's job harder
        // trying to sort out which events to send to the user.
        //
        // We can't delay until applyChanges() is called because
        // then we risk doing work collecting changes for entities
        // that don't apply anymore.  Though I guess in the remote
        // case it won't be sending them.
        //
        // What is the downside of setting a flag and waiting for
        // the next applyChanges()?  In single player, we will accept
        // changes for entities that we haven't purged.  Which isn't
        // too big of a deal.
        // In multiplayer this won't happen except anything that was
        // already sent anyway.  The server will have accurate filtering
        // after its next update... and will send us our removes, too
        // actually.  Perhaps that is not something that we want.
        //
        // I don't see a way of avoiding it, though.  Even if we purge
        // them here we would have to do special case code not to send
        // them on the server.  I suppose we could reset the filter
        // and apply all at once even on the server and just send
        // the changes right then... but then we potentially have a
        // threading problem that we didn't have before.  Right now
        // only one thread is applying changes and sending those
        // results out.  So we'd still delay and we'd have to special
        // case things in the send changes loop.
        //
        // We can always add a flag later, I guess... or just expose
        // the one we've added here and let the loop see if the filter
        // has changed before applyChanges() is called.
        //
        // I've kept the above for reference but the current implementation
        // sets a flag and lets applyChanges() fix things.

        // Switch out the main filter
        //setMainFilter(filter);

        // EntityCriteria compatibility adapting
        if( filter == null ) {
            // This is the case that is non-trivial to handle... except that
            // code that uses resetFilter(filter) is already expecting there to only
            // be one filter... so we'll remove all of the filters we find.
            criteria.clearFilters();
        } else {
            criteria.setFilter(filter.getComponentType(), filter);
        }

        resetEntityCriteria(criteria);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void resetEntityCriteria( EntityCriteria update ) {
        // It's possible we were passed in our own criteria object in
        // which case we can skip some checks and updates.
        if( criteria != update ) {
            // Have to use the array form of the types to compare order.
            Class<? extends EntityComponent>[] updateTypes = update.toTypeArray();
            if( !Arrays.equals(types, updateTypes) ) {
                throw new IllegalArgumentException("Types do not match, existing:" + Arrays.asList(types)
                                                + ", update:" + Arrays.asList(updateTypes));
            }
            criteria.clearFilters();
            for( ComponentFilter filter : update.getFilters() ) {
                if( filter != null ) {
                    criteria.setFilter(filter.getComponentType(), filter);
                }
            }
        }

        // Rebuild our local filter array.
        this.filters = criteria.toFilterArray();
        filtersChanged();
    }

    protected void filtersChanged() {
        filtersChanged = true;
    }

    @Override
    public boolean containsId( EntityId id ) {
        return entities.containsKey(id);
    }

    @Override
    public Set<EntityId> getEntityIds() {
        return entities.keySet();
    }

    @Override
    public Entity getEntity( EntityId id ) {
        return entities.get(id);
    }

    @Override
    public boolean equals( Object o ) {
        return o == this;
    }

    @Override
    public int size() {
        return entities.size();
    }

    @Override
    public Iterator<Entity> iterator() {
        return new EntityIterator();
    }

    @Override
    public void clear() {
        entities.clear();
    }

    @Override
    public boolean add( Entity e ) {
        // Note: this may come back to bite me later but we
        //       return that we don't add it but we actually do replace
        //       the old value.
        return entities.put(e.getId(), e) == null;
    }

    protected Entity remove( EntityId id ) {
        return entities.remove(id);
    }

    @Override
    public boolean remove( Object e ) {
        if( !(e instanceof Entity) )
            return false;
        return entities.remove(((Entity)e).getId()) != null;
    }

    @Override
    public boolean contains( Object e ) {
        if( !(e instanceof Entity) )
            return false;
        return entities.containsKey(((Entity)e).getId());
    }

    /**
     *  Returns the entities that were added during applyChanges.
     */
    @Override
    public Set<Entity> getAddedEntities() {
        return addedEntities;
    }

    /**
     *  Returns the entities that were changed during applyChanges.
     */
    @Override
    public Set<Entity> getChangedEntities() {
        return changedEntities;
    }

    /**
     *  Returns the entities that were removed during applyChanges.
     */
    @Override
    public Set<Entity> getRemovedEntities() {
        return removedEntities;
    }

    @Override
    public void clearChangeSets() {
        addedEntities.clear();
        changedEntities.clear();
        removedEntities.clear();
    }

    @Override
    public boolean hasChanges() {
        return !addedEntities.isEmpty()
                || !changedEntities.isEmpty()
                || !removedEntities.isEmpty();
    }

    /**
     *  Applies any accumulated changes to this list's entities since
     *  the last time it was called and returns true if there were
     *  changes.
     */
    @Override
    public boolean applyChanges() {
        return applyChanges(null, true);
    }

    /**
     *  Applies any accumulated changes to this list's entities since
     *  the last time it was called and returns true if there were
     *  changes.  Changes that caused an update (not an add or a remove)
     *  will be added to the supplied updates set.
     */
    @Override
    @Deprecated
    public boolean applyChanges( Set<EntityChange> updates ) {
        //if( updates != null ) {
        //    throw new UnsupportedOperationException("The applyChanges(Set<EntityChange>) is deprecated and no longer operates.  It will be removed soon.");
        return applyChanges(updates, true);
    }

    protected boolean buildTransactionChanges( Set<EntityChange> updates ) {

        if( changes.isEmpty() )
            return false;

        EntityChange change;
        while( (change = changes.poll()) != null ) {
            transaction.addChange(change, updates);
        }
        return true;
    }

    public boolean hasFilterChanged() {
        return filtersChanged;
    }

    protected boolean applyChanges( Set<EntityChange> updates, boolean clearChangeSets ) {

        long start = System.nanoTime();
        long transStart = 0;
        long transEnd = 0;
        long filterStart = 0;
        long filterEnd = 0;

try {
        // Need to return something to the caller about the adds and removes
        // actually we could just add that information to internal buffers
        // that get reset each time this is called. That means for any given
        // frame you can see what changed since the last one.  We could
        // provide a method for clearing them if the app was memory starved
        // or add an argument to this method.
        //
        // this is useful for cleaning up things like the scene graph where
        // we keep non-component objects out there.  I'm not sure they could
        // be made components either and we'd probably still care about whether
        // they were added or not or what specific components had changed...
        // even if only for performance reasons.  We'll see.
        if( clearChangeSets ) {
            clearChangeSets();
        }

        if( released ) {
            // Then the changes are irrelevant
            changes.clear();

            // And everything is a remove
            removedEntities.addAll(this);
            clear();
            return hasChanges();
        }

        transStart = System.nanoTime();
        if( buildTransactionChanges(updates) ) {
            // Resolve all of the changes into the change sets
            transaction.resolveChanges();
        }
        transEnd = System.nanoTime();

        if( filtersChanged ) {
            filtersChanged = false;

            filterStart = System.nanoTime();

            // Recompose our local filters array
            this.filters = criteria.toFilterArray();

            // Remove any entities that no longer match
            purgeEntities();

            // Find the latest entities
            loadEntities(true);

            filterEnd = System.nanoTime();
        }

        return !addedEntities.isEmpty()
                || !changedEntities.isEmpty()
                || !removedEntities.isEmpty();
} finally {
    long end = System.nanoTime();
    if( end - start > 13000000L ) {
        log.warn(String.format("Long update: %.03f ms, trans: %.03f, filter: %.03f, this: %s",
                 (end - start)/1000000.0,
                 (transEnd - transStart)/1000000.0,
                 (filterEnd - filterStart)/1000000.0,
                 this
                 ));
    }
}
    }

    /**
     *  Releases this entity set from processing further entity
     *  updates and destroys any internal data structures.
     */
    @Override
    public void release() {

        if( ed instanceof DefaultEntityData ) {
            // Other non-DefaultEntityData implementations will have
            // to override release() if they need special behavior.
            ((DefaultEntityData)ed).releaseEntitySet(this);
        }

        // Except we can't clear because release() might have been
        // called from a different thread than the one processing
        // the data.
        // clear();
        released = true;
    }

    protected boolean isReleased() {
        return released;
    }

    protected boolean entityMatches( Entity e ) {

        EntityComponent[] array = e.getComponents();
        for( int i = 0; i < types.length; i++ ) {
            if( array[i] == null ) {
                return false;
            }

            if( array[i] == REMOVED_COMPONENT ) {
                // Note: we may not catch them all but that's ok.  The
                // entity is "removed" and so the state is invalid anyway...
                // but I feel more comfortable clearing the reference if I can.
                array[i] = null;
                return false;
            }

            if( filters[i] == null ) {
                continue;
            }

            if( !filters[i].evaluate(array[i]) ) {
                return false;
            }
        }
        return true;
    }

    /**
     *  Returns true if the specific component matches the criteria
     *  for this entity set.
     */
    protected boolean isMatchingComponent( EntityComponent c ) {
        return criteria.isMatchingComponent(c);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final boolean hasType( Class type ) {
        return criteria.hasType(type);
    }

    private int typeIndex( Class type ) {
        for( int i = 0; i < types.length; i++ ) {
            if( types[i] == type ) {
                return i;
            }
        }
        return -1;
    }

    protected boolean isRelevantChange( EntityChange change ) {

        // Here we care about any changes that change the status
        // of an entity's inclusion in this set.
        // So:
        // -changes for components we don't watch can be ignored.
        // -changes for entities we have and types we have are
        //  always important.
        // -changes for entities we don't have but that match
        //  our filters we do care about.

        // Is this even a type we watch?
        if( !hasType(change.getComponentType()) ) {
            if( log.isTraceEnabled() )
                log.trace( "   not our type." );
            return false;  // doesn't matter to us
        }

        // There use to be a bunch of logic here trying to determine
        // if a change is relevant to an entity we have or might have.
        // It can be made to work but not without a lot of threading
        // care.  Changes come in from all over and right now we
        // nicely shove them into a queue and forget about them.
        // If we decide to do the book-keeping then we have to track
        // entity ID interest as well.  If we see a change that matches
        // our filter and is related to an entity we don't have then
        // we need to add that entity to an interest set.  If we see
        // the same component and it no longer matches the filter then
        // we know we'd need to keep it.
        // This is all in the interest of minimizing the amount of
        // component retrieval required during applyChanges().
        // This book-keeping then needs to be in sync with the change
        // queue and event checking to make sure threads aren't stomping
        // all over themselves.
        // And at the end of the day, most of the time processing an
        // irrelevant change isn't that expensive.  At worst we'll create
        // an entity just to throw it away but the most common use-case
        // is when one component changes often.  In that case, when
        // we filter it out in Transaction.addChange() we've avoided the
        // extra work.  And that side is single-threaded and has a consisent
        // state.

        // We might care
        return true;
    }

    protected void entityChange( EntityChange change ) {
        if( log.isTraceEnabled() ) {
            log.trace("entityChange(" + change + ")");
        }
        if( !isRelevantChange(change) ) {
            return;
        }
        if( log.isTraceEnabled() ) {
            log.trace("Adding change:" + change);
        }

        // Accumulate the change for the next update pass
        changes.add(change);
    }

    /**
     *  Called by the Transaction object to fill in the missing
     *  components for an entity if this is the first time it's
     *  been seen by the set and may not have all of its components
     *  yet.
     */
    protected boolean completeEntity( DefaultEntity e ) {

        // Try to make it complete if is isn't already.
        // We need to recheck the components against the
        // filters because we do no prefiltering on changes.
        // Technically, we shouldn't get changes from components
        // that don't match but it is better to be safe.
        // For example, there is a "bug" at the moment where
        // direct removes are happening before the transaction
        // processing which means we see changes for an entity
        // that has been forcefully removed and we think it is
        // an add.  But because when we had the entity we accept
        // all changes even if they didn't match (in case it
        // would remove the entity) then we would add it again
        // even though the components did not match.
        EntityComponent[] array = e.getComponents();
        for( int i = 0; i < types.length; i++ ) {
            boolean rechecking = false;
            if( array[i] == null ) {
                // Fill it in
                if( log.isDebugEnabled() ) {
                    log.debug("Pulling component type:" + types[i] + " for id:" + e.getId());
                }
                array[i] = ed.getComponent(e.getId(), types[i]);

                // If we get nothing back then this entity can't be completed
                if( array[i] == null ) {
                    if( log.isDebugEnabled() ) {
                        log.debug("Entity " + e.getId() + " could not be completed for type:" + types[i]);
                    }
                    return false;
                }
            } else if( array[i] == REMOVED_COMPONENT ) {
                // Set it back to null again just in case the caller
                // is holding a reference... but otherwise it means this
                // entity is 'dead'.
                array[i] = null;
                return false;
            } else {
                rechecking = true;
            }

            // Now that we have a value, check the filters

            // If we have a filter and it doesn't match the filter then
            // this whole entity doesn't match
            ComponentFilter filter = filters[i];
            if( filter != null && !filter.evaluate(array[i]) ) {
                // Because we wasted a lot of time for something
                // that should be filtered in most cases... usually
                // it's a bug when it isn't.
                //if( rechecking )
                //    log.warn( "Non-matching component:" + array[i] + " for entity:" + e );
                return false;
            }
        }

        // Just a safety net... can maybe be removed
        ((DefaultEntity)e).validate();

        return true;
    }

    protected ConcurrentLinkedQueue<EntityChange> getChangeQueue() {
        return changes;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getSimpleName())
            .add("types", Arrays.asList(types))
            .add("criteria", criteria)
            .add("entityCount", entities.size())
            .add("changeCount", changes.size())
            .add("released", released)
            .toString();
    }

    private class EntityIterator implements Iterator<Entity> {

        private final Iterator<Map.Entry<EntityId,Entity>> delegate = entities.entrySet().iterator();

        public EntityIterator() {
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public Entity next() {
            return delegate.next().getValue();
        }

        @Override
        public void remove() {
            delegate.remove();
        }
    }

    /**
     *  A special entity component that is used during transaction processing
     *  to temporarily mark a component as "removed".  This is different than
     *  null which could also indicate "unset".
     */
    protected static class RemovedComponent implements EntityComponent {
    }

    /**
     *  Keeps accumulated data about a set of changes.  This buffers
     *  the changes and finalizes them all at once which should be
     *  a more efficient and simpler design than the deeply nested
     *  loop I used to have in applyChanges().
     *
     *  Logic design is as follows:
        // Keep a map of the entities that we got components for that
        // we applied... so a DefaultEntity just kept there.
        //
        // Keep a map of the entities that we have changed for other
        // reasons (or just the set).
        //
        // Go through the adds and complete any that are incomplete.
        // Remove any that don't match the filter.
        //
        // Go through the otherwise changed entities and check them
        // against the filter.  If no longer matching then they
        // are removes.  Otherwise, they are really changes.
        //
        // In the adds, there is a slight race condition of sorts
        // since when we go to fully retrieve the entity then we
        // may see state that we haven't applied changes for yet.
        // I think that's ok.  We still give an accurate state upon
        // return from this method and the next changes will just
        // be redundant.
        //
        // The other benefit of the above is that in the cases
        // where all of the entity components have changed (like when
        // forwarding a new entity over the network) then we don't
        // redundantly pull its whole info and we no longer stress
        // the cache we put in to worry about that case.
     */
    protected class Transaction {

        final Map<EntityId,DefaultEntity> adds = new HashMap<>();
        final Set<EntityId> mods = new HashSet<>();

        /**
         *  Called when we know (for whatever reason) that a full entity
         *  is being added.  This is useful for any implementation that
         *  asynchronously loads data.
         */
        public void directAdd( DefaultEntity e ) {
            adds.put(e.getId(), e);
        }

        public void addChange( EntityChange change, Set<EntityChange> updates ) {
            EntityId id = change.getEntityId();
            EntityComponent comp = change.getComponent();
            DefaultEntity e = (DefaultEntity)entities.get( id );

            // If we don't have the entity then it's an add
            // and we need to create one.
            if( e == null ) {
                // See if we already added this one
                e = adds.get(id);

                if( e == null ) {
                    if( comp == null ) {
                        // We've never seen this entity before and we get
                        // an event about removing a component.  We can
                        // safely ignore it
                        return;
                    }

                    // We add the components even if they don't match because
                    // otherwise we might have to retrieve them again.  We
                    // could get lots of changes for this entity and we will
                    // validate what we have in the completion loop.
                    // ...BUT...
                    // On the other hand, we might not get any more changes
                    // for this entity at all and if the component didn't match
                    // then we created the entity for nothing.  This is an
                    // extremely common use-case for the types of components most
                    // likely to be spammed and filtered.
                    if( !isMatchingComponent(comp) ) {
                        return;
                    }

                    // Else we need to add it.

                    // Create an empty entity with the right number
                    // of components.
                    e = new DefaultEntity(ed, id, new EntityComponent[types.length], types);
                    adds.put(id, e);
                }

            } else {
                // Then it's an entity we have already and we are about
                // to change it.
                mods.add(id);

                // We track the updates that caused a change... we'll
                // filter out the ones that were for removed entities
                // later.  This is actually somewhat better than we did
                // before since we only send changes for entities that
                // are still relevant.
            }

            // Apply the change
            int index;
            if( comp == null ) {
                index = typeIndex(change.getComponentType());
            } else {
                index = typeIndex(comp.getClass());
            }

            // We keep track of the changes that might have been
            // relevant.  Technically this is too broadly scoped but
            // determining accurate affecting changes requires book-keeping
            // per entity.
            // If we do no filtering at all that means that all Position
            // changes get delivered to all clients even if they don't
            // have the entity... no.  Because we would have pre-filtered
            // that case.  Still, we can double-check here by hitting it
            // against the filter before adding it to the updates set.
            if( updates != null ) {
                if( comp == null
                    || filters[index] == null
                    || filters[index].evaluate(comp) ) {

                    // || mainFilter.evaluate(e.get(mainFilter.getComponentType())) ) {
                    //
                    // The last condition above is proposed by user qxCsXO1
                    // in this thread: http://hub.jmonkeyengine.org/t/zay-es-net-componentfilter-not-working/33196
                    //
                    // What it does is avoid adding updates for entities that don't
                    // meet the main filter.  This comes up in cases where a player
                    // is filtering by something like an OwnedBy component but is still
                    // getting position changes for those entities.
                    //
                    // Unfortunately, the issue with the above change is it might
                    // miss some changes.  We may not have applied the change yet that
                    // lets e.get(mainFilter.getComponentType()) pass.  When we eventually
                    // get to it then we've already missed the other updates.  If those
                    // other changes happen infrequently then the client can be really
                    // behind.
                    //
                    // It could be that we have no choice but to make two passes through
                    // the change events or go through the updates and remove the ones
                    // for entities that aren't in the set anymore.
                    updates.add(change);
                } else {
                }
            }

            // Setting a component to null because of a 'removed'
            // component is different than a component that just
            // happens to be null because it hasn't been filled in yet.
            e.getComponents()[index] = comp != null ? comp : REMOVED_COMPONENT;

            // There is another issue I've just thought of regarding the updates
            // set.  The updates will potentially come out of order.  Mostly
            // we update often enough that it won't happen much but it is possible
            // that two changes to the same component get added to the set out of
            // order and then the clients (in a network use-case) end up with
            // incorrect values.
            //
            // Two approaches I see:
            // 1) timestamp the updates so we can sort them by order.
            //
            // 2) abandon the collection of an updates set completely and go
            //    with another solution in the networking layer that does its
            //    own change tracking more accurately and sends them per entity set
            //    or something.

        }

        public void resolveChanges() {
            // So now we can take what we've accumulated and figure
            // out what's what.

            // Process the adds.
            for( DefaultEntity e : adds.values() ) {
                if( completeEntity(e) ) {
                    // It was an added entity
                    if( add(e) ) {
                        addedEntities.add(e);
                    }
                } else {
                    // It couldn't be completed so it is not really an add...
                    // it's just a waste of our time. ;)
                }
            }

            // Now... see which changes were removes and which ones
            // were real updates
            for( EntityId id : mods ) {

                Entity e = entities.get(id);

                //((DefaultEntity)e).validate();

                // e.isComplete() would run through the whole component
                // loop which we will do anyway to check the filters.
                // We'll combine these into one step
                if( entityMatches(e) ) {
                    changedEntities.add(e);
                } else {
                    if( remove(e) ) {
                        removedEntities.add(e);
                    }
                }
            }

            // Clear the buffers for next time
            adds.clear();
            mods.clear();
        }
    }
}

