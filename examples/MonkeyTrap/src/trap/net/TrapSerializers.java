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

package trap.net;

import com.jme3.network.serializing.Serializer;
import com.jme3.network.serializing.serializers.FieldSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trap.game.Activity;
import trap.game.ArmorStrength;
import trap.game.Buff;
import trap.game.Collision;
import trap.game.CombatStrength;
import trap.game.Dead;
import trap.game.Decay;
import trap.game.HealthChange;
import trap.game.HitPoints;
import trap.game.ItemBuff;
import trap.game.MaxHitPoints;
import trap.game.MeleeTarget;
import trap.game.ModelType;
import trap.game.MoveTo;
import trap.game.Position;
import trap.game.Speed;
import trap.net.msg.MazeDataMessage;
import trap.net.msg.PlayerInfoMessage;


/**
 *
 *  @author    Paul Speed
 */
public class TrapSerializers {

    static Logger log = LoggerFactory.getLogger(TrapSerializers.class);
    
    private static final Class[] classes = {
        MazeDataMessage.class,
        PlayerInfoMessage.class
    };
    
    private static final Class[] forced = {
        Activity.class,
        ArmorStrength.class,
        Buff.class,
        Collision.class,
        CombatStrength.class,
        Dead.class,
        Decay.class,
        HealthChange.class,
        HitPoints.class,
        ItemBuff.class,
        MaxHitPoints.class,
        MeleeTarget.class,
        ModelType.class,
        MoveTo.class,
        Position.class,
        Speed.class,
    };

    public static void initialize() {
 
        Serializer.registerClasses(classes);
    
        // Register these manually since Spider Monkey currently
        // requires them all to have @Serializable but we already know
        // which serializer we want to use.  Eventually I will fix SM
        // but for now I'll do this here.
        Serializer fieldSerializer = new FieldSerializer();
        boolean error = false;        
        for( Class c : forced) {
            try {
                Serializer.registerClass(c, fieldSerializer);
            } catch( Exception e ) {
                log.error("Error registering class:" + c, e);
                error = true;
            }
        }
        if( error ) {
            throw new RuntimeException("Some classes failed to register");
        }
    }
}


