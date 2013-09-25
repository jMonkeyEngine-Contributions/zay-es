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

import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityId;


/**
 *
 *  @author    Paul Speed
 */
public class Collision implements EntityComponent {
    private EntityId collider1;
    private EntityId collider2;
    private ModelType type1;
    private ModelType type2;
    
    public Collision( EntityId collider1, ModelType type1,
                      EntityId collider2, ModelType type2 ) {
        this.collider1 = collider1;
        this.type1 = type1;
        this.collider2 = collider2;
        this.type2 = type2;                      
    }
    
    public EntityId getCollider1() {
        return collider1;
    }
    
    public ModelType getType1() {
        return type1;
    }
 
    public EntityId getCollider2() {
        return collider2;
    }
    
    public ModelType getType2() {
        return type2;
    }
    
    @Override
    public String toString() {
        return "Collision[" + collider1 + ":" + type1 + ", " + collider2 + ":" + type2 + "]";
    }                      
}
