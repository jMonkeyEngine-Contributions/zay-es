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
        return new MapComponentHandler<T>();
    }

    /**
     *  Returns true if a handler has already been resolved for the specified
     *  type.
     */
    protected <T extends EntityComponent> boolean hasHandler( Class<T> type ) {
        return handlers.containsKey(type);
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
        
        // Can now update the entity sets that care
        entityChange(new EntityChange(entityId, type));
        
        return result; 
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

    @SuppressWarnings("unchecked")  // because Java doesn't like generic varargs
    protected DefaultEntitySet createSet( ComponentFilter filter, Class... types ) {
        DefaultEntitySet set = new DefaultEntitySet(this, filter, types);
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
    public EntitySet getEntities( Class... types ) {
    
        DefaultEntitySet results = createSet((ComponentFilter)null, types);
        results.loadEntities(false);
         
        /*
        Should be enough to let the EntitySet load itself.        
        Set<EntityId> first = getEntityIds(types[0]);
        if( first.isEmpty() ) {
            return results;
        } 
        Set<EntityId> and = new HashSet<EntityId>();
        and.addAll(first); 
            
        for( int i = 1; i < types.length; i++ ) {
            and.retainAll(getEntityIds(types[i]));
        }
                              
        // Now we have the info needed to build the entity set
        EntityComponent[] buffer = new EntityComponent[types.length]; 
        for( EntityId id : and ) {
            for( int i = 0; i < buffer.length; i++ ) {
                buffer[i] = getComponent(id, types[i]);
            }
                
            // Now create the entity
            DefaultEntity e = new DefaultEntity(this, id, buffer.clone(), types);
            results.add(e);
        }*/
            
        return results;
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
        
        Set<EntityId> first = getEntityIds(types[0], forType(filter, types[0]));
        if( first.isEmpty() )
            return null; 
        Set<EntityId> and = new HashSet<EntityId>();
        and.addAll(first); 
            
        for( int i = 1; i < types.length; i++ ) {
            Set<EntityId> sub = getEntityIds(types[i], forType(filter, types[i]));
            if( sub.isEmpty() ) {
                return null;
            }  
            and.retainAll(sub);
        }
 
        if( and.isEmpty() )
            return null;
        
        return and.iterator().next();        
    }
 
    @Override
    public Set<EntityId> findEntities( ComponentFilter filter, Class... types ) {
        if( types == null || types.length == 0 ) {
            types = new Class[] { filter.getComponentType() };
        }
        
        Set<EntityId> first = getEntityIds(types[0], forType(filter, types[0]));
        if( first.isEmpty() ) {
            return Collections.emptySet();
        } 
        Set<EntityId> and = new HashSet<EntityId>();
        and.addAll(first); 
            
        for( int i = 1; i < types.length; i++ ) {
            Set<EntityId> sub = getEntityIds(types[i], forType(filter, types[i]));
            if( sub.isEmpty() ) {
                return Collections.emptySet();
            }  
            and.retainAll(sub);
        }
        
        return and;        
    }

    @Override
    public EntitySet getEntities( ComponentFilter filter, Class... types ) {
        DefaultEntitySet results = createSet(filter, types);
        results.loadEntities(false);
        return results;
    }

    @Override
    @SuppressWarnings("unchecked")  // because Java doesn't like generic varargs
    public WatchedEntity watchEntity( EntityId id, Class... types ) {

        // Collect the components    
        /*EntityComponent[] buffer = new EntityComponent[types.length]; 
        for( int i = 0; i < buffer.length; i++ ) {
            buffer[i] = getComponent(id, types[i]);
        }
        
        DefaultWatchedEntity does that itself now
        */
    
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
