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

import trap.game.Maze;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;


/**
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class MeshGenerator
{
    private MeshGenerator() {
    }

    private static float[][] wallVerts = {
    
                // North (facing south)
                {
                    0, 0, 0,
                    1, 0, 0,
                    1, 1, 0,
                    0, 1, 0
                },
                // South (facing north)
                {
                    1, 0, 1,
                    0, 0, 1,
                    0, 1, 1,
                    1, 1, 1
                },
                // East (facing west)
                {
                    1, 0, 0,
                    1, 0, 1,
                    1, 1, 1,
                    1, 1, 0
                },
                // West (facing east)
                {
                    0, 0, 1,
                    0, 0, 0,
                    0, 1, 0,
                    0, 1, 1
                }                      
            };
    
    private static float[][] wallNorms = {
                // North (facing south)
                {
                    0, 0, 1
                },
                // South (facing north)
                {
                    0, 0, -1
                },
                // East (facing west)
                {
                    -1, 0, 0
                },
                // West (facing east)
                {
                    1, 0, 0
                }                      
            };

    private static float[] floorVerts = {
                0, 0, 1,
                1, 0, 1,
                1, 0, 0,
                0, 0, 0
            };

    private static float[] upNormal = {
                0, 1, 0
            };
    private static float[] downNormal = {
                0, -1, 0
            };

    public static Mesh generateMesh( Maze maze, float xScale, float yScale ) {
            
        int xSize = maze.getWidth();
        int ySize = maze.getHeight();    
 
        List<Wall> walls = new ArrayList<Wall>();
        List<Floor> floors = new ArrayList<Floor>();     
 
        for( int x = 1; x < xSize - 1; x++ ) {
            for( int y = 1; y < ySize - 1; y++ ) {
 
                // We only render empty spaces               
                int v = maze.get(x,y);
                if( maze.isSolid(v) ) {
                    continue;
                }
                                               
                floors.add(new Floor(x,y));
                                               
                // Simple for now... just find solid sides
                for( int d = 0; d < 4; d++ ) {
                    int neighbor = maze.get(d,x,y);
                    if( !maze.isSolid(neighbor) ) {
                        continue;
                    }
                    // Generate a size for this direction
                    walls.add(new Wall(x, y, d));
                }
            }
        }
 
        // Now build the buffers and mesh
        int vertCount = 2 * walls.size() * 4 + floors.size() * 4;
        int quadCount = 2 * walls.size() + floors.size();
        
        FloatBuffer pos = BufferUtils.createVector3Buffer(vertCount);
        FloatBuffer normals = BufferUtils.createVector3Buffer(vertCount);
        FloatBuffer texes = BufferUtils.createVector2Buffer(vertCount);
        ShortBuffer indexes = BufferUtils.createShortBuffer(quadCount * 2 * 3);
        
        short[] indexArray = { 0, 1, 2, 0, 2, 3 };
        short[] revIndexArray = { 0, 2, 1, 0, 3, 2 };
        short baseIndex = 0;
 
        // Add all of the floors first
        for( Floor f : floors ) {
            int vIndex = 0;
            for( int i = 0; i < 4; i++ ) {
                
                float x = f.x * xScale + floorVerts[vIndex++] * xScale;
                float y = 0 + floorVerts[vIndex++] * yScale;
                float z = f.y * yScale + floorVerts[vIndex++] * xScale;
 
                pos.put(x);
                pos.put(y);
                pos.put(z);
                
                normals.put(upNormal);
            }
            
            texes.put((1024 - 128 - 8)/1024f).put(8/1024f);                
            texes.put((1024 - 8)/1024f).put(8/1024f);                
            texes.put((1024 - 8)/1024f).put((128+8)/1024f);                
            texes.put((1024 - 128 - 8)/1024f).put((128+8)/1024f);
 
            for( int i = 0; i < 6; i++ ) {              
                indexes.put((short)(baseIndex + indexArray[i]));
            }                        
            baseIndex += 4; 
        }
        
        for( Wall w : walls ) {
            float[] vs = wallVerts[w.dir];
            float[] ns = wallNorms[w.dir];
            int vIndex = 0;
            for( int i = 0; i < 4; i++ ) {
                
                float x = w.x * xScale + vs[vIndex++] * xScale;
                float y = 0 + vs[vIndex++] * yScale;
                float z = w.y * yScale + vs[vIndex++] * xScale;
 
                pos.put(x);
                pos.put(y);
                pos.put(z);
                
                normals.put(ns);
            }
            
            texes.put(8/1024f).put(520/1024f);                
            texes.put(519/1024f).put(520/1024f);                
            texes.put(519/1024f).put(1016/1024f);                
            texes.put(8/1024f).put(1016/1024f);
 
            for( int i = 0; i < 6; i++ ) {              
                indexes.put((short)(baseIndex + indexArray[i]));
            }                        
            baseIndex += 4;
        }

        // Add backsides to the wall...  basically 
        // a shorter version of the wall with the indexes inverted. 
        for( Wall w : walls ) {
            float[] vs = wallVerts[w.dir];
            float[] ns = wallNorms[w.dir];
            int vIndex = 0;
            for( int i = 0; i < 4; i++ ) {
                
                float x = w.x * xScale + vs[vIndex++] * xScale;
                float y = 0 + vs[vIndex++] * yScale;
                float z = w.y * yScale + vs[vIndex++] * xScale;
 
                pos.put(x);
                pos.put(y);
                pos.put(z);
 
                // Point the normal down so they are dark               
                normals.put(downNormal);
            }
            
            texes.put(8/1024f).put(8/1024f);                
            texes.put((8 + 127)/1024f).put(8/1024f);                
            texes.put((8 + 127)/1024f).put((128+8)/1024f);                
            texes.put(8/1024f).put((128+8)/1024f);
 
            for( int i = 0; i < 6; i++ ) {              
                indexes.put((short)(baseIndex + revIndexArray[i]));
            }                        
            baseIndex += 4;
        }
 
        Mesh result = new Mesh();
        result.setBuffer(Type.Position, 3, pos);
        result.setBuffer(Type.Normal, 3, normals);
        result.setBuffer(Type.TexCoord, 2, texes); 
        result.setBuffer(Type.Index, 3, indexes);
        
        result.updateBound();
 
        return result;
    }

    public static Mesh generateAmbientOcclusion( Maze maze, float xScale, float yScale ) {
            
        int xSize = maze.getWidth();
        int ySize = maze.getHeight();    
 
        List<Wall> walls = new ArrayList<Wall>();
        List<Floor> floors = new ArrayList<Floor>();     
 
        for( int x = 1; x < xSize - 1; x++ ) {
            for( int y = 1; y < ySize - 1; y++ ) {
 
                // We only render empty spaces               
                int v = maze.get(x,y);
                if( maze.isSolid(v) ) {
                    continue;
                }
                                               
                floors.add(new Floor(x,y));
                                               
                // Simple for now... just find solid sides
                for( int d = 0; d < 4; d++ ) {
                    int neighbor = maze.get(d,x,y);
                    if( !maze.isSolid(neighbor) ) {
                        continue;
                    }
                    // Generate a size for this direction
                    walls.add(new Wall(x, y, d));
                }
            }
        }
 
        // Now build the buffers and mesh
        int vertCount = walls.size() * 4 + floors.size() * 4;
        int quadCount = walls.size() + floors.size();
        
        FloatBuffer pos = BufferUtils.createVector3Buffer(vertCount);
        FloatBuffer normals = BufferUtils.createVector3Buffer(vertCount);
        FloatBuffer texes = BufferUtils.createVector2Buffer(vertCount);
        ShortBuffer indexes = BufferUtils.createShortBuffer(quadCount * 2 * 3);
        
        short[] indexArray = { 0, 1, 2, 0, 2, 3 };
        short[] revIndexArray = { 0, 2, 1, 0, 3, 2 };
        short baseIndex = 0;
 
        // Add all of the floors first
        for( Floor f : floors ) {
            int vIndex = 0;
            for( int i = 0; i < 4; i++ ) {
                
                float x = f.x * xScale + floorVerts[vIndex++] * xScale;
                float y = 0 + floorVerts[vIndex++] * yScale;
                float z = f.y * yScale + floorVerts[vIndex++] * xScale;
 
                pos.put(x);
                pos.put(y);
                pos.put(z);
                
                normals.put(upNormal);
            }

            // See what's around us
            boolean north = maze.isSolid(maze.get(0, f.x, f.y));
            boolean south = maze.isSolid(maze.get(1, f.x, f.y));
            boolean east = maze.isSolid(maze.get(2, f.x, f.y));
            boolean west = maze.isSolid(maze.get(3, f.x, f.y));
            
            texes.put(west ? 0 : 0.5f).put(south ? 0 : 0.5f);                            
            texes.put(east ? 1 : 0.5f).put(south ? 0 : 0.5f);                            
            texes.put(east ? 1 : 0.5f).put(north ? 1 : 0.5f);                           
            texes.put(west ? 0 : 0.5f).put(north ? 1 : 0.5f);
 
            for( int i = 0; i < 6; i++ ) {              
                indexes.put((short)(baseIndex + indexArray[i]));
            }                        
            baseIndex += 4; 
        }
        
        for( Wall w : walls ) {
            float[] vs = wallVerts[w.dir];
            float[] ns = wallNorms[w.dir];
            int vIndex = 0;
            for( int i = 0; i < 4; i++ ) {
                
                float x = w.x * xScale + vs[vIndex++] * xScale;
                float y = 0 + vs[vIndex++] * yScale;
                float z = w.y * yScale + vs[vIndex++] * xScale;
 
                pos.put(x);
                pos.put(y);
                pos.put(z);
                
                normals.put(ns);
            }
            
            boolean north = maze.isSolid(maze.get(0, w.x, w.y));
            boolean south = maze.isSolid(maze.get(1, w.x, w.y));
            boolean east = maze.isSolid(maze.get(2, w.x, w.y));
            boolean west = maze.isSolid(maze.get(3, w.x, w.y));
            
            boolean up = false;
            boolean down = true;
            boolean left = false;
            boolean right = false;
            
            switch(w.dir) {
                case 0:
                    left = west;
                    right = east;
                    break; 
                case 1:
                    left = east;
                    right = west;
                    break;
                case 2:
                    left = north;
                    right = south;
                    break;
                case 3:
                    left = south;
                    right = north;
                    break;
            }
            
            texes.put(left ? 0 : 0.5f).put(down ? 0 : 0.5f);                            
            texes.put(right ? 1 : 0.5f).put(down ? 0 : 0.5f);                            
            texes.put(right ? 1 : 0.5f).put(up ? 1 : 0.5f);                           
            texes.put(left ? 0 : 0.5f).put(up ? 1 : 0.5f);
 
            for( int i = 0; i < 6; i++ ) {              
                indexes.put((short)(baseIndex + indexArray[i]));
            }                        
            baseIndex += 4;
        }

        Mesh result = new Mesh();
        result.setBuffer(Type.Position, 3, pos);
        result.setBuffer(Type.Normal, 3, normals);
        result.setBuffer(Type.TexCoord, 2, texes); 
        result.setBuffer(Type.Index, 3, indexes);
        
        result.updateBound();
 
        return result;
    }

    public static Mesh generateOverlay( Maze maze, float xScale, float yScale ) {
            
        int xSize = maze.getWidth();
        int ySize = maze.getHeight();    
 
        List<Wall> walls = new ArrayList<Wall>();
        List<Floor> floors = new ArrayList<Floor>();     
 
        for( int x = 1; x < xSize - 1; x++ ) {
            for( int y = 1; y < ySize - 1; y++ ) {
 
                // We only render empty spaces               
                int v = maze.get(x,y);
                if( maze.isSolid(v) ) {
                    continue;
                }
                                               
                floors.add(new Floor(x,y));
                                               
                // Simple for now... just find solid sides
                for( int d = 0; d < 4; d++ ) {
                    int neighbor = maze.get(d,x,y);
                    if( !maze.isSolid(neighbor) ) {
                        continue;
                    }
                    // Generate a size for this direction
                    walls.add(new Wall(x, y, d));
                }
            }
        }
 
        // Now build the buffers and mesh
        int vertCount = walls.size() * 4 + floors.size() * 4;
        int quadCount = walls.size() + floors.size();
        
        FloatBuffer pos = BufferUtils.createVector3Buffer(vertCount);
        FloatBuffer normals = BufferUtils.createVector3Buffer(vertCount);
        FloatBuffer texes = BufferUtils.createVector2Buffer(vertCount);
        ShortBuffer indexes = BufferUtils.createShortBuffer(quadCount * 2 * 3);
        
        short[] indexArray = { 0, 1, 2, 0, 2, 3 };
        short[] revIndexArray = { 0, 2, 1, 0, 3, 2 };
        short baseIndex = 0;
 
        // Add all of the floors first
        for( Floor f : floors ) {
            int vIndex = 0;
            for( int i = 0; i < 4; i++ ) {
                
                float x = f.x * xScale + floorVerts[vIndex++] * xScale;
                float y = 0 + floorVerts[vIndex++] * yScale;
                float z = f.y * yScale + floorVerts[vIndex++] * xScale;
 
                pos.put(x);
                pos.put(y);
                pos.put(z);
                
                normals.put(upNormal);
            }

            // See what's around us
            boolean north = maze.isSolid(maze.get(0, f.x, f.y));
            boolean south = maze.isSolid(maze.get(1, f.x, f.y));
            boolean east = maze.isSolid(maze.get(2, f.x, f.y));
            boolean west = maze.isSolid(maze.get(3, f.x, f.y));
            
            // We do this for proper blending when we want it
            float s = (f.x+0.5f) / (float)xSize;
            float t = (f.y+0.5f) / (float)ySize;
            float sStep = 0.5f / (float)xSize; 
            float tStep = 0.5f / (float)ySize; 
 
            texes.put(west ? s : s - sStep).put(south ? t : t + tStep);
            texes.put(east ? s : s + sStep).put(south ? t : t + tStep);
            texes.put(east ? s : s + sStep).put(north ? t : t - tStep);                           
            texes.put(west ? s : s - sStep).put(north ? t : t - tStep);
 
            for( int i = 0; i < 6; i++ ) {              
                indexes.put((short)(baseIndex + indexArray[i]));
            }                        
            baseIndex += 4; 
        }
        
        for( Wall w : walls ) {
            float[] vs = wallVerts[w.dir];
            float[] ns = wallNorms[w.dir];
            int vIndex = 0;
            for( int i = 0; i < 4; i++ ) {
                
                float x = w.x * xScale + vs[vIndex++] * xScale;
                float y = 0 + vs[vIndex++] * yScale;
                float z = w.y * yScale + vs[vIndex++] * xScale;
 
                pos.put(x);
                pos.put(y);
                pos.put(z);
                
                normals.put(ns);
            }
            
            boolean north = maze.isSolid(maze.get(0, w.x, w.y));
            boolean south = maze.isSolid(maze.get(1, w.x, w.y));
            boolean east = maze.isSolid(maze.get(2, w.x, w.y));
            boolean west = maze.isSolid(maze.get(3, w.x, w.y));
            
            float s = (w.x+0.5f) / (float)xSize;
            float t = (w.y+0.5f) / (float)ySize;
            float sStep = 0.5f / (float)xSize; 
            float tStep = 0.5f / (float)ySize; 
 
            switch( w.dir ) {
                case 0:
                    texes.put(west ? s : s - sStep).put(t);
                    texes.put(east ? s : s + sStep).put(t);
                    texes.put(east ? s : s + sStep).put(t);                           
                    texes.put(west ? s : s - sStep).put(t);
                    break;
                case 1:
                    texes.put(east ? s : s + sStep).put(t);
                    texes.put(west ? s : s - sStep).put(t);
                    texes.put(west ? s : s - sStep).put(t);                           
                    texes.put(east ? s : s + sStep).put(t);
                    break;
                case 2:
                    texes.put(s).put(north ? t : t - tStep);
                    texes.put(s).put(south ? t : t + tStep);
                    texes.put(s).put(south ? t : t + tStep);                           
                    texes.put(s).put(north ? t : t - tStep);
                    break;
                case 3:
                    texes.put(s).put(south ? t : t + tStep);
                    texes.put(s).put(north ? t : t - tStep);
                    texes.put(s).put(north ? t : t - tStep);                           
                    texes.put(s).put(south ? t : t + tStep);
                    break;
            }
            for( int i = 0; i < 6; i++ ) {              
                indexes.put((short)(baseIndex + indexArray[i]));
            }                        
            baseIndex += 4;
        }

        Mesh result = new Mesh();
        result.setBuffer(Type.Position, 3, pos);
        result.setBuffer(Type.Normal, 3, normals);
        result.setBuffer(Type.TexCoord, 2, texes); 
        result.setBuffer(Type.Index, 3, indexes);
        
        result.updateBound();
 
        return result;
    }
    
    private static class Wall {
        int x;
        int y;
        int dir;
        
        public Wall( int x, int y, int dir ) {
            this.x = x;
            this.y = y;
            this.dir = dir;
        }
    }
    
    private static class Floor {
        int x;
        int y;
        
        public Floor( int x, int y ) {
            this.x = x;
            this.y = y;
        }
    }
}
