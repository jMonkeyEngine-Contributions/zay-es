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

import com.simsilica.es.EntityId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 *  Part of the MazeIndex, holds the entities that are currently
 *  residing in a particular cell.
 *
 *  @author    Paul Speed
 */ 
public class CellEntities implements Iterable<EntityId> {
    private List<EntityId> entities;
    private List<EntityId> solid;
    
    public CellEntities() {
    }
 
    public int size() {
        return entities == null ? 0 : entities.size();
    }
    
    public boolean add( EntityId e, boolean isSolid ) {
        if( entities == null ) {
            entities = new ArrayList<EntityId>();
        }
        if( entities.add(e) ) {
            if( isSolid ) {                
                if( solid == null ) {
                    solid = new ArrayList<EntityId>();
                }
                solid.add(e);
            }
            return true;
        }
        return false;        
    }
    
    public void remove( EntityId e ) {
        if( entities != null ) {
            entities.remove(e);
        }
        if( solid != null ) {
            solid.remove(e);
        }
    }
    
    public boolean hasSolids() {
        return solid != null && !solid.isEmpty(); 
    }
    
    public boolean isEmpty() {
        return entities == null || entities.isEmpty();
    }
 
    public List<EntityId> getEntities() {
        List<EntityId> result = entities;
        if( result == null ) {
            result = Collections.emptyList();  // generics are dumb again
        } 
        return Collections.unmodifiableList(result); 
    }
    
    public Iterator<EntityId> iterator() {
        if( entities != null ) {
            return entities.iterator();
        }
        // Two lines because sometimes generics are dumb
        List<EntityId> empty = Collections.emptyList(); 
        return empty.iterator();   
    }
    
    @Override
    public String toString() {
        return "CellEntities[" + entities + ", solid=" + solid + "]";
    }
}


