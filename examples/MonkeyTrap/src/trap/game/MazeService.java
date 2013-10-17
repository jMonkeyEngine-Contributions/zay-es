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
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.Random;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 *  @author    Paul Speed
 */ 
public class MazeService implements Service {

    private Logger log = LoggerFactory.getLogger(MazeService.class);
    
    private int xSize;
    private int ySize;
    private Maze maze;
    private Long seed;
 
    private EntityData ed;
    private EntitySet objects;
    private EntitySet solidObjects;
    private MazeIndex index; 
    
    public MazeService( int xSize, int ySize ) {
        this.xSize = xSize;
        this.ySize = ySize;
        this.maze = new Maze(xSize, ySize);
        this.index = new MazeIndex();
    }
    
    public Maze getMaze() {
        return maze;
    }

    public Vector3f getPlayerSpawnLocation() {
        // For now always drop them in the same place
        return new Vector3f(maze.getXSeed() * 2, 0, maze.getYSeed() * 2);
    }

    public Vector3f findRandomLocation() {
        int x = (int)(Math.random() * (xSize-1)) + 1;
        int y = (int)(Math.random() * (xSize-1)) + 1;
        
        // Is it occupied?
        if( !isOccupied(x,y) )
            return new Vector3f(x, 0, y);
 
        // Else we need to find one... basically, we will
        // walk out in all directions until we find one.
        for( int radius = 1; radius < xSize; radius++ ) {
            for( int xTest = x - radius; xTest <= x + radius; xTest++ ) {
                for( int yTest = y - radius; yTest <= y + radius; yTest++ ) {
                    if( !isOccupied(xTest, yTest) ) {
                        return new Vector3f(xTest, 0, yTest);
                    }
                } 
            } 
        }
        
        // And still we fail somehow return the seed
        return new Vector3f(maze.getXSeed(), 0, maze.getYSeed());                    
    }

    public boolean isSolid( int x, int y ) {
        int t = maze.get(x,y);
        return maze.isSolid(t);
    } 

    public boolean isSolid( Direction dir, int x, int y ) {
        x += dir.getXDelta();
        y += dir.getYDelta();
        return isSolid(x, y);
    } 

    public boolean isBlocked( Direction dir, int x, int y, boolean includeObjects ) {
        x += dir.getXDelta();
        y += dir.getYDelta();
        return isBlocked(x, y, includeObjects);
    }
    
    public boolean isBlocked( int x, int y, boolean includeObjects ) {
        int t = maze.get(x,y);
        if( maze.isSolid(t) ) {
            return true;
        } else if( !includeObjects ) {
            return false;
        }                
        CellEntities cell = index.getCellEntities(new Vector3f(x*2, 0, y*2), false);
        if( cell == null ) {
            return false;
        } 
        return cell.hasSolids();        
    } 

    public boolean isOccupied( Vector3f loc ) {
        return isOccupied((int)(loc.x * 0.5), (int)(loc.z * 0.5));
    }

    public boolean isOccupied( int x, int y ) {    
        int t = maze.get(x,y);
        if( maze.isSolid(t) ) {
            return true;
        }
        CellEntities cell = index.getCellEntities(new Vector3f(x*2, 0, y*2), false);
        if( cell == null ) {
            return false;
        } 
        return cell.hasSolids();        
    } 

    public boolean isOccupied( Direction dir, int x, int y ) {
        x += dir.getXDelta();
        y += dir.getYDelta();
        return isOccupied(x, y);
    } 

    public CellEntities getEntities( Direction dir, int x, int y ) {
        x += dir.getXDelta();
        y += dir.getYDelta();
        return getEntities(x, y);
    }
     
    public CellEntities getEntities( int x, int y ) {
        return index.getCellEntities(new Vector3f(x*2, 0, y*2), false);
    }

    public void initialize( GameSystems systems ) {
        long s;
        if( seed != null ) {
            s = seed;
        } else {
            s = System.currentTimeMillis();
        }
        log.info("Using maze seed:" + s);
        maze.setSeed(s);
        maze.generate();
 
        populateMaze(s);
        
        // We'll keep track of the positioned entities in the maze
        // so that we can answer intersection queries and so on
        ed = systems.getEntityData();
        
        // We only watch for entity's with hit points because otherwise
        // they are traversable.  Well, we'll grab them all and sort
        // out the "solidness" later.
        objects = ed.getEntities(Position.class, ModelType.class);
        addObjects(objects);
        
        solidObjects = ed.getEntities(Position.class, ModelType.class, HitPoints.class);
        addSolidObjects(solidObjects);        
    }
 
    protected void populateMaze(long seed) {
        Random random = new Random(seed);
        ObjectDropper dropper = new ObjectDropper(random);
        int count = maze.visit(dropper);
        log.info("Visited " + count + " cells when dropping items.");      
    }
 
    protected EntityId createCollision( Entity mover, Entity collider, 
                                        long time, long delay, EntityComponent... adds ) {
        EntityId collision = EntityFactories.createCollision( mover.getId(), mover.get(ModelType.class),
                                                              collider.getId(), collider.get(ModelType.class),
                                                              time, delay, adds );
        return collision;
    }
 
    protected void setSolid( Entity e, Position pos, boolean isSolid ) {
        Vector3f loc = pos != null ? pos.getLocation(): null;
        index.setSolid(e.getId(), loc, isSolid);
    } 
    
    protected void setPosition( Entity e, Position pos ) {
        boolean isSolid = ed.getComponent(e.getId(), HitPoints.class) != null;
        Vector3f loc = pos != null ? pos.getLocation(): null;
        CellEntities cell = index.setPosition(e.getId(), loc, isSolid);
        if( cell != null ) {
            // It was actually moved so check for collisions maybe
            // If we were not the first in the cell and we were moving...
            if( cell.size() > 1 && pos.getChangeTime() != pos.getTime() ) {
                // There was a collision
                for( EntityId c : cell ) {
                    Entity collider = objects.getEntity(c);
                    if( collider == null ) {
                        throw new RuntimeException("Found cell entity that we aren't managing:" + c);
                    }
                    
                    // If the entity is us then we skip
                    if( collider == e ) {
                        continue;
                    }
                    
                    // A safety check for other systems
                    if( e.get(ModelType.class) == collider.get(ModelType.class) ) {
                        log.error("Ogre collided with itself which means other checks are failing:" 
                                    + e + " and " + collider);
                    }
                
                    // Generate the collision
                    // Give it a 2 second decay so that if nothing
                    // handles the collision it will eventually get removed.
                    EntityId collision = createCollision(e, collider, pos.getTime(), 2000 * 1000000L, pos);    
                    if( log.isTraceEnabled() ) {
                        log.trace("Collision:" + collision + "  entity:" + e + "  hit:" + collider);
                    }
                }                
            }            
        } 
    }

    protected void addObjects( Set<Entity> set ) {
        for( Entity e : set ) {
            setPosition(e, e.get(Position.class));
        }
    }
    
    protected void updateObjects( Set<Entity> set ) {
        for( Entity e : set ) {
            setPosition(e, e.get(Position.class));
        }
    }
    
    protected void removeObjects( Set<Entity> set ) {
        for( Entity e : set ) {
            setPosition(e, null);
        }
    }
    
    protected void addSolidObjects( Set<Entity> set ) {
        for( Entity e : set ) {
            setSolid(e, e.get(Position.class), true);
        }
    }
    
    protected void removeSolidObjects( Set<Entity> set ) {
        for( Entity e : set ) {
            setSolid(e, e.get(Position.class), false);
        }
    }  

    protected void refreshIndex() {
        if( objects.applyChanges() ) {
            removeObjects(objects.getRemovedEntities());
            addObjects(objects.getAddedEntities());
            updateObjects(objects.getChangedEntities());
        }
        if( solidObjects.applyChanges() ) {
            removeSolidObjects(solidObjects.getRemovedEntities());
            addSolidObjects(solidObjects.getAddedEntities());
        }
    }

    public void update( long gameTime ) {
        refreshIndex();
    }

    public void terminate( GameSystems systems ) {
        objects.release();
    }
    
    private class ObjectDropper implements MazeVisitor {
 
        Random random;   
        double chanceToDrop = 0;
        
        public ObjectDropper(Random random) {
            this.random = random;
        }
        
        public boolean visit( Maze maze, int x, int y, int value, int depth, boolean pathsPending ) {
            
            double drop = random.nextDouble() * 100;
            if( drop < chanceToDrop ) {
            
                // Create the drop
                log.debug("Item drop at:" + x + ", " + y);
 
                double type = random.nextDouble();
                if( type < 0.2 ) {
                    // 20% chance of chest
                    EntityFactories.createObject( MonkeyTrapConstants.TYPE_CHEST,
                                                  -1, new Vector3f(x*2, 0, y*2));
                } else {
                    // else barrels           
                    EntityFactories.createObject( MonkeyTrapConstants.TYPE_BARRELS,
                                                  -1, new Vector3f(x*2, 0, y*2));
                }                                         
            
                chanceToDrop = 0;
            } else {
                chanceToDrop += random.nextDouble();
            }
        
            return true;
        }
    }
}
