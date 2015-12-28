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

import com.simsilica.es.ComponentFilter;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityChange;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityComponentListener;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.ObservableEntityData;
import com.simsilica.es.StringIndex;
import com.simsilica.es.WatchedEntity;
import com.simsilica.es.base.DefaultEntitySet;
import com.simsilica.es.base.DefaultWatchedEntity;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 *  Wraps a delegate EntityData and passes most calls directly
 *  through but accumulates EventChanges in its own queue to be
 *  applied at a later time.  EntitySets and WatchedEntities are 
 *  created locally to this this EntityDataWrapper so that they
 *  don't get entity change notifications until later snapshot
 *  processing.
 *
 *  <p>The main purpose of this wrapper is to have a consistent
 *  view between the EntityChanges applied and what the EntitySets
 *  have queued.  This facilitates sending appropriate changes
 *  to the client 'mirror' of this data.</p>
 *
 *  @author    Paul Speed
 */
public class EntityDataWrapper implements ObservableEntityData {

    private final ObservableEntityData delegate;
    private final ChangeObserver listener = new ChangeObserver();
    
    private final List<LocalEntitySet> entitySets = new CopyOnWriteArrayList<LocalEntitySet>();
    private final List<EntityComponentListener> entityListeners = new CopyOnWriteArrayList<EntityComponentListener>();      

    private final ConcurrentLinkedQueue<EntityChange> changes = new ConcurrentLinkedQueue<>();
    
    public EntityDataWrapper( ObservableEntityData delegate ) {
        this.delegate = delegate;
        delegate.addEntityComponentListener(listener);
    } 

    /**
     *  Provides direct access to a set's type list to allow efficient mark/sweep
     *  iteration.
     */
    public Class[] getTypes( EntitySet set ) {
        return ((LocalEntitySet)set).getTypes();
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
    public boolean removeComponent( EntityId entityId, Class type ) {
        return delegate.removeComponent(entityId, type);
    }

    @Override
    public <T extends EntityComponent> T getComponent( EntityId entityId, Class<T> type ) {
        return delegate.getComponent(entityId, type);
    }

    @Override
    public Entity getEntity( EntityId entityId, Class... types ) {
        // Ok to just return it because this entity is not tracked or anything
        return delegate.getEntity(entityId, types);
    }

    @Override
    public EntityId findEntity( ComponentFilter filter, Class... types ) {
        return delegate.findEntity(filter, types);
    }

    @Override
    public Set<EntityId> findEntities(ComponentFilter filter, Class... types) {
        return delegate.findEntities(filter, types);
    }

    @Override
    public EntitySet getEntities( Class... types ) {
        return getEntities(null, types);
    }

    @Override
    public EntitySet getEntities( ComponentFilter filter, Class... types ) {
        LocalEntitySet result = new LocalEntitySet(this, filter, types);
        result.loadEntities(false);
        entitySets.add(result);
        return result;   
    }

    @Override
    public WatchedEntity watchEntity( EntityId entityId, Class... types ) {
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
        // Drain the queue, applying all changes to the entity sets
        // and listeners... and keeping track of what we actually
        // applied.  This should keep all of the views consistent and
        // is basically the entire point of this wrapper class.
        if( changes.isEmpty() )
            return false;

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

        public LocalEntitySet( EntityData ed, ComponentFilter filter, Class[] types ) {
            super(ed, filter, types);
        }
 
        /** 
         *  Overridden just for local access.
         */
        @Override
        protected Class[] getTypes() {
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
