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
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;


/**
 *  Watches entities with Position and Velocity components
 *  and "integrates" their position over time.  This is a general
 *  system that is not specific to Asteroid Panic and could be
 *  used for any simple physics-based game.
 *
 *  @author    Paul Speed
 */
public class PhysicsState extends BaseAppState {

    private EntityData ed;
    private EntitySet entities;
    private long lastFrame;

    @Override
    protected void initialize( Application app ) {

        ed = getState(EntityDataState.class).getEntityData();
        entities = ed.getEntities(Position.class, Velocity.class);
    }

    @Override
    protected void cleanup( Application app ) {
        // Release the entity set we grabbed previously
        entities.release();
        entities = null;
    }

    @Override
    protected void onEnable() {
        lastFrame = System.nanoTime();
    }

    @Override
    protected void onDisable() {
    }

    private Quaternion addScaledVector( Quaternion orientation, Vector3f v, double scale ) {

        double x = orientation.getX();
        double y = orientation.getY();
        double z = orientation.getZ();
        double w = orientation.getW();

        Quaternion q = new Quaternion((float)(v.x * scale), (float)(v.y * scale), (float)(v.z * scale), 0);
        q.multLocal(orientation);

        x = x + q.getX() * 0.5;
        y = y + q.getY() * 0.5;
        z = z + q.getZ() * 0.5;
        w = w + q.getW() * 0.5;

        return new Quaternion((float)x,(float)y,(float)z,(float)w);
    }

    protected void integrate(double tpf) {

        // Make sure we have the latest set but we
        // don't really care who left or joined
        entities.applyChanges();
        for( Entity e : entities ) {
            Position pos = e.get(Position.class);
            Velocity vel = e.get(Velocity.class);

            Vector3f loc = pos.getLocation();
            Vector3f linear = vel.getLinear();
            loc = loc.add( (float)(linear.x * tpf),
                           (float)(linear.y * tpf),
                           (float)(linear.z * tpf) );

            // A little quaternion magic for adding rotational
            // velocity to orientation
            Quaternion orientation = pos.getFacing();
            orientation = addScaledVector(orientation, vel.getAngular(), tpf);
            orientation.normalizeLocal();

            e.set(new Position(loc, orientation));
        }
    }

    @Override
    public void update( float tpf ) {

        // Use our own tpf calculation in case frame rate is
        // running away making this tpf unstable
        long time = System.nanoTime();
        long delta = time - lastFrame;
        lastFrame = time;
        if( delta == 0 ) {
            return; // no update to perform
        }

        double seconds = delta / 1000000000.0;

        // Clamp frame time to no bigger than a certain amount
        // to prevent physics errors.  A little jitter for slow frames
        // is better than tunneling/ghost objects
        if( seconds > 0.1 ) {
            seconds = 0.1;
        }

        integrate(seconds);
    }
}
