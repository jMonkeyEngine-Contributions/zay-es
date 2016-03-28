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
import java.util.Random;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *  Keeps track of positioned entities within the maze in an
 *  easily queriable way.
 *
 *  @author    Paul Speed
 */ 
public class MazeIndex {

    private Logger log = LoggerFactory.getLogger(MazeIndex.class);
    
    private Map<Vector3f, CellEntities> index = new HashMap<>();
    private Map<EntityId, Vector3f> lastPositions = new HashMap<>(); 
    
    public MazeIndex() {
    }
    
    public CellEntities getCellEntities( Vector3f pos, boolean create ) {
        CellEntities result = index.get(pos);
        if( result == null && create ) {
            result = new CellEntities();
            index.put(pos, result);
        }
        return result;
    }

    public CellEntities add( EntityId e, Vector3f pos, boolean isSolid ) {
        Vector3f old = lastPositions.get(e);
        if( old != null ) {
            throw new RuntimeException("Entity:" + e + " is already in index at location:" + old); 
        }
        CellEntities cell = getCellEntities(pos, true);
        cell.add(e, isSolid);
        lastPositions.put(e, pos);           
        return cell;        
    }
    
    public Vector3f getPosition( EntityId e ) {
        return lastPositions.get(e);
    }
    
    public void remove( EntityId e ) {
        Vector3f old = lastPositions.get(e);
        if( old == null ) {
            throw new RuntimeException("Entity:" + e + " is not in index.");
        }
        remove(e, old); 
    }
        
    protected void remove( EntityId e, Vector3f pos ) {
        CellEntities cell = getCellEntities(pos, false);
        if( cell == null ) {
            return;
        }
        lastPositions.remove(e);
        cell.remove(e);
        if( cell.isEmpty() ) {
            index.remove(pos);
        }
    }
 
    public void setSolid( EntityId e, Vector3f pos, boolean isSolid ) {
        Vector3f old = lastPositions.get(e);
        if( old == null || !old.equals(pos) ) {
            // The index doesn't know about this one yet so we'll at least
            // log a warning
            log.warn("Changing solidity of unmanaged or misaligned entity:" + e 
                        + " lastPos:" + old + "  pos:" + pos + "  solid:" + isSolid);
        }
        CellEntities cell = getCellEntities(pos, false);
        if( cell == null ) {
            log.warn("No cell for entity:" + e 
                        + " lastPos:" + old + "  pos:" + pos + "  solid:" + isSolid);
        }
        cell.setSolid(e, isSolid);        
    } 
    
    public CellEntities setPosition( EntityId e, Vector3f pos, boolean isSolid ) {
        Vector3f old = lastPositions.get(e);
        if( pos != null && pos.equals(old) ) {
            // Nothing to do
            return null;
        } 
        if( old != null ) {
            remove(e, old);
        }
        if( pos != null ) {
            return add(e, pos, isSolid);            
        } else {
            return null;
        }
    }    
}
