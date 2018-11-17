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
import java.util.*;

/**
 *  Simple random maze.
 *  
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class Maze
{
    private int xSize;
    private int ySize;
    private int[][] cells;
    private int xSeed;
    private int ySeed;
 
    private transient Random random;   
    private transient LinkedList<Move> pending = new LinkedList<>();
 
    // Note: north = -y ... so we build from the bottom up
    // ...should get these from Direction now, really.   
    private static int[][] dirs = { {0,-1}, {0,1}, {1,0}, {-1,0} };
    private static int[] reverse = { 1, 0, 3, 2 };
 
    private double breakthroughPenaltyStraight = 0.5;
    private double breakthroughPenaltySides = 0.75;
    private double baseForwardChance = 0.75;
    private double baseSideChance = 0.5;
    private double awayForwardChance = 0.9;
    private double teeBranchChance = 0.75;
    private double sidePenalty = 0.25;    
    
    public Maze( int xSize, int ySize ) {
        this.xSize = xSize;
        this.ySize = ySize;
        this.cells = new int[xSize][ySize];
    }
 
    public Maze( int xSize, int ySize, int[][] cells ) {
        this.xSize = xSize;
        this.ySize = ySize;
        this.cells = cells;
    }
 
    public int getXSeed() {
        return xSeed;
    }
    
    public int getYSeed() {
        return ySeed;
    }
    
    public int getWidth() {
        return xSize;       
    }
    
    public int getHeight() {
        return ySize;
    }
 
    public final int get( int x, int y ) {
        if( x < 1 || y < 1 ) {
            return -1;
        }
        if( x >= xSize - 1 || y >= ySize - 1 ) {
            return -1;
        }
        return cells[x][y];
    }

    public final int get( Direction dir, int x, int y ) {
        return get(dir.ordinal(), x, y);
    }

    public final int get( int dir, int x, int y ) {
        x += dirs[dir][0];
        y += dirs[dir][1];
        if( x < 1 || y < 1 ) {
            return -1;
        }
        if( x >= xSize - 1 || y >= ySize - 1 ) {
            return -1;
        }
        return cells[x][y];
    }
 
    public boolean isSolid(int type) {
        return type != 0;
    }
    
    public int[][] getCells() {
        return cells;
    }
    
    public void setSeed( long seed ) { 
    
        // Fill the whole maze to start out
        for( int i = 0; i < xSize; i++ ) {
            for( int j = 0; j < ySize; j++ ) {
                cells[i][j] = 1;
            }
        }
    
        random = new Random(seed);
        
        // Pick a random location near the middle to start
        int xCenter = xSize / 2;
        int yCenter = ySize / 2;
        int x = (int)(xCenter - (random.nextDouble() * xSize * 0.2 - xSize * 0.1));
        int y = (int)(yCenter - (random.nextDouble() * ySize * 0.2 - ySize * 0.1));
        xSeed = x;
        ySeed = y;
 
        cells[x][y] = 0;
               
        // Seed all around
        for( int d = 0; d < 4; d++ )
            {
            pending.add(new Move(d, 0, x + dirs[d][0], y + dirs[d][1]));
            }
    }
                
    public int generate() {
        return generate(-1);
    }
 
    public int generate( int iterations ) {
    
        int count = 0;
                   
        // While there are still moves left, generate more random
        // moves
        while( !pending.isEmpty() ) {
            if( iterations > 0 && count >= iterations )
                break;
            count++; 
            
            Move m = pending.removeFirst();
            
            // If this cell is already 0 then we're done with this branch
            int value = m.cell();
            if( !isSolid(value) || value < 0 ) {
                continue;
            }                
 
            double breakthroughChance = 1.0;
            
            // See if any of the directions other than the one we came
            // are empty, ie: we'd be breaking through to another cell.
            for( int d = 0; d < 4; d++ ) {
                if( d == reverse[m.dir] ) {
                    continue;
                }
                int v = get(d, m.x, m.y);
                if( !isSolid(v) ) {
                    if( d == m.dir ) {
                        breakthroughChance -= breakthroughPenaltyStraight;     
                    } else {
                        breakthroughChance -= breakthroughPenaltySides;     
                    }
                }
            }           
            
            // If the tunnel would break through there is
            // a random chance it won't.
            if( random.nextDouble() >= breakthroughChance ) {
                // Not allowed to break through... so we're done
                continue;
            }

 
            // Else let's make a tunnel.
            cells[m.x][m.y] = 0;
 
            double sideChance = baseSideChance;
 
            // Chance of going straight is based on how close we
            // are to the center and whether we are heading away
            // or towards center.
            double xDist = (m.x - xSize/2);
            double yDist = (m.y - ySize/2);
            double dist = xDist * xDist + yDist * yDist;
            xDist = ((m.x + dirs[m.dir][0]) - xSize/2);
            yDist = ((m.y + dirs[m.dir][1]) - ySize/2);
            double nextDist = xDist * xDist + yDist * yDist;
            
            double forwardChance = baseForwardChance;
            if( nextDist > dist ) {
                // we are moving away from the center...
                // how far away?
                if( dist < (xSize / 4) * (xSize / 4) ) {
                    forwardChance = awayForwardChance;
                }
            } 
                       
            // There is one chance we keep going           
            if( random.nextDouble() < forwardChance ) {
                // Keep going forward
                pending.add(m.next(m.dir));
                
                // And now less likely to go to either side
                sideChance -= sidePenalty; 
            } else {
                // We didn't go forward so reset the chance
                // of a side
                sideChance = teeBranchChance;
            }
            
            // A different chance that we'll go either left or right
            for( int d = 0; d < 4; d++ ) {
                if( d == m.dir || d == reverse[m.dir] ) {
                    continue;
                }
                if( random.nextDouble() < sideChance ) {
                    pending.add(m.next(d));
                }                
            }
        }
        
        return count;            
    }
 
    public int visit( MazeVisitor visitor ) {
 
        int result = 0;
 
        boolean[][] visited = new boolean[xSize][ySize];
                       
        // Start at the seed location
        LinkedList<Move> pending = new LinkedList<>();
        pending.add(new Move(-1, 0, xSeed, ySeed));
 
        while( !pending.isEmpty() ) {
            Move m = pending.removeFirst();
 
            // See if we've been here already
            if( visited[m.x][m.y] ) {
                continue;
            }
            
            // Calculate the branches first
            for( int d = 0; d < 4; d++ ) {
                Move next = m.next(d);
                int value = next.cell();
                if( isSolid(value) ) {
                    continue;
                }
                pending.add(next);                
            }
 
            visited[m.x][m.y] = true;
            result++;
 
            if( !visitor.visit(this, m.x, m.y, m.cell(), m.depth, !pending.isEmpty()) ) {
                return result;
            }                      
        }
        return result;
    }
    
    private class Move {
        int dir;
        int depth;
        int x;
        int y;
        
        public Move( int dir, int depth, int x, int y ) {
            this.dir = dir;
            this.depth = depth;
            this.x = x;
            this.y = y;
        }
 
        public int cell() {
            return get(x,y);
        }
 
        public Move next( int dir ) {
            return new Move(dir, depth+1, x + dirs[dir][0], y + dirs[dir][1]);
        }
        
        public int lookAhead() {
            return get(dir, x, y);
        }
        
        public String toString() {
            return "Move[dir:" + dir + " into:" + x + ", " + y + "]";
        }
    }    
}
