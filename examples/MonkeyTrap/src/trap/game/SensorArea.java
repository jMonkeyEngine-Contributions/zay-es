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

import java.util.Arrays;
import java.util.LinkedList;


/**
 *
 *  @version   $Revision$
 */
public class SensorArea {
    private Maze maze;
    private int xCenter;
    private int yCenter;
    private int radius;
    private int radiusSq;
    private boolean[][] visible;
 
    public SensorArea( Maze maze, int radius ) {
        this.maze = maze;
        this.radius = radius;
        this.radiusSq = radius * radius;
        clear();
    }
    
    public SensorArea( Maze maze, int xCenter, int yCenter, int radius ) {
        this( maze, radius );
        
        this.xCenter = xCenter;
        this.yCenter = yCenter;
        scan();
    }
 
    public void setCenter( int x, int y ) {
        this.xCenter = x;
        this.yCenter = y;
        clear();
        scan();
    }
 
    public int getRadius() {
        return radius;
    }
    
    public boolean isVisible( int xWorld, int yWorld ) {
        if( xWorld < xCenter - radius || xWorld > xCenter + radius )  
            return false;
        if( yWorld < yCenter - radius || yWorld > yCenter + radius )  
            return false;
        return visible[xWorld - (xCenter - radius)][yWorld - (yCenter - radius)];
    } 
    
    protected void clear() {
        int size = radius * 2 + 1;
        if( visible == null ) {
            visible = new boolean[size][size];
        } else {
            for( int i = 0; i < size; i++ ) {
                Arrays.fill(visible[i], false);
            }
        }
    }
    
    protected boolean isClear( int xWorld, int yWorld ) {
        float xDelta = xWorld - xCenter;
        float yDelta = yWorld - yCenter;
        if( xDelta == 0 || yDelta == 0 ) {
            // We would have found this a different way if we
            // could see it.
            return false;
        }
 
        float xDist = Math.abs(xDelta);
        float yDist = Math.abs(yDelta);
        int count;
        if( xDist == yDist ) {
            // We can move one square at a time, no problem.
            count = (int)xDist;
            xDelta = xDelta / count;
            yDelta = yDelta / count;
        } else {
            // We will make extra steps to make sure to
            // always cross the adjacent borders.  Sometimes
            // we will hit the same cell more than once but that's
            // a small price to pay for simpler math.
            count = (int)Math.max(xDist, yDist) * 3;
            xDelta = xDelta / count;
            yDelta = yDelta / count;
        }
        
        for( int i = 1; i < count; i++ ) {
            float x = xCenter + 0.5f + i * xDelta;
            float y = yCenter + 0.5f + i * yDelta;
            int value = maze.get((int)x, (int)y);
            if( maze.isSolid(value) ) {
                return false;
            }
        }
        return true;
    }
    
    protected void scan() {
 
        LinkedList<Step> pending = new LinkedList<Step>();
        pending.add(new Step(xCenter, yCenter, 0));

        int size = radius * 2 + 1;
        boolean[][] visited = new boolean[size][size];
        
        while( !pending.isEmpty() ) {
            Step step = pending.removeFirst();
            
            int xDelta = step.x - xCenter;
            int yDelta = step.y - yCenter;
            
            // See if we've exceeded our maximum range then we're done
            int distSq = xDelta * xDelta + yDelta * yDelta;
            if( distSq > radiusSq )
                continue;
            
            // See if we've visited this spot before                               
            int xLocal = step.x - (xCenter - radius);
            int yLocal = step.y - (yCenter - radius);
            if( visited[xLocal][yLocal] )
                continue;
            visited[xLocal][yLocal] = true;
 
            // Now see if it is solid           
            int value = maze.get(step.x, step.y);
            if( maze.isSolid(value) ) {
                continue;
            } 
                       
            // See if it is visible
            if( step.depth <= 2 ) {
                // No way this can't be visible
                visible[xLocal][yLocal] = true;
            } else if( xDelta == 0 && step.depth == Math.abs(yDelta) ) {
                // We are exactly north or south by depth so we must be
                // visible
                visible[xLocal][yLocal] = true;
            } else if( yDelta == 0 && step.depth == Math.abs(xDelta) ) {
                // We are exactly east or west by depth so we must be
                // visible
                visible[xLocal][yLocal] = true;
            } else {
                // Cast a ray to the point and see if we are blocked
                if( isClear(step.x, step.y) ) {
                    visible[xLocal][yLocal] = true;
                } else {
                    // We should not branch
                    continue;
                }
            } 
 
            // Now scan out in the various directions... we'll 
            // let the top of the loop sort out if they are
            // outside the range, blocked, etc..
            pending.add(step.next(Direction.North));    
            pending.add(step.next(Direction.South));    
            pending.add(step.next(Direction.East));    
            pending.add(step.next(Direction.West));    
        }       
    }
    
    private class Step {
        int x;
        int y;
        int depth;
        Direction from;
        
        public Step( int x, int y, int depth ) {
            this.x = x;
            this.y = y;
            this.depth = depth;
        }
        
        public Step next( Direction dir ) {
            int xNew = x + dir.getXDelta();
            int yNew = y + dir.getYDelta();
            Step result = new Step(xNew, yNew, depth+1);
            result.from = dir;
            return result;
        }
        
        public String toString() {
            return "Step[" + x + ", " + y + ", " + depth + ", " + from + "]";
        }
    }
}
