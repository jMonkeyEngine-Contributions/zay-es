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
import trap.game.MonkeyTrapConstants;
import trap.game.Position;


/**
 *  This state attempts to algorithmically cover the
 *  whole maze by essentially dragging a hand against
 *  the left wall.  If it detects a loop then it switches
 *  hands but the maze generator often generates mazes
 *  with intersecting loops that will still confuse
 *  this approach.  Not only that, two meeting wanderers
 *  will block each other... if mobsRedirect is false.
 *  In any case, on its own this wander state will get
 *  into loops a lot.  Might want to detect frequent
 *  loops and temporarily switch to random or cast out
 *  to an unvisited cell.
 *
 *  @author    Paul Speed
 */
public class SurveyWanderState implements State {

    private boolean mobsRedirect = true;

    public void enter( StateMachine fsm, Mob mob ) {
    }
    
    public void execute( StateMachine fsm, long time, Mob mob ) {
 
        Activity current = mob.getComponent(Activity.class);
        if( current != null && current.getEndTime() > time ) {
            // Not done with the last activity yet
            return;
        }
 
        // See what the last direction was
        Direction last = mob.get("lastDirection");
        if( last == null ) {
            mob.set("distance", 0);
        }
        Integer distance = mob.get("distance");
        boolean leftHand = mob.get("leftHand", true);

        Position pos = mob.getPosition();
        Vector3f loc = pos.getLocation();
        int x = (int)(loc.x / 2);
        int y = (int)(loc.z / 2);

        Direction dir = last != null ? last : Direction.South;
        
        // First we will always try to go left if we have gone even
        // one step in this direction
        MazeService mazeService = fsm.getSystems().getService(MazeService.class);
        if( distance > 0 || mazeService.isBlocked(dir, x, y, mobsRedirect) ) {  
            dir = leftHand ? dir.left() : dir.right();
            // If that direction is blocked then we go right from it
            // until we find an unblocked direction.  We treat
            // mobs differently than blocks.
            if( mazeService.isBlocked(dir, x, y, mobsRedirect) ) {
                int i;
                for( i = 0; i < 4; i++ ) {
                    dir = leftHand ? dir.right() : dir.left(); 
                    if( !mazeService.isBlocked(dir, x, y, mobsRedirect) ) {
                        break;
                    }
                }
                if( i == 4 ) {
                    System.out.println( "No way out.   " + mob.getEntity().getId() );
                    return;
                }
            }
        }

        // Once we've picked a direction to go, see if we've looped
        Vector3f start = mob.get("start");
        Direction startDir = mob.get("startDir");
        if( start == null ) {
            // keep the starting position and direction
            mob.set( "start", loc );
            mob.set( "startDir", dir );
            mob.set( "traveled", 0 );
        } 
        Integer traveled = mob.get("traveled");
        if( traveled > 0 && startDir == dir && start.equals(loc) ) {
            traveled = 0;
            mob.set( "traveled", 0 );            
            // Flip the hand rule
            mob.set( "leftHand", !leftHand );
            mob.set( "start", null );
        } 
 
        // So, if we are blocked then we wait instead of trying to move
        if( mazeService.isOccupied(dir, x, y) ) {        
            long actionTimeMs = 100;  // 100 ms  just wait a bit
            long actionTimeNanos = actionTimeMs * 1000000;            
            Activity act = new Activity(Activity.WAITING, time, time + actionTimeNanos);                                           
            mob.setComponents(act);
        } else if( dir != last ) {
            // So if the dir is different then we are turning and
            // not walking.
            mob.set("lastDirection", dir);
            mob.set("distance", 0);
            
            long actionTimeMs = 200;  // 200 ms  // ogres take longer to turn than monkeys 
            long actionTimeNanos = actionTimeMs * 1000000;
            
            Position next = new Position(pos.getLocation(), dir.getFacing(), time, time + actionTimeNanos);
            Activity act = new Activity(Activity.TURNING, time, time + actionTimeNanos);                                           
            mob.setComponents(next, act);
        } else {
            // We move forward               
            mob.set("lastDirection", dir);
            mob.set("distance", distance + 1);
            mob.set("traveled", traveled + 1);
            double stepDistance = 2.0;       
            long actionTimeMs = (long)(stepDistance/MonkeyTrapConstants.OGRE_SPEED * 1000.0);
            long actionTimeNanos = actionTimeMs * 1000000;
            Position next = new Position(dir.forward(loc, 2),
                                         dir.getFacing(),
                                         time,
                                         time + actionTimeNanos);
            Activity act = new Activity(Activity.WALKING, time, time + actionTimeNanos);
            mob.setComponents(next, act);
        }                       
    }
    
    public void leave( StateMachine fsm, Mob mob ) {
    }
}
