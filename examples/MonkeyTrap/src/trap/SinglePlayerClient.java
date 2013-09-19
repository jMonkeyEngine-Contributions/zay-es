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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;


/**
 *
 *  @author    Paul Speed
 */
public class SinglePlayerClient implements GameClient
{
    private EntityData ed;
    private EntityId player;
    private long frameDelay = 100 * 1000000L; // 100 ms 

    private Direction currentDir = Direction.South;
    private long nextMove = 0;    

    public SinglePlayerClient( EntityData ed, EntityId player ) {
        this.ed = ed;
        this.player = player;
    }
   
    public long getGameTime() {
        return System.nanoTime();
    }
   
    public long getRenderTime() {
        return System.nanoTime() - frameDelay;
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
 
        if( dir == currentDir ) {       
            Position current = ed.getComponent(player, Position.class);
            Position next = new Position(dir.forward(current.getLocation(), 2),
                                        dir.getFacing());
            ed.setComponent(player, next);
        
            nextMove = time + 500 * 1000000L;
        } else {
            // Change the dir first... but that's quicker
            currentDir = dir;
            Position current = ed.getComponent(player, Position.class);
            Position next = new Position(current.getLocation(), dir.getFacing());
            ed.setComponent(player, next);
            nextMove = time + 100 * 1000000L;             
        }       
    }
}
