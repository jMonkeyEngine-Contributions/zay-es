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

package trap.game;

import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 *  Watches for entities with MoveTo, Position, and Speed
 *  components and processes their actual movement.
 *  
 *  @author    Paul Speed
 */
public class MovementService implements Service {
 
    static Logger log = LoggerFactory.getLogger(MovementService.class);
    
    private GameSystems systems;
    private EntityData ed;
    private MazeService mazeService;
    private Maze maze;
    
    private EntitySet mobs;
       
    public MovementService() {
    }

    public void initialize( GameSystems systems ) {
        this.systems = systems;
        this.ed = systems.getEntityData();
        this.mazeService = systems.getService(MazeService.class); 
        this.maze = mazeService.getMaze();
 
        mobs = ed.getEntities(MoveTo.class, Position.class, Speed.class);
    }

    public void update( long gameTime ) {
        mobs.applyChanges();
 
        // Keep track of the places that are moved to
        // in this frame so that we can cancel additional moves of
        // mobs into those spaces without having to constantly
        // recalculate from the maze service.
        Set<Vector3f> occupied = new HashSet<Vector3f>();
 
        // The presumption is that the code setting the move to
        // already checked space availability at that time.  We
        // only have to check for availability that changes because
        // of these moves.
 
        log.debug("Doing actual movements...");
                       
        // Perform all movements for all active mobs
        for( Entity e : mobs ) {
            MoveTo to = e.get(MoveTo.class);
if( to == null ) {
    System.out.println( "Incomplete entity:" + e );
}            
            if( to.getTime() > gameTime ) {
                continue;
            }
            
            Position pos = e.get(Position.class);
            Speed speed = e.get(Speed.class);
            
            Direction dir = Direction.fromDelta(pos.getLocation(), to.getLocation());

            // This is a little fragile but we take advantage of
            // the fact that the Position's original direction was
            // set from dir.getFacing()
            if( dir.getFacing().equals(pos.getFacing()) ) {
                // Then we can move
                
                if( log.isDebugEnabled() ) {
                    log.debug("Move:" + e + " to:" + pos);
                }                
 
                // Remove the component because we no longer need it
                ed.removeComponent(e.getId(), MoveTo.class);
 
                if( !occupied.add(to.getLocation()) ) {
                    // Something already moved here... nothing left
                    // to do
                    continue;
                }
                
                // Check the maze service, too because if we delayed
                // moving because of a turn then something might have
                // moved into our spot.
                if( mazeService.isOccupied(to.getLocation()) ) {
                    if( log.isDebugEnabled() ) {
                        log.debug("Already occupied:" + to.getLocation() 
                                    + " by:" + mazeService.getEntities( (int)(to.getLocation().x/2), (int)(to.getLocation().z/2) ) );
                    }                                        
                    // Something already moved here... nothing left
                    // to do
                    continue;
                }
 
                // Right now the distance is always the same so we
                // won't bother calculating it.               
                double stepDistance = 2.0;       
                long actionTimeMs = (long)(stepDistance/speed.getMoveSpeed() * 1000.0);
                long actionTimeNanos = actionTimeMs * 1000000;
                //long time = gameTime; // could also be to.getTime()... I'm torn.
                long time = to.getTime();
                Position next = new Position(to.getLocation(), dir.getFacing(),
                                             time, time + actionTimeNanos);
                Activity act = new Activity(Activity.WALKING, time, time + actionTimeNanos);
                ed.setComponents(e.getId(), next, act);
                
            } else {
                if( log.isDebugEnabled() ) {
                    log.debug("Turn:" + e + " to:" + pos);
                }                
                // We need to turn first
                long actionTimeMs = (long)(0.25/speed.getTurnSpeed() * 1000.0);
                long actionTimeNanos = actionTimeMs * 1000000;
                //aaalong time = gameTime; // could also be to.getTime()... I'm torn.
                long time = to.getTime();
                
                Position next = new Position(pos.getLocation(), dir.getFacing(), 
                                             time, time + actionTimeNanos);
                Activity act = new Activity(Activity.TURNING, time, time + actionTimeNanos);
                ed.setComponents(e.getId(), next, act);
 
                // And reset the move to time to after the turn is done
                e.set(to.newTime(act.getEndTime()));
            }               
        }
    }

    public void terminate( GameSystems systems ) {
        mobs.release();
    }
    
}
