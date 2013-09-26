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

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;


/**
 *  A general set of factories for creating entities of
 *  various types.
 *
 *  @author    Paul Speed
 */
public class EntityFactories {
    private static EntityData ed;
 
    public static void initialize( EntityData ed ) {
        EntityFactories.ed = ed;                
    }
    
    public static EntityId createCollision( EntityId e1, ModelType type1, 
                                            EntityId e2, ModelType type2,
                                            long time, long decayLength, 
                                            EntityComponent... adds ) {
        Collision collision = new Collision(time, e1, type1, e2, type2);
        Decay decay = new Decay(time + decayLength);
        
        EntityId e = ed.createEntity();
        ed.setComponents(e, collision, decay);
        if( adds != null && adds.length > 0 ) {
            ed.setComponents(e, adds);
        }
        return e;                                                     
    }
    
    public static EntityId createBuff( long time, EntityId target, 
                                       EntityComponent... adds ) {
        EntityId e = ed.createEntity();
        ed.setComponent(e, new Buff(target, time));
        if( adds != null && adds.length > 0 ) {
            ed.setComponents(e, adds);
        } else {
            throw new IllegalArgumentException( "Buff entity created with no extra components." );
        }
        return e;                                                       
    }
    
    public static EntityId createObject( long time, Vector3f loc, 
                                         EntityComponent... adds ) {
        Position pos = new Position(loc, time, time);
        return createObject(pos, adds);
    }
                                             
    public static EntityId createObject( long time, 
                                         Vector3f loc, Quaternion facing, 
                                         EntityComponent... adds ) { 
        Position pos = new Position(loc, facing, time, time);
        return createObject(pos, adds);                                         
    }
    
    public static EntityId createObject( Position pos, EntityComponent... adds ) {
        EntityId e = ed.createEntity();
        ed.setComponent(e, pos);
        if( adds != null && adds.length > 0 ) {
            ed.setComponents(e, adds);
        }
        return e;        
    }
    
    public static EntityId createObject( ModelType type, long time, Vector3f loc, 
                                         EntityComponent... adds ) {
        return createObject(type, time, new Position(loc, time, time), adds );                                         
    }
                                             
    public static EntityId createObject( ModelType type, long time,                                                                                     
                                         Vector3f loc, Quaternion facing, 
                                         EntityComponent... adds ) {
        return createObject(type, time, new Position(loc, facing, time, time), adds); 
    }        
        
    public static EntityId createObject( ModelType type, long time,                                                                                     
                                         Position pos, 
                                         EntityComponent... adds ) {
        EntityId e = ed.createEntity();
        ed.setComponents(e, type, pos);
 
        // Now setup the rest... could use templates or factories or some combo       
        if( MonkeyTrapConstants.TYPE_OGRE.equals(type) ) {
            ed.setComponents(e, new Activity(Activity.SPAWNING, time, time + 2000 * 1000000L),
                                MonkeyTrapConstants.AI_SURVEY,
                                MonkeyTrapConstants.SPEED_OGRE,
                                new HitPoints(MonkeyTrapConstants.OGRE_HITPOINTS));
        } else if( MonkeyTrapConstants.TYPE_MONKEY.equals(type) ) {
            ed.setComponents(e, new Activity(Activity.SPAWNING, time, time + 2000 * 1000000L),
                                MonkeyTrapConstants.SPEED_MONKEY,
                                new HitPoints(MonkeyTrapConstants.MONKEY_HITPOINTS));
        } else {
            // Assume there is no more
        }
        
        // Just add the adds
        if( adds != null && adds.length > 0 ) {
            ed.setComponents(e, adds);
        }
        return e;        
    }                                          
}


