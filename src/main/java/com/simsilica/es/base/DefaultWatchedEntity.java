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

package com.simsilica.es.base;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.simsilica.es.*;


/**
 *  
 *
 *  @author    Paul Speed
 */
public class DefaultWatchedEntity implements WatchedEntity {

    // We implement our own stuff instead of extending DefaultEntity
    // because it's use-case is slightly different and it may not 
    // always contain the stuff we need to do the job here.  For example,
    // right now DefaultEntity contains a Class[] types array but it
    // doesn't use it and doesn't need it.  It will always have valid
    // components and can use them for type checks.  A WatchedEntity may
    // have null values and thus must keep the types array around.
    // I didn't want to add the requirement to DefaultEntity to keep
    // it just to support this isolated use-case.
    
    private final EntityData ed;
    private final EntityId id;
    private final EntityComponent[] components;
    private final Class<EntityComponent>[] types;
    private final Set<Class<EntityComponent>> typeSet;
    private final ChangeProcessor listener;
    private final ConcurrentLinkedQueue<EntityChange> changes = new ConcurrentLinkedQueue<EntityChange>();
    private boolean released;
    
    public DefaultWatchedEntity( EntityData ed, EntityId id, Class<EntityComponent>[] types ) {
        this(ed, id, null, types);
    }
    
    public DefaultWatchedEntity( EntityData ed, EntityId id, EntityComponent[] data, Class<EntityComponent>[] types ) {
        this.ed = ed;
        this.id = id;
        this.components = data == null ? new EntityComponent[types.length] : data;
        this.types = types;
        this.typeSet = new HashSet<>(Arrays.asList(types));
        this.listener = new ChangeProcessor();
        if( ed instanceof ObservableEntityData ) {
            ((ObservableEntityData)ed).addEntityComponentListener(listener);
        }
        if( data == null ) {
            load();
        }
    }
 
    protected final void load() {
        // Collect the components    
        for( int i = 0; i < components.length; i++ ) {
            components[i] = ed.getComponent(id, types[i]);
        }
    }
    
    protected EntityComponentListener getListener() {
        return listener;
    }                                 

    @Override
    public EntityId getId() {
        return id;
    }

    @Override
    public <T extends EntityComponent> T get( Class<T> type ) {
        for( int i = 0; i < types.length; i++ ) {
            if( types[i] == type ) {
                return type.cast(components[i]);
            }
        }
        return null;   
    }

    @Override
    public void set( EntityComponent c ) {
        for( int i = 0; i < types.length; i++ ) {
            if( types[i].isInstance(c) ) {
                ed.setComponent(getId(), c);
                components[i] = c;
                break;
            }
        }
        
        // Still pass it through as a convenience
        ed.setComponent(getId(), c);
    }

    @Override
    public boolean isComplete() {
        for( EntityComponent component : components ) {
            if (component == null) {
                return false;
            }
        }
        return true;            
    }

    @Override
    public EntityComponent[] getComponents() {
        return components;
    }
    
    @Override
    public boolean hasChanges() {
        return !changes.isEmpty();
    }

    @Override
    public boolean applyChanges() {
        return applyChanges(null);
    }

    @Override
    public boolean applyChanges( Set<EntityChange> updates ) {
 
        if( released ) {
            // Changes are irrelevent so just clean up
            changes.clear();
            return false;
        }
 
        // So apply whatever changes there are       
        EntityChange change;
        boolean hasChanges = false;
        while( (change = changes.poll()) != null ) {
            if( applyChange(change) ) {
                hasChanges = true;
                if( updates != null ) {
                    updates.add(change);
                }
            }
        }
        
        return hasChanges;               
    }
    
    protected boolean applyChange( EntityChange change ) {
        // Note: it seems like we could afford to do actual
        //       .equals() value checking here but there is
        //       little point.  If we don't return true for
        //       the change then we risk the caller missing
        //       updates that they set themselves which destroys
        //       typical MVC patterns.
        for( int i = 0; i < types.length; i++ ) {
            if( types[i] == change.getComponentType() ) {
                components[i] = change.getComponent();
                return true;
            }
        }    
        return false;
    }

    @Override
    public void release() {
        released = true;
        if( ed instanceof ObservableEntityData ) {
            ((ObservableEntityData)ed).removeEntityComponentListener(listener);
        }
    }
 
    protected boolean isReleased() {
        return released;
    }
 
    protected void addChange( EntityChange change ) {

        // If it's not for this entity then just ignore it
        if( id.getId() != change.getEntityId().getId() ) {
            return;
        }
            
        // Now that we've done the quick check do the ever slightly
        // more expensive check
        if( !typeSet.contains(change.getComponentType()) ) {
            return;
        }

        // Good enough       
        changes.add(change);
    }

    @Override
    public String toString() {
        return "WatchedEntity[" + id + ", values=" + Arrays.asList(components) + "]";
    }

    private class ChangeProcessor implements EntityComponentListener {
    
        public ChangeProcessor() {
        }
    
        @Override
        public void componentChange( EntityChange change ) {
            addChange(change);
        }
    }
}
