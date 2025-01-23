/*
 * $Id$
 *
 * Copyright (c) 2015, Simsilica, LLC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.simsilica.es.server;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.*;

import com.simsilica.es.*;
import com.simsilica.es.base.CompositeQuery;
import com.simsilica.es.base.DefaultEntitySet;
import com.simsilica.es.base.DefaultWatchedEntity;


/**
 *  A client-specific view of the EntityData that wraps a delegate EntityData and
 *  passes most calls directly through but accumulates EventChanges in its own
 *  queue to be applied at a later time.  EntitySets and WatchedEntities are
 *  created locally to this EntityDataWrapper so that they don't get entity
 *  change notifications until later snapshot processing.
 *
 *  <p>The main purpose of this wrapper is to have a consistent
 *  view between the EntityChanges applied and what the EntitySets
 *  have queued.  This facilitates sending appropriate changes
 *  to the client 'mirror' of this data.</p>
 *
 *  @author    Paul Speed
 */
public class EntityDataWrapper implements ObservableEntityData {

    static Logger log = LoggerFactory.getLogger(EntityDataWrapper.class);

    private final ObservableEntityData delegate;
    private final ChangeObserver listener = new ChangeObserver();

    /**
     *  A registry of strategy objects that control the client's visibility of certain
     *  component values.
     */
    private final Map<Class, ComponentVisibility> visibilityFilters = new HashMap<>();

    private final List<LocalEntitySet> entitySets = new CopyOnWriteArrayList<LocalEntitySet>();
    private final List<EntityComponentListener> entityListeners = new CopyOnWriteArrayList<EntityComponentListener>();

    private final ConcurrentLinkedQueue<EntityChange> changes = new ConcurrentLinkedQueue<>();

    public EntityDataWrapper( ObservableEntityData delegate ) {
        this.delegate = delegate;
        delegate.addEntityComponentListener(listener);
    }

    /**
     *  Registers a ComponentVisibility strategy object that will limit this client's
     *  view of the specific handled component values.  To the client seeing this EntityDataWrapper,
     *  invisible component values appear not to exist.
     */
    public void registerComponentVisibility( ComponentVisibility visibility ) {
        visibilityFilters.put(visibility.getComponentType(), visibility);
        visibility.initialize(delegate);
    }

    /**
     *  Provides direct access to a set's type list to allow efficient mark/sweep
     *  iteration.
     */
    public Class<? extends EntityComponent>[] getTypes( EntitySet set ) {
        return ((LocalEntitySet)set).getTypes();
    }

    /**
     *  After an applyChanges() this will return the purged entities for a particular
     *  local client-specific entity set.
     */
    public Set<EntityId> getPurgedEntities( EntitySet set ) {
        return ((LocalEntitySet)set).getPurgedEntities();
    }

    @Override
    public EntityId createEntity() {
        return delegate.createEntity();
    }

    @Override
    public void removeEntity( EntityId entityId ) {
        delegate.removeEntity(entityId);
    }

    @Override
    public void setComponent( EntityId entityId, EntityComponent component ) {
        delegate.setComponent(entityId, component);
    }

    @Override
    public void setComponents( EntityId entityId, EntityComponent... components ) {
        delegate.setComponents(entityId, components);
    }

    @Override
    public <T extends EntityComponent> boolean removeComponent( EntityId entityId, Class<T> type ) {
        return delegate.removeComponent(entityId, type);
    }

    @Override
    public void removeComponents( EntityId entityId, Class... types ) {
        delegate.removeComponents(entityId, types);
    }

    @Override
    public <T extends EntityComponent> T getComponent( EntityId entityId, Class<T> type ) {
        if( log.isTraceEnabled() ) {
            log.trace("getComponent(" + entityId + ", " + type + ")");
        }
        ComponentVisibility visibility = visibilityFilters.get(type);
        if( visibility != null ) {
            return visibility.getComponent(entityId, type);
        }
        return delegate.getComponent(entityId, type);
    }

    @Override
    public Entity getEntity( EntityId entityId, Class... types ) {
        if( log.isTraceEnabled() ) {
            log.trace("getEntity(" + entityId + ", " + Arrays.asList(types) + ")");
        }
        // Ok to just return it because this entity is not tracked or anything
        // FIXME: filter the entity's components based on visibility filters
        return delegate.getEntity(entityId, types);
    }

    @Override
    public EntityId findEntity( ComponentFilter filter, Class... types ) {
        if( log.isTraceEnabled() ) {
            log.trace("findEntity(" + filter + ", " + Arrays.asList(types) + ")");
        }
        // FIXME: reimplement in terms of findEntities() or using a similar
        // technique to pay attention to component visility.
        return delegate.findEntity(filter, types);
    }

    @Override
    public EntityId findEntity( EntityCriteria criteria ) {
        if( log.isTraceEnabled() ) {
            log.trace("findEntity(" + criteria + ")");
        }
        Query query = createQuery(criteria);
        return query.findFirst();
    }

    protected ComponentFilter forType( ComponentFilter filter, Class type ) {
        if( filter == null || filter.getComponentType() != type )
            return null;
        return filter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<EntityId> findEntities( ComponentFilter filter, Class... types ) {
        if( log.isTraceEnabled() ) {
            log.trace("findEntities(" + filter + ", " + Arrays.asList(types) + ")");
        }

        // If there are no visility filters then there is no reason to
        // do extra work.
        if( visibilityFilters.isEmpty() ) {
            return delegate.findEntities(filter, types);
        }

        if( types == null || types.length == 0 ) {
            types = new Class[] { filter.getComponentType() };
        }

        // See if any of the specified types have a visibility filter
        int visIndex = -1;
        ComponentVisibility visibility = null;
        for( int i = 0; i < types.length; i++ ) {
            visibility = visibilityFilters.get(types[i]);
            if( visibility != null ) {
                visIndex = i;
                break;
            }
        }

        if( visibility == null ) {
            // Just default behavior then
            return delegate.findEntities(filter, types);
        }

        // Else start with the filter
        Set<EntityId> first = visibility.getEntityIds(forType(filter, types[visIndex]));
        if( first.isEmpty() ) {
            return Collections.emptySet();
        }

        // We'll assume that the delegate can efficiently return the rest of the
        // ID sets for reduction... but we could also have gone through each ID
        // in the 'first' set and tried to fill them out.  The thing is that
        // if all other types are handled in the DefaultEntitySet way then it's
        // probably more efficient to just grab their sets.  SqlEntityData might
        // be an exception.  Actually, let me prototype both.  Could be we use
        // some heuristics and add some 'isEfficient' style methods to EntityData.
        // Note: if this is really a thing, we probably want to make two passes.
        Set<EntityId> and = new HashSet<EntityId>();
        and.addAll(first);

        for( int i = 0; i < types.length; i++ ) {
            if( i == visIndex ) {
                continue;
            }
            if( true ) {
                // The 'everything in RAM' way
                Set<EntityId> sub = delegate.findEntities(filter, types[i]);
                if( sub.isEmpty() ) {
                    // Intersection would be empty... early out
                    return Collections.emptySet();
                }
                // Keep only the intersection
                and.retainAll(sub);
            } else {
                // The 'loading all IDs would be slow for this type' way
                for( Iterator<EntityId> it = and.iterator(); it.hasNext(); ) {
                    EntityId id = it.next();
                    if( delegate.getComponent(id, types[i]) == null ) {
                        it.remove();
                    }
                }
            }
        }

        return and;
    }

    @Override
    public <T extends EntityComponent> Query createQuery( ComponentFilter<T> filter, Class<T> type ) {
        ComponentVisibility visibility = visibilityFilters.get(type);
        if( visibility != null ) {
            // The visibility query trumps normal component queries
            return new Query() {
                    @Override
                    public Set<EntityId> execute() {
                        return visibility.getEntityIds(filter);
                    }
                };
        }
        return delegate.createQuery(filter, type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Query createQuery( EntityCriteria criteria ) {
        ComponentFilter[] filters = criteria.toFilterArray();
        Class[] types = criteria.toTypeArray();

        List<Query> subQueries = new ArrayList<>();
        for( int i = 0; i < types.length; i++ ) {
            Query query = createQuery(filters[i], types[i]);

            // See if it can be merged with any of the existing subqueries
            for( ListIterator<Query> it = subQueries.listIterator(); it.hasNext(); ) {
                Query sub = it.next();
                Query merged = sub.join(query);
                if( merged != null ) {
                    query = null;
                    it.set(merged);
                    break;
                }
            }

            // If it wasn't merged with an existing query then add it to
            // the list.
            if( query != null ) {
                subQueries.add(query);
            }
        }

        if( subQueries.size() == 1 ) {
            return subQueries.get(0);
        }
        return new CompositeQuery(subQueries);
    }

    @Override
    public Set<EntityId> findEntities( EntityCriteria criteria ) {
        // If there are no visility filters then there is no reason to
        // do extra work.
        if( visibilityFilters.isEmpty() ) {
            return delegate.findEntities(criteria);
        }
        Query query = createQuery(criteria);
        return query.execute();
    }

    @Override
    public EntitySet getEntities( Class... types ) {
        return getEntities(null, types);
    }

    @Override
    @SuppressWarnings("unchecked") // because Java doesn't like generic varargs
    public EntitySet getEntities( ComponentFilter filter, Class... types ) {
        return getEntities(new EntityCriteria().add(types));
    }

    @Override
    public EntitySet getEntities( EntityCriteria criteria ) {
        LocalEntitySet result = new LocalEntitySet(this, criteria);
        result.loadEntities(false);
        entitySets.add(result);
        return result;
    }

    @Override
    @SuppressWarnings("unchecked") // because Java doesn't like generic varargs
    public WatchedEntity watchEntity( EntityId entityId, Class... types ) {
        if( log.isTraceEnabled() ) {
            log.trace("watchEntity(" + entityId + ", " + Arrays.asList(types) + ")");
        }
        return new DefaultWatchedEntity(this, entityId, types);
    }

    @Override
    public StringIndex getStrings() {
        return delegate.getStrings();
    }

    @Override
    public void addEntityComponentListener( EntityComponentListener l ) {
        entityListeners.add(l);
    }

    @Override
    public void removeEntityComponentListener( EntityComponentListener l ) {
        entityListeners.remove(l);
    }

    @Override
    public void close() {
        // We are just a view... so don't pass it on

        // Just remove our listener
        delegate.removeEntityComponentListener(listener);
    }

    /**
     *  Applies the queued changes to this
     */
    public boolean applyChanges( List<EntityChange> updates ) {

        if( !visibilityFilters.isEmpty() ) {
            for( ComponentVisibility vis : visibilityFilters.values() ) {
                vis.collectChanges(changes);
            }
        }

        // Drain the queue, applying all changes to the entity sets
        // and listeners... and keeping track of what we actually
        // applied.  This should keep all of the views consistent and
        // is basically the entire point of this wrapper class.
        if( changes.isEmpty() ) {
            return false;
        }

        EntityChange change;
        while( (change = changes.poll()) != null ) {
            updates.add(change);
            entityChange(change);
        }
        return true;
    }

    protected void entityChange( EntityChange change ) {

        for( EntityComponentListener l : entityListeners ) {
            l.componentChange(change);
        }

        for( LocalEntitySet set : entitySets ) {
            set.entityChange(change);
        }
    }

    /**
     *  A local DefaultEntitySet subclass only so that we can have
     *  access to some protected methods and potentially hook into some
     *  other stuff.
     */
    protected class LocalEntitySet extends DefaultEntitySet {
        // Keep track of the purged entities caused by switching filters.
        private Set<EntityId> purged = new HashSet<>();

        public LocalEntitySet( EntityData ed, EntityCriteria criteria ) {
            super(ed, criteria);
        }

        public Set<EntityId> getPurgedEntities() {
            return purged;
        }

        @Override
        public void clearChangeSets() {
            super.clearChangeSets();
            purged.clear();
        }

        protected void onEntityPurged( Entity e ) {
            purged.add(e.getId());
        }

        /**
         *  Overridden just for local access.
         */
        @Override
        protected Class<? extends EntityComponent>[] getTypes() {
            return super.getTypes();
        }

        /**
         *  Overridden just for local access.
         */
        @Override
        protected void loadEntities( boolean reload ) {
            super.loadEntities(reload);
        }

        /**
         *  Overridden just for local access.
         */
        @Override
        protected void entityChange( EntityChange change ) {
            super.entityChange(change);
        }

        @Override
        public void release() {
            entitySets.remove(this);
            super.release();
        }
    }

    private class ChangeObserver implements EntityComponentListener {

        @Override
        public void componentChange( EntityChange change ) {
            changes.add(change);
        }
    }
}
