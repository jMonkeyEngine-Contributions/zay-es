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
import trap.game.Activity;
import trap.game.Direction;
import trap.game.MazeService;
import trap.game.MoveTo;
import trap.game.Position;


/**
 *  Randomly wanders around without any real strategy.
 *
 *  @author    Paul Speed
 */
public class RandomWanderState implements State {

    public void enter( StateMachine fsm, Mob mob ) {
    }
    
    public void execute( StateMachine fsm, long time, Mob mob ) {
 
        Activity current = mob.getComponent(Activity.class);
        MoveTo moving = mob.getComponent(MoveTo.class);
        if( moving != null || (current != null && current.getEndTime() >= time) ) {
            // Not done with the last activity yet
            return;
        }
 
        // See what the last direction was
        Direction last = mob.get("lastDirection");
        if( last == null ) {
            last = Direction.random();
//            mob.set("distance", 0);
        }
//        Integer distance = mob.get("distance");
        
        Position pos = mob.getPosition();
        Vector3f loc = pos.getLocation();
        int x = (int)(loc.x / 2);
        int y = (int)(loc.z / 2);
        
        // See if we will keep the same direction or not
        // if we are blocked or if a random chance decides we
        // will turn... then we will turn.
        Direction dir = last;
        MazeService mazeService = fsm.getSystems().getService(MazeService.class);
        //if( (distance < 1 && Math.random() < 0.25) || mazeService.isOccupied(dir, x, y) ) {
        if( (Math.random() < 0.25) || mazeService.isOccupied(dir, x, y) ) {
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
                    return;
                }
            }                
        }
 
        // Set the desire to move to the specific location and 
        // let the movement system work out turning and stuff
        mob.setComponents(new MoveTo(dir.forward(loc, 2), time));
 
 /*                      
        // So if the dir is different then we are turning and
        // not walking.
        if( dir != last ) {
            mob.set("lastDirection", dir);
            mob.set("distance", 0);
            
            long actionTimeMs = 200;  // 200 ms  // ogres take longer to turn than monkeys 
            long actionTimeNanos = actionTimeMs * 1000000;
            
            Position next = new Position(pos.getLocation(), dir.getFacing(), time, time + actionTimeNanos);
            Activity act = new Activity(Activity.TURNING, time, time + actionTimeNanos);                                           
            mob.setComponents(next, act);
//System.out.println( "Turning:" + e.getId() + "  to:" + dir + "             at to:" + next );
        } else {
                
            mob.set("lastDirection", dir);
            mob.set("distance", distance + 1);
            
            // We move forward               
            double stepDistance = 2.0;       
            long actionTimeMs = (long)(stepDistance/MonkeyTrapConstants.OGRE_MOVE_SPEED * 1000.0);
            long actionTimeNanos = actionTimeMs * 1000000;
            Position next = new Position(dir.forward(loc, 2),
                                         dir.getFacing(),
                                         time,
                                         time + actionTimeNanos);
            Activity act = new Activity(Activity.WALKING, time, time + actionTimeNanos);
            mob.setComponents(next, act);
//System.out.println( "Moving:" + e.getId() + " to:" + next );
        }*/                       
    }
    
    public void leave( StateMachine fsm, Mob mob ) {
    }
}
