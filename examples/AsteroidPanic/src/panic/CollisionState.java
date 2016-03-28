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

package panic;

import com.jme3.app.Application;
import com.jme3.math.Vector3f;
import com.jme3.util.SafeArrayList;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import com.simsilica.lemur.event.BaseAppState;
import java.util.Set;


/**
 *  Keeps track of the list of colliders and performs
 *  collision checks.  The concept is general but the implementation
 *  is currently "sphere" specific as only radius checks are done
 *  using the simplified CollisionShape components.
 *  This watches all entities with Position and CollisionShape components.
 *  Any generated contacts are passed to a ContactHandler which can
 *  deal with them directly or turn them into contact entities or whatever
 *  the game requires.  No default contact resolution is performed at
 *  all and is 100% up to the ContactHandler callback object.
 *  For Asteroid Panic, there is a custom handler that implements some
 *  of the game logic based on collisions rather than further involving
 *  the entity system.  This may change.
 *
 *  @author    Paul Speed
 */
public class CollisionState extends BaseAppState {

    private EntityData ed;
    private EntitySet entities;

    private SafeArrayList<Entity> colliders = new SafeArrayList<>(Entity.class);

    private ContactHandler contactHandler;

    public CollisionState() {
    }

    public CollisionState( ContactHandler contactHandler ) {
        this.contactHandler = contactHandler;
    }

    public void setContactHandler( ContactHandler handler ) {
        if( this.contactHandler != null ) {
            this.contactHandler.setCollisionState(null);
        }
        this.contactHandler = handler;
        if( this.contactHandler != null && isInitialized() ) {
            this.contactHandler.setCollisionState(this);
        }
    }

    public ContactHandler getContactHandler() {
        return contactHandler;
    }

    protected void addColliders( Set<Entity> set ) {
        colliders.addAll(set);
    }

    protected void removeColliders( Set<Entity> set ) {
        colliders.removeAll(set);
    }

    protected void generateContacts( Entity e1, Entity e2 ) {
        Position p1 = e1.get(Position.class);
        Position p2 = e2.get(Position.class);
        CollisionShape s1 = e1.get(CollisionShape.class);
        float r1 = s1.getRadius();
        CollisionShape s2 = e2.get(CollisionShape.class);
        float r2 = s2.getRadius();
        float threshold = r1 + r2;
        threshold *= threshold;

        float distSq = p1.getLocation().distanceSquared(p2.getLocation());
        if( distSq > threshold ) {
            return; // no collision
        }

        // Calculate the contact values...

        // Find the contact normal.
        Vector3f cn = p2.getLocation().subtract(p1.getLocation());
        float dist = cn.length();
        cn.multLocal(1/dist); // normalize it

        // Positive if penetrating
        float penetration = (r1 + r2) - dist;

        // Calculate a contact point half-way along the penetration
        Vector3f cp = p1.getLocation().add( cn.mult(r1 - penetration * 0.5f) );

        contactHandler.handleContact(e1, e2, cp, cn, penetration);
    }

    protected void generateContacts() {

        if( contactHandler == null )
            return;

        Entity[] array = colliders.getArray();
        for( int i = 0; i < array.length; i++ ) {
            Entity e1 = array[i];
            for( int j = i + 1; j < array.length; j++ ) {
                Entity e2 = array[j];
                generateContacts(e1, e2);
            }
        }
    }

    @Override
    protected void initialize( Application app ) {
        ed = getState(EntityDataState.class).getEntityData();
        entities = ed.getEntities(Position.class, CollisionShape.class);

        if( contactHandler != null ) {
            contactHandler.setCollisionState(this);
        }
    }

    @Override
    protected void cleanup( Application app ) {
        // Release the entity set we grabbed previously
        entities.release();
        entities = null;

        if( contactHandler != null ) {
            contactHandler.setCollisionState(null);
        }
    }

    @Override
    protected void enable() {
        entities.applyChanges();
        addColliders(entities);
    }

    @Override
    protected void disable() {
        removeColliders(entities);
    }

    @Override
    public void update( float tpf ) {
        if( entities.applyChanges() ) {
            removeColliders(entities.getRemovedEntities());
            addColliders(entities.getAddedEntities());
        }

        generateContacts();
    }

}



