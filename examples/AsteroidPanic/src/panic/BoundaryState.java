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
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;


/**
 *  Watches all entities with Position and Model components
 *  and then moves them to the opposite side of the screen if
 *  they exceed the boundary.
 *
 *  @author    Paul Speed
 */
public class BoundaryState extends BaseAppState {

    private EntityData ed;
    private EntitySet entities;
    private float margin;
    private Vector3f min;
    private Vector3f max;

    public BoundaryState( float margin ) {
        this.margin = margin;
    }

    @Override
    protected void initialize( Application app ) {

        ed = getState(EntityDataState.class).getEntityData();
        entities = ed.getEntities(Position.class, ModelType.class);

        // Calculate how big min/max needs to be to incorporate
        // the full view + margin at z = 0
        Camera cam = app.getCamera();
        float z = cam.getViewToProjectionZ( cam.getLocation().z );
        Vector3f worldMin = cam.getWorldCoordinates( new Vector2f(0,0), z);
        Vector3f worldMax = cam.getWorldCoordinates( new Vector2f(cam.getWidth(),cam.getHeight()), z);
        min = worldMin.addLocal( -margin, -margin, 0 );
        max = worldMax.addLocal( margin, margin, 0 );
    }

    @Override
    protected void cleanup( Application app ) {
        // Release the entity set we grabbed previously
        entities.release();
        entities = null;
    }


    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }

    @Override
    public void update( float tpf ) {

        entities.applyChanges();
        for( Entity e : entities ) {
            // if the entity is off one side or the
            // other then teleport it
            Position pos = e.get(Position.class);
            Vector3f loc = pos.getLocation();

            boolean changed = false;
            if( loc.x < min.x ) {
                loc.x += max.x - min.x;
                changed = true;
            } else if( loc.x > max.x ) {
                loc.x -= max.x - min.x;
                changed = true;
            }

            if( loc.y < min.y ) {
                loc.y += max.y - min.y;
                changed = true;
            } else if( loc.y > max.y ) {
                loc.y -= max.y - min.y;
                changed = true;
            }

            if( changed ) {
                e.set(new Position(loc, pos.getFacing()));
            }
        }
    }

}
