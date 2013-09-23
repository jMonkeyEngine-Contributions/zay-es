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

package trap.game.ai;

import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.HashMap;
import java.util.Map;
import trap.game.Activity;
import trap.game.Direction;
import trap.game.GameSystems;
import trap.game.MazeService;
import trap.game.MonkeyTrapConstants;
import trap.game.Position;
import trap.game.Service;


/**
 *
 *  @author    Paul Speed
 */
public class AiService implements Service {

    private GameSystems systems;
    private EntityData ed;
    private MazeService mazeService;

    private EntitySet mobs;

    private long lastTime; 
    
    public AiService() {
    }

    public void initialize( GameSystems systems ) {
        this.systems = systems;
        this.ed = systems.getEntityData();
        this.mazeService = systems.getService(MazeService.class); 
 
        // Right now we will grab all AI types but we should eventually
        // maybe be selective.
        mobs = ed.getEntities(AiType.class, Position.class);        
    }

    private Map<EntityId, Direction> temp = new HashMap<EntityId, Direction>();

    public void update( long gameTime ) {
    
        mobs.applyChanges();
        for( Entity e : mobs ) {
        
            Activity current = ed.getComponent(e.getId(), Activity.class);
            if( current != null && current.getEndTime() > gameTime ) {
                // Not done with the last move yet
                continue;
            }
 
            // See what the last direction was
            Direction last = temp.get(e.getId());
            if( last == null ) 
                last = Direction.South;            
 
            Position pos = e.get(Position.class);
            Vector3f loc = pos.getLocation();
            int x = (int)(loc.x / 2);
            int y = (int)(loc.z / 2);
 
            // See if we will keep the same direction or not
            // if we are blocked or if a random chance decides we
            // will turn... then we will turn.
            Direction dir = last;
            if( Math.random() < 0.25 || mazeService.isOccupied(dir, x, y) ) {
                dir = Direction.random();
//System.out.println( "Random:" + dir );                
                if( mazeService.isOccupied(dir, x, y) ) {
                    // See if we can find a dir that isn't occupied
                    int i;
                    for( i = 0; i < 4; i++ ) {
                        dir = dir.right(); 
                        if( !mazeService.isOccupied(dir, x, y) )
                            break;
                    }
                    if( i == 4 ) {
                        System.out.println( "No way out right now." );
                        continue;
                    }
                }                
            }
 
            // So if the dir is different then we are turning and
            // not walking.
            if( dir != last ) {
                temp.put(e.getId(), dir);
            
                long actionTimeMs = 200;  // 200 ms  // ogres take longer to turn than monkeys 
                long actionTimeNanos = actionTimeMs * 1000000;
            
                Position next = new Position(pos.getLocation(), dir.getFacing(), gameTime, gameTime + actionTimeNanos);
                Activity act = new Activity(Activity.TURNING, gameTime, gameTime + actionTimeNanos);                                           
                ed.setComponents(e.getId(), next, act);
//System.out.println( "Turning:" + e.getId() + "  to:" + dir + "             at to:" + next );
            } else {
                
                // We move forward               
                double distance = 2.0;       
                long actionTimeMs = (long)(distance/MonkeyTrapConstants.OGRE_SPEED * 1000.0);
                long actionTimeNanos = actionTimeMs * 1000000;
                Position next = new Position(dir.forward(loc, 2),
                                            dir.getFacing(),
                                            gameTime,
                                            gameTime + actionTimeNanos);
                Activity act = new Activity(Activity.WALKING, gameTime, gameTime + actionTimeNanos);                                           
                ed.setComponents(e.getId(), next, act);
//System.out.println( "Moving:" + e.getId() + " to:" + next );
            }                
        }
    }

    public void terminate( GameSystems systems ) {
        mobs.release();
    }
}
