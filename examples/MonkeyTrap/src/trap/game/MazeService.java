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
    private Map<Vector3f, List<EntityId>> index = new HashMap<Vector3f, List<EntityId>>();
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
        refreshIndex();
        return !getEntities(x, y).isEmpty();        
    } 

    public boolean isOccupied( int x, int y ) {    
        int t = maze.get(x,y);
        if( maze.isSolid(t) ) {
            return true;
        }
        refreshIndex();
        return !getEntities(x, y).isEmpty();
    } 

    public boolean isOccupied( Direction dir, int x, int y ) {
        x += dir.getXDelta();
        y += dir.getYDelta();
        return isOccupied(x, y);
        /*int t = maze.get(x,y);
        if( maze.isSolid(t) ) {
            return true;
        }
        List<EntityId> list = getEntities(x, y);
        if( !list.isEmpty() ) {
            return true;              
        }
        return false;*/
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
        objects = ed.getEntities(Position.class, ModelType.class);
        addObjects(objects);        
    }
 
    public List<EntityId> getEntities( int x, int y ) {
        List<EntityId> list = getEntities(new Vector3f(x*2, 0, y*2), false);
        if( list == null ) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(list);
    }
 
    protected List<EntityId> getEntities( Vector3f pos, boolean create ) {
        List<EntityId> result = index.get(pos);
        if( result == null && create ) {
            result = new ArrayList<EntityId>();
            index.put(pos, result);
        }
        return result;
    }
    
    protected void add( EntityId id, Vector3f pos ) {
        getEntities(pos, true).add(id);
    }
    
    protected void remove( EntityId id, Vector3f pos ) {
        List<EntityId> list = getEntities(pos, false);
        if( list == null ) {
            return;
        }
        list.remove(id);
        if( list.isEmpty() ) {
            index.remove(pos);
        }
    }
    
    protected void setPosition( EntityId id, Vector3f pos ) {
        Vector3f old = lastPositions.remove(id);
        if( old != null ) {
            remove(id, old);
        }
        if( pos != null ) {
            add(id, pos);
            lastPositions.put(id, pos);           
        }
    }

    protected void addObjects( Set<Entity> set ) {
        for( Entity e : set ) {
            setPosition(e.getId(), e.get(Position.class).getLocation());
        }
    }
    
    protected void updateObjects( Set<Entity> set ) {
        for( Entity e : set ) {
            setPosition(e.getId(), e.get(Position.class).getLocation());
        }
    }
    
    protected void removeObjects( Set<Entity> set ) {
        for( Entity e : set ) {
            setPosition(e.getId(), null);
        }
    }  

    protected void refreshIndex() {
        if( objects.applyChanges() ) {
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
}
