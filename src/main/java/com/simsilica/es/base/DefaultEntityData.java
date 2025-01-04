/*
 * $Id: DefaultEntityData.java 1580 2015-03-01 07:28:10Z PSpeed42@gmail.com $
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
import java.util.concurrent.*;

import org.slf4j.*;

import com.simsilica.util.ReportSystem;
import com.simsilica.util.Reporter;

import com.simsilica.es.*;

/**
 *
 *  @author    Paul Speed
 */
public class DefaultEntityData implements ObservableEntityData {

    static Logger log = LoggerFactory.getLogger(DefaultEntityData.class);

    private final Map<Class<? extends EntityComponent>, ComponentHandler> handlers = new ConcurrentHashMap<>();
    private EntityIdGenerator idGenerator;
    private StringIndex stringIndex;
    private EntityDataStats stats = new NoopEntityDataStats();

    /**
     *  Keeps the unreleased entity sets so that we can give
     *  them the change updates relevant to them.
     */
    private final List<DefaultEntitySet> entitySets = new CopyOnWriteArrayList<>();
    private final List<EntityComponentListener> entityListeners = new CopyOnWriteArrayList<>();

    public DefaultEntityData() {
        this(new DefaultEntityIdGenerator());
    }

    public DefaultEntityData( EntityIdGenerator idGenerator ) {
        ReportSystem.registerCacheReporter(new EntitySetsReporter());
        this.idGenerator = idGenerator;

        // If we haven't been extended then go ahead and create a
        // default string index
        if( getClass() == DefaultEntityData.class ) {
            this.stringIndex = new MemStringIndex();
        }
    }

    public void setEntityDataStats( EntityDataStats stats ) {
        this.stats = stats;
    }

    public EntityDataStats getEntityDataStats() {
        return stats;
    }

    protected void setIdGenerator( EntityIdGenerator idGenerator ) {
        this.idGenerator = idGenerator;
    }

    protected void setStringIndex( StringIndex stringIndex ) {
        this.stringIndex = stringIndex;
    }

    protected <T extends EntityComponent> void registerComponentHandler( Class<T> type, ComponentHandler<T> handler ) {
        handlers.put(type, handler);
    }

    @Override
    public void addEntityComponentListener( EntityComponentListener l ) {
        if( l == null ) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        entityListeners.add(l);
    }

    @Override
    public void removeEntityComponentListener( EntityComponentListener l ) {
        entityListeners.remove(l);
    }

    @Override
    public void close() {
    }

    @Override
    public EntityId createEntity() {
        return new EntityId(idGenerator.nextEntityId());
    }

    @Override
    public void removeEntity( EntityId entityId ) {
        if( log.isTraceEnabled() ) {
            log.trace("removeEntity(" + entityId + ")");
        }
        // Note: because we only add the ComponentHandlers when
        // we encounter the component types... it's possible that
        // the entity stays orphaned with a few components if we
        // have never accessed any of them.  SqlEntityData should
        // probably specifically be given types someday.  FIXME

        // Remove all of its components
        for( Class<? extends EntityComponent> c : handlers.keySet() ) {
            removeComponent(entityId, c);
        }
    }

    @Override
    public StringIndex getStrings() {
        return stringIndex;
    }

    /**
     *  When no specific type handler exists, this attempts to
     *  find an appropriate handler.  Default implementation returns
     *  a new MapComponentHandler.
     */
    protected <T extends EntityComponent> ComponentHandler<T> lookupDefaultHandler( Class<T> type ) {
        return new MapComponentHandler<T>(type);
    }

    /**
     *  Returns true if a handler has already been resolved for the specified
     *  type.
     */
    protected <T extends EntityComponent> boolean hasHandler( Class<T> type ) {
        return handlers.containsKey(type);
    }

    protected Map<Class<? extends EntityComponent>, ComponentHandler> getComponentHandlers() {
        return handlers;
    }

    @SuppressWarnings("unchecked")
    protected <T extends EntityComponent> ComponentHandler<T> getHandler( Class type ) {

        ComponentHandler result = handlers.get(type);
        if( result == null ) {
            // A little double checked locking to make sure we
            // don't create a handler twice
            synchronized( this ) {
                result = handlers.get(type);
                if( result == null ) {
                    result = lookupDefaultHandler(type);
                    handlers.put(type, result);
                }
            }
        }
        return (ComponentHandler<T>)result;
    }

    @Override
    public <T extends EntityComponent> T getComponent( EntityId entityId, Class<T> type ) {
        if( entityId == null ) {
            throw new IllegalArgumentException("EntityId cannot be null.");
        }
        ComponentHandler<T> handler = getHandler(type);
        return handler.getComponent(entityId);
    }

    @Override
    public <T extends EntityComponent> void setComponent( EntityId entityId, T component ) {
        if( entityId == null ) {
            throw new IllegalArgumentException("EntityId cannot be null.");
        }
        ComponentHandler<T> handler = getHandler(component.getClass());
        handler.setComponent(entityId, component);

        // Can now update the entity sets that care
        entityChange(new EntityChange(entityId, component));
    }

    @Override
    public <T extends EntityComponent> boolean removeComponent( EntityId entityId, Class<T> type ) {
        if( entityId == null ) {
            throw new IllegalArgumentException("EntityId cannot be null.");
        }
        if( log.isTraceEnabled() ) {
            log.trace("removeComponent(" + entityId + ", " + type + ")");
        }
        ComponentHandler handler = getHandler(type);
        boolean result = handler.removeComponent(entityId);

        // 2022-11-06 - Adding a check based on problem report #27 which
        // indicates that we end up adding an EntityChange for every component type
        // when calling removeEntity().  As long as we trust handler.removeComponent()
        // to return a proper true/false then we should avoid sendind the entity
        // change on false.  At best it's redundant.
        if( result ) {
            // Can now update the entity sets that care
            entityChange(new EntityChange(entityId, type));
        }

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void removeComponents( EntityId entityId, Class... types ) {
        for (Class type : types) {
            removeComponent(entityId, type);
        }
    }

    protected EntityId findSingleEntity( ComponentFilter filter ) {
        return getHandler(filter.getComponentType()).findEntity(filter);
    }

    protected Set<EntityId> getEntityIds( Class type ) {
        return getHandler(type).getEntities();
    }

    protected Set<EntityId> getEntityIds( Class type, ComponentFilter filter ) {
        return getHandler(type).getEntities(filter);
    }

    protected DefaultEntitySet createSet( EntityCriteria criteria ) {
        DefaultEntitySet set = new DefaultEntitySet(this, criteria);
        entitySets.add(set);
        return set;
    }

    protected void replace( Entity e, EntityComponent oldValue, EntityComponent newValue ) {
        setComponent(e.getId(), newValue);
    }

    @Override
    public void setComponents( EntityId entityId, EntityComponent... components ) {
        for( EntityComponent c : components ) {
            setComponent(entityId, c);
        }
    }

    @Override
    @SuppressWarnings("unchecked")  // because Java doesn't like generic varargs
    public Entity getEntity( EntityId entityId, Class... types ) {
        EntityComponent[] values = new EntityComponent[types.length];
        for( int i = 0; i < values.length; i++ ) {
            values[i] = getComponent(entityId, types[i]);
        }
        return new DefaultEntity( this, entityId, values, types );
    }

    @Override
    @SuppressWarnings("unchecked") // because Java doesn't like generic varargs
    public EntitySet getEntities( Class... types ) {
        return getEntities(new EntityCriteria().add(types));
    }

    protected ComponentFilter forType( ComponentFilter filter, Class type ) {
        if( filter == null || filter.getComponentType() != type )
            return null;
        return filter;
    }

    @Override
    public EntityId findEntity( ComponentFilter filter, Class... types ) {
        if( types == null || types.length == 0 ) {
            return findSingleEntity(filter);
        }
        return findEntity(new EntityCriteria().set(filter, types));
    }

    @Override
    public EntityId findEntity( EntityCriteria criteria ) {
        Query query = createQuery(criteria);
        long start = System.nanoTime();
        boolean found = false;
        try {
            EntityId result = query.findFirst();
            found = result != null;
            return result;
        } finally {
            long delta = System.nanoTime() - start;
            stats.findEntity(delta, criteria, query, found);
        }
    }

    @Override
    public Set<EntityId> findEntities( ComponentFilter filter, Class... types ) {
        return findEntities(new EntityCriteria().set(filter, types));
    }

    @Override
    public <T extends EntityComponent> Query createQuery( ComponentFilter<T> filter, Class<T> type ) {
        ComponentHandler<T> handler = getHandler(type);
        return handler.createQuery(filter);
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
        Query query = createQuery(criteria);
        long start = System.nanoTime();
        int size = 0;
        try {
            Set<EntityId> results = query.execute();
            size = results.size();
            return results;
        } finally {
            long delta = System.nanoTime() - start;
            stats.findEntities(delta, criteria, query, size);
        }
    }

    @Override
    public EntitySet getEntities( ComponentFilter filter, Class... types ) {
        return getEntities(new EntityCriteria().set(filter, types));
    }

    @Override
    public EntitySet getEntities( EntityCriteria criteria ) {
        DefaultEntitySet results = createSet(criteria);
        results.loadEntities(false);
        return results;
    }

    @Override
    @SuppressWarnings("unchecked")  // because Java doesn't like generic varargs
    public WatchedEntity watchEntity( EntityId id, Class... types ) {
        return new DefaultWatchedEntity(this, id, types);
    }

    protected void releaseEntitySet( EntitySet entities ) {
        entitySets.remove((DefaultEntitySet)entities);
    }

    protected void entityChange( EntityChange change ) {

        for( EntityComponentListener l : entityListeners ) {
            l.componentChange(change);
        }

        for( DefaultEntitySet set : entitySets ) {
            set.entityChange(change);
        }
    }

    private class EntitySetsReporter implements Reporter {

        @Override
        public void printReport( String type, java.io.PrintWriter out ) {
            out.println("EntityData->EntitySets:" + entitySets.size());
        }
    }
}
