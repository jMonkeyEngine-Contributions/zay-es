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

package trap;

import com.jme3.app.Application;
import com.jme3.scene.Spatial;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import com.simsilica.lemur.event.BaseAppState;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trap.game.Dead;
import trap.game.ModelType;
import trap.game.Position;
import trap.game.TimeProvider;


/**
 *  Applies a death animation or effect to an entity's model 
 *  based on the "Death" state of that entity.
 *
 *  @author    Paul Speed
 */
public class DeathState extends BaseAppState {

    static Logger log = LoggerFactory.getLogger( DeathState.class );

    private TimeProvider time;
    private EntityData ed;
    private EntitySet entities;
    private ModelState modelState;

    public DeathState( TimeProvider time ) { 
        this.time = time;
    }

    protected void addDeathState( Set<Entity> set ) {

        long now = time.getTime();
        
        for( Entity e : set ) {
            Dead dead = e.get(Dead.class);
            
            Spatial s = modelState.getSpatial(e.getId());
            if( s == null ) {
                log.warn("No model found for added entity:" + e);
                continue;
            }

            // See if the spatial has the right control
            TaskControl tasks = s.getControl(TaskControl.class);
            if( tasks == null ) {
                continue;
            }
            
            // Else start the death effect
            tasks.playTask("Death", dead.getTime(), dead.getTime() + 2 * 1000 * 1000000L); 
        }
    }

    @Override
    protected void initialize( Application app ) {

        modelState = getState(ModelState.class);

        // Grab the set of entities we are interested in
        ed = getState(EntityDataState.class).getEntityData();
        entities = ed.getEntities(Position.class, ModelType.class, Dead.class);
    }

    @Override
    protected void cleanup( Application app ) {

        // Release the entity set we grabbed previously
        entities.release();
        entities = null;
    }

    @Override
    protected void enable() {
        entities.applyChanges();
        addDeathState(entities);
    }

    @Override
    public void update( float tpf ) {
        if( entities.applyChanges() ) {
            addDeathState(entities.getAddedEntities());
        }
    }

    @Override
    protected void disable() {
    }

}
