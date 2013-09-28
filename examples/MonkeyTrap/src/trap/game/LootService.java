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

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trap.game.ai.AiType;


/**
 *  Generates loot when an entity dies.
 *  
 *  @author    Paul Speed
 */
public class LootService implements Service {
 
    static Logger log = LoggerFactory.getLogger(LootService.class); 
 
    private static ModelType[][] lootLevels = new ModelType[][] {
            {
                MonkeyTrapConstants.TYPE_BANANA,
                MonkeyTrapConstants.TYPE_BANANA,
                MonkeyTrapConstants.TYPE_BANANA,
                MonkeyTrapConstants.TYPE_POTION1,
                MonkeyTrapConstants.TYPE_POTION2,
                MonkeyTrapConstants.TYPE_POTION3
            }, {
                MonkeyTrapConstants.TYPE_POTION1,
                MonkeyTrapConstants.TYPE_POTION2,
                MonkeyTrapConstants.TYPE_POTION3,
                MonkeyTrapConstants.TYPE_POTION1,
                MonkeyTrapConstants.TYPE_POTION2,
                MonkeyTrapConstants.TYPE_POTION3,
                MonkeyTrapConstants.TYPE_POTION1,
                MonkeyTrapConstants.TYPE_POTION2,
                MonkeyTrapConstants.TYPE_POTION3,
                MonkeyTrapConstants.TYPE_RING1,
                MonkeyTrapConstants.TYPE_RING2,
                MonkeyTrapConstants.TYPE_RING3
            }, {
                MonkeyTrapConstants.TYPE_RING1,
                MonkeyTrapConstants.TYPE_RING2,
                MonkeyTrapConstants.TYPE_RING3
            }};
            
 
    private EntityData ed;    
    private EntitySet dead;
       
    public LootService() {
    }

    public void initialize( GameSystems systems ) {
        this.ed = systems.getEntityData();
        dead = ed.getEntities(Dead.class);
    }

    protected ModelType randomType( int level ) {
        ModelType[] loot = lootLevels[level];
        int index = (int)(Math.random() * loot.length);
        return loot[index];
    }

    public void update( long gameTime ) {
        dead.applyChanges();
        for( Entity e : dead ) {
            // don't generate the loot until the time is right
            Dead death = e.get(Dead.class);
            if( death.getTime() < gameTime ) {
                continue;
            }
            
            System.out.println( "Dead:" + e );
            e.set(new Decay(death.getTime() + 2000 * 1000000L));
            
            // Now figure out what loot to generate
            ModelType type = ed.getComponent(e.getId(), ModelType.class);
            Position pos = ed.getComponent(e.getId(), Position.class);
            if( type == null || pos == null ) {
                log.warn( "Entity:" + e + " is missing type or position, type:" + type + ", pos:" + pos);
            }
            
            if( type == MonkeyTrapConstants.TYPE_BARRELS ) {
                EntityFactories.createObject(randomType(0), death.getTime(), 
                                             pos.getLocation());
            } else if( type == MonkeyTrapConstants.TYPE_OGRE ) {            
                EntityFactories.createObject(randomType(1), death.getTime(), 
                                             pos.getLocation());
            } else if( type == MonkeyTrapConstants.TYPE_CHEST ) {
                EntityFactories.createObject(randomType(2), death.getTime(), 
                                             pos.getLocation());
            } else if( type == MonkeyTrapConstants.TYPE_MONKEY ) {
                EntityFactories.createObject(MonkeyTrapConstants.TYPE_BANANA, death.getTime(), 
                                             pos.getLocation());
            }
                        
            // Also if it isn't an AI than remove its position and
            // if it is an AI then remove it's brain
            /*
            The problem with doing this is that there is no time
            associated with it... so it often is seen before the
            actions that caused it.
            AiType ai = ed.getComponent(e.getId(), AiType.class);
            if( ai == null ) {
                ed.removeComponent(e.getId(), Position.class);
            } else {
                ed.removeComponent(e.getId(), AiType.class);
            }*/            
        }
    }

    public void terminate( GameSystems systems ) {
        dead.release();
    }
    
}
