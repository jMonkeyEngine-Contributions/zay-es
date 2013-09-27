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

import trap.game.Direction;
import trap.game.Position;
import trap.game.Activity;
import trap.game.Maze;
import trap.game.MonkeyTrapConstants;
import trap.game.TimeProvider;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import trap.game.GameSystems;
import trap.game.HitPoints;
import trap.game.MazeService;
import trap.game.MoveTo;


/**
 *
 *  @author    Paul Speed
 */
public class SinglePlayerClient implements GameClient
{
    private GameSystems systems;
    private EntityData ed;
    private EntityId player;
    private long frameDelay = 100 * 1000000L; // 100 ms 
    private long renderTime;

    private Direction currentDir = Direction.South;
    private long nextMove = 0;
    
    private Maze maze;
    private MazeService mazeService;    

    public SinglePlayerClient( GameSystems systems ) {
        this.systems = systems;        
    }
 
    // Single player specific
    public void start() {
        systems.start();
 
        this.ed = systems.getEntityData();
        this.mazeService = systems.getService(MazeService.class); 
        this.maze = mazeService.getMaze();
        
        // Create a single player entity (maybe here only temporarily)
        player = ed.createEntity();

        // Use the maze seed as starting position
        Vector3f location = new Vector3f(maze.getXSeed() * 2, 0, maze.getYSeed() * 2);
        System.out.println( "Setting player to location:" + location );
        ed.setComponent(player, new Position(location, -1, -1));        
        ed.setComponent(player, MonkeyTrapConstants.TYPE_MONKEY);   
        ed.setComponent(player, MonkeyTrapConstants.SPEED_MONKEY);             
        ed.setComponent(player, new HitPoints(MonkeyTrapConstants.MONKEY_HITPOINTS));       
    }
    
    public void close() {
        systems.stop();
    }
    
    public final long getGameTime() {
        return systems.getGameTime();
    }
   
    public final long getRenderTime() {
        return renderTime; //System.nanoTime() - frameDelay;
    }

    public void updateRenderTime() {
        renderTime = getGameTime() - frameDelay;
    }
    
    public TimeProvider getRenderTimeProvider() {
        return new TimeProvider() {
                public final long getTime() {
                    return getRenderTime();
                }
            };
    }
    
    public EntityData getEntityData() {
        return ed;
    }
    
    public EntityId getPlayer() {
        return player;
    }
    
    public void move( Direction dir ) {
        long time = getGameTime();
        if( time < nextMove ) {
            return;
        }
 
        // See if we have a move or activity
        MoveTo move = ed.getComponent(player, MoveTo.class);
        if( move != null ) {
//System.out.println( "moving " + (time/1000000.0) );        
            return;
        }
        Activity activity = ed.getComponent(player, Activity.class);
//System.out.println( "activity:" + activity );

        // Seems like the grace period is not needed but I will leave
        // it just in case.        
        long gracePeriod = 0; // 200 * 1000000L;
        if( activity != null && (time + gracePeriod) < activity.getEndTime() ) {
//System.out.println( "Bdddusy doing activity until:" + (activity.getEndTime()/1000000.0) + "  " + (time/1000000.0) );                
//            nextMove = activity.getEndTime();
            return; 
        }  
//System.out.println( "Now:" + (time/1000000.0) );
 
        if( dir == currentDir ) {       
            Position current = ed.getComponent(player, Position.class);
            Vector3f loc = current.getLocation();
            int x = (int)(loc.x / 2);
            int y = (int)(loc.z / 2);            
            int value = maze.get(dir, x, y);            
            if( maze.isSolid(value) )
                return;
 
            // Else see if there is something to attack.
            // Note: in multiplayer this check would be done on the server
            //       and not the client, so would still have access to the
            //       maze service.
            if( mazeService.isOccupied(dir, x, y) ) {
                // Attack instead of move
                System.out.println( "ATTACK!!!" );
                return;
            }                
 
            long moveTime = activity != null ? activity.getEndTime() : time;
            moveTime = Math.max(moveTime, time);
            
            //System.out.println( "Trying to move to:" + dir.forward(loc, 2) + "  at:" + (moveTime/1000000.0) ); 
            ed.setComponent(player, new MoveTo(dir.forward(loc, 2), moveTime));
            
            // Just give it a little time to catch up
            //nextMove = time + 100 * 1000000L;
            
            //mob.setComponents(new MoveTo(dir.forward(loc, 2), time));            
            /*
                
            double distance = 2.0;       
            long actionTimeMs = (long)(distance/MonkeyTrapConstants.MONKEY_MOVE_SPEED * 1000.0);
            long actionTimeNanos = actionTimeMs * 1000000;
            
            Position next = new Position(dir.forward(loc, 2),
                                         dir.getFacing(),
                                         time,
                                         time + actionTimeNanos);
            Activity act = new Activity(Activity.WALKING, time, time + actionTimeNanos);                                           
            ed.setComponents(player, next, act);
            nextMove = time + actionTimeNanos;// * 1000000L;*/
        } else {
            if( activity != null && time < activity.getEndTime() ) {
                return;
            }
 
            // Note: it might be better to move the decision to turn
            // or move to the control side, ie: give two separate methods
            // and let the control interface decide.  Gives move options
            // now that the movement service is used and will automatically
            // turn the player anyway.  This turn is just for when they want
            // to turn and not move.
 
//System.out.println( "Turning." );            
            // Change the dir first... but that's quicker
            currentDir = dir;
            Position current = ed.getComponent(player, Position.class);
            
            long actionTimeMs = 100;  // 100 ms 
            long actionTimeNanos = actionTimeMs * 1000000;
            
            long moveTime = activity != null ? activity.getEndTime() : time;
            moveTime = Math.max(moveTime, time);
            
            Position next = new Position(current.getLocation(), dir.getFacing(), moveTime, moveTime + actionTimeNanos);
            Activity act = new Activity(Activity.TURNING, moveTime, moveTime + actionTimeNanos);                                           
            ed.setComponents(player, next, act);
            nextMove = time + actionTimeNanos;// * 1000000L;             
        }       
    }
}
