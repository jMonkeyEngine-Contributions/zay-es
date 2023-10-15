/*
 * $Id$
 * 
 * Copyright (c) 2015, Simsilica, LLC
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.simsilica.es.server;

import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *  Keeps track of a 'frame' of 'version' for any active
 *  entity ID + component type combination.  Basically this is
 *  implementing a mark-and-sweep style usage tracking that the
 *  HostedEntityData will use to keep track of what's relevant
 *  for the client when sending updates.
 *
 *  @author    Paul Speed
 */
public class ComponentUsageTracker {
    
    private final Map<Class<? extends EntityComponent>, Map<EntityId, Long>> map = new HashMap<>();
    private final List<PendingClean> pending = new ArrayList<>();
    
    public ComponentUsageTracker() {
    }
 
    protected Map<EntityId, Long> getFrameMap( Class<? extends EntityComponent> type, boolean create ) {
        Map<EntityId, Long> result = map.get(type);
        if( result == null && create ) {
            result = new HashMap<>();
            map.put(type, result);
        }
        return result; 
    }
 
    protected void checkPending() {
        if( !pending.isEmpty() ) {
            throw new IllegalStateException("Cannot update frame tracking while expirations are pending.  Call sweep() first.");
        }
    }
 
    /**
     *  Sets the current frame counter for the specified EntityId and component
     *  type combination. 
     */
    public Long set( EntityId id, Class<? extends EntityComponent> type, Long frame ) {
        checkPending();
        return getFrameMap(type, true).put(id, frame);
    }

    /**
     *  Sets the current frame counter for all of the specified EntityIds and component
     *  type combination. 
     */
    public void set( Collection<EntityId> ids, Class<? extends EntityComponent> type, Long frame ) {
        checkPending();
        Map<EntityId, Long> frames = getFrameMap(type, true);
        for( EntityId id : ids ) {
            frames.put(id, frame);
        }
    }
 
    /**
     *  Returns the last stored version for the ID and type, removing it if the last
     *  stored version is older than the current version.  If there is no stored version
     *  then null is returned. 
     */   
    public Long getAndExpire( EntityId id, Class<? extends EntityComponent> type, Long current ) {
        Map<EntityId, Long> frames = getFrameMap(type, false);
        if( frames == null ) {
            return null;
        }
        Long last = frames.get(id);
        if( last == null ) {
            return null;
        }
        if( last == current.longValue() ) {
            // We're fine... everything is up to date
            return current;
        }
        
        // Else we need to remove the entry
        //frames.remove(id);
        //
        //// We will clear the type entry if it is empty only because 
        //// it lets the potentially-expanded internal storage of the
        //// hashmap get GC'ed.  Generally, though, I'd expect these maps
        //// to be long-living and relatively stable in size.
        //if( frames.isEmpty() ) {
        //    map.remove(type);
        //}
        // 2023-10-15 - when a component changes multiple times since the
        // last frame, we would only send the first EntityChange because this
        // method would remove all tracking upon encountering that first EntityChange.
        // This is especially problematic if the component was changed then removed
        // or removed then changed... entity sets on the client would then be inconsistent
        // with the real entity sets.  I know for sure this is happening if the component
        // was changed in the same actual game systems update.  I assume it would happen
        // if the same component changed between HostedEntityData sweeps but I dit not
        // prove it.
        // Note: it is true that we add duplicate PendingCleans in this case but
        // I think the simpler data structure is still better for something this
        // is (clearly) pretty rare (since we're only just now finding it).
        pending.add(new PendingClean(id, type, frames));

        // Mmm... if we update the frame to current then we avoid adding extra
        // cleans and it's technically more accurate.
        // Honestly, from today's perspective, I can't think why else we'd be keeping
        // the frame except to handle cases like this.
        frames.put(id, current);        
 
        return last;       
    }
 
    /**
     *  Sweeps the pending expirations from the internal book-keeping.
     */
    public void sweep() {
        if( pending.isEmpty() ) {
            return;
        }
        for( PendingClean clean : pending ) {
            clean.clean();
        }
        pending.clear();
    }
 
    /** 
     *  Returns the last stored version for the specified ID and type or null
     *  if there is no tracking for the specified ID and type combination.
     */    
    public Long get( EntityId id, Class<? extends EntityComponent> type ) {
        Map<EntityId, Long> frames = getFrameMap(type, false);
        if( frames == null ) {
            return null;
        }
        return frames.get(id);
    }
    
    private class PendingClean {
        private EntityId id;
        private Class<? extends EntityComponent> type; 
        private Map<EntityId, Long> frames;        
 
        public PendingClean( EntityId id, Class<? extends EntityComponent> type, Map<EntityId, Long> frames ) {
            this.id = id;
            this.type = type;
            this.frames = frames;
        }  
        
        public void clean() {
            frames.remove(id);
        
            // We will clear the type entry if it is empty only because 
            // it lets the potentially-expanded internal storage of the
            // hashmap get GC'ed.  Generally, though, I'd expect these maps
            // to be long-living and relatively stable in size.
            if( frames.isEmpty() ) {
                map.remove(type);
            }
        }
    }     
}
