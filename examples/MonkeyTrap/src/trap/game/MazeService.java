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
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 *  @author    Paul Speed
 */ 
public class MazeService implements Service {

    private Logger log = LoggerFactory.getLogger(GameSystems.class);
    
    private int xSize;
    private int ySize;
    private Maze maze;
    private Long seed;
 
    private EntityData ed;
    private EntitySet objects;
    private Map<Vector3f, CellEntities> solidIndex = new HashMap<Vector3f, CellEntities>();
    private Map<EntityId, Vector3f> lastPositions = new HashMap<EntityId, Vector3f>(); 
    
    public MazeService( int xSize, int ySize ) {
        this.xSize = xSize;
        this.ySize = ySize;
        this.maze = new Maze(xSize, ySize);
    }
    
    public Maze getMaze() {
        return maze;
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
        CellEntities cell = getCellEntities(new Vector3f(x*2, 0, y*2), false);
        if( cell == null ) {
            return false;
        } 
        return cell.isOccupied();        
    } 

    public boolean isOccupied( Vector3f loc ) {
        return isOccupied((int)(loc.x * 0.5), (int)(loc.z * 0.5));
    }

    public boolean isOccupied( int x, int y ) {    
        int t = maze.get(x,y);
        if( maze.isSolid(t) ) {
            return true;
        }
        CellEntities cell = getCellEntities(new Vector3f(x*2, 0, y*2), false);
        if( cell == null ) {
            return false;
        } 
        return cell.isOccupied();        
    } 

    public boolean isOccupied( Direction dir, int x, int y ) {
        x += dir.getXDelta();
        y += dir.getYDelta();
        return isOccupied(x, y);
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
        
        // We'll keep track of the positioned entities in the maze
        // so that we can answer intersection queries and so on
        ed = systems.getEntityData();
        
        // We only watch for entity's with hit points because otherwise
        // they are traversable.  Well, we'll grab them all and sort
        // out the "solidness" later.
        objects = ed.getEntities(Position.class, ModelType.class);
        addObjects(objects);        
    }
 
    public List<Entity> getEntities( int x, int y ) {
        CellEntities cell = getCellEntities(new Vector3f(x*2, 0, y*2), false);
        if( cell == null || cell.entities == null ) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(cell.entities);
    }
 
    protected CellEntities getCellEntities( Vector3f pos, boolean create ) {
        CellEntities result = solidIndex.get(pos);
        if( result == null && create ) {
            result = new CellEntities();
            solidIndex.put(pos, result);
        }
        return result;
    }
    
    protected void add( Entity e, Position pos ) {
        CellEntities cell = getCellEntities(pos.getLocation(), true);
        if( !cell.isEmpty() && pos.getChangeTime() != pos.getTime() ) {
            // Calculate the collisions before we add ourselves to
            // the list.
            for( Entity collider : cell.entities ) {
                if( e.get(ModelType.class) == collider.get(ModelType.class) ) {
                    log.error("Ogre collided with itself which means other checks are failing:" 
                                + e + " and " + collider);
                }
                
                // Generate the collision
                // Give it a 2 second decay so that if anything doesn't
                // handle the collision it will eventually get removed.
                EntityId collision = ed.createEntity();
System.out.println( "Collision:" + collision + "  entity:" + e + "  hit:" + collider );                                
                ed.setComponents(collision, 
                                 new Collision(e.getId(), e.get(ModelType.class), 
                                               collider.getId(), collider.get(ModelType.class)),
                                 pos,
                                 new Decay(pos.getTime() + 2000 * 1000000L));
                                 
 
                if( e.get(ModelType.class) == MonkeyTrapConstants.TYPE_MONKEY 
                    && collider.get(ModelType.class) != MonkeyTrapConstants.TYPE_BLING ) {                                
                    // For testing....
                    EntityId test = ed.createEntity();
                    ed.setComponents(test, pos.newTime(pos.getTime(), pos.getTime()), 
                                     MonkeyTrapConstants.TYPE_BLING, 
                                     new Decay(pos.getTime() + 2000 * 1000000L));
                    EntityId buff = ed.createEntity();
                    ed.setComponents(buff, new Buff(e.getId(), pos.getTime()), new HealthChange(2));
                }                                                                                                                                                         
            }
        }            
        cell.add(e);
    }
    
    protected void remove( Entity e, Vector3f pos ) {
        CellEntities cell = getCellEntities(pos, false);
        if( cell == null ) {
            return;
        }
        cell.remove(e);
        if( cell.isEmpty() ) {
            solidIndex.remove(pos);
        }
    }
    
    protected void setPosition( Entity e, Position pos ) {
        Vector3f old = lastPositions.get(e.getId());
        if( pos != null && pos.getLocation().equals(old) ) {
//System.out.println( "Locations are same, probably just turned." + old + "  new:" + pos.getLocation() );        
            return; // we just turned
        } else {
            lastPositions.remove(e.getId());
        }
        
        if( old != null ) {
            remove(e, old);
        }
        if( pos != null ) {
            add(e, pos);
            lastPositions.put(e.getId(), pos.getLocation());           
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

    protected void refreshIndex() {
        if( objects.applyChanges() ) {
//System.out.println( "Upating index..." );        
            removeObjects(objects.getRemovedEntities());
            addObjects(objects.getAddedEntities());
            updateObjects(objects.getChangedEntities());
        }
    }

    public void update( long gameTime ) {
        refreshIndex();
    }

    public void terminate( GameSystems systems ) {
        objects.release();
    }
    
    protected class CellEntities {
        List<Entity> entities;
        List<Entity> solid;
        
        public CellEntities() {
        }
        
        public void add( Entity e ) {
            if( entities == null ) {
                entities = new ArrayList<Entity>();
            }
            if( entities.add(e) ) {
                if( ed.getComponent(e.getId(), HitPoints.class) != null ) {                
                    if( solid == null ) {
                        solid = new ArrayList<Entity>();
                    }
                    solid.add(e);
                }
            }
        }
        
        public void remove( Entity e ) {
            entities.remove(e);
            solid.remove(e);
        }
 
        public boolean isOccupied() {
            return !solid.isEmpty();
        }
        
        public boolean isEmpty() {
            return entities == null || entities.isEmpty();
        }
    }
}
