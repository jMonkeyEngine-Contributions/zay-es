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
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.Name;
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
 
    private LootItem BANANA = new LootItem( "Banana", MonkeyTrapConstants.TYPE_BANANA,
                                            -1, new HealthChange(1) );
                                                           
    private LootItem POTION1 = new LootItem( "Potion of Attack", 
                                             MonkeyTrapConstants.TYPE_POTION1,
                                             15000 * 1000000L, // 15 seconds
                                             new CombatStrength(1, 0, 0) );  
    private LootItem POTION2 = new LootItem( "Potion of Defense", 
                                             MonkeyTrapConstants.TYPE_POTION2,
                                             15000 * 1000000L, // 15 seconds
                                             new CombatStrength(0, 1, 0) );  
    private LootItem POTION3 = new LootItem( "Potion of Damage", 
                                             MonkeyTrapConstants.TYPE_POTION3,
                                             15000 * 1000000L, // 15 seconds
                                             new CombatStrength(0, 0, 1) );  
    private LootItem POTION4 = new LootItem( "Potion of Damage Reduction", 
                                             MonkeyTrapConstants.TYPE_POTION4,
                                             15000 * 1000000L, // 15 seconds
                                             new ArmorStrength(1) );
                                                            
    private LootItem RING1 = new LootItem( "+1 Ring of Attack", 
                                           MonkeyTrapConstants.TYPE_RING1,
                                           -1,
                                           new CombatStrength(1, 0, 0) );  
    private LootItem RING2 = new LootItem( "+1 Ring of Defense", 
                                           MonkeyTrapConstants.TYPE_RING2,
                                           -1,
                                           new CombatStrength(0, 1, 0) );  
    private LootItem RING3 = new LootItem( "+1 Ring of Damage", 
                                           MonkeyTrapConstants.TYPE_RING3,
                                           -1,
                                           new CombatStrength(0, 0, 1) );  
    private LootItem RING4 = new LootItem( "+1 Ring of Armor", 
                                           MonkeyTrapConstants.TYPE_RING4,
                                           -1,
                                           new ArmorStrength(1) );

 
    private LootItem[][] lootLevels = new LootItem[][] {
            {
                BANANA, BANANA, BANANA,
                POTION1, POTION2, POTION3, POTION4
            }, {
                BANANA, BANANA, BANANA,
                POTION1, POTION2, POTION3, POTION4,
                POTION1, POTION2, POTION3, POTION4,
                POTION1, POTION2, POTION3, POTION4,
                RING1, RING2, RING3, RING4
            }, {
                RING1, RING2, RING3, RING4
            }};
            
 
    private EntityData ed;    
    private EntitySet dead;
       
    public LootService() {
    }

    public void initialize( GameSystems systems ) {
        this.ed = systems.getEntityData();
        dead = ed.getEntities(Dead.class);
    }

    protected LootItem randomType( int level ) {
        LootItem[] loot = lootLevels[level];
        int index = (int)(Math.random() * loot.length);
        return loot[index];
    }

    public void update( long gameTime ) {
        dead.applyChanges();        
        for( Entity e : dead ) {
            // don't generate the loot until the time is right
            Dead death = e.get(Dead.class);
            if( death.getTime() > gameTime ) {
                continue;
            }
            
            // See if it still has hitpoints... if not then we've
            // already seen it and we're just waiting for it to decay
            if( ed.getComponent(e.getId(), HitPoints.class) == null ) {
                continue;
            }
            
            if( log.isDebugEnabled() ) {
                log.debug( "Dead:" + e );
            }
            e.set(new Decay(death.getTime() + 2000 * 1000000L));
            
            // Now figure out what loot to generate
            ModelType type = ed.getComponent(e.getId(), ModelType.class);
            Position pos = ed.getComponent(e.getId(), Position.class);
            if( type == null || pos == null ) {
                log.warn( "Entity:" + e + " is missing type or position, type:" + type + ", pos:" + pos);
            }
            
            if( type == MonkeyTrapConstants.TYPE_BARRELS ) {
                randomType(0).createObject(death.getTime(), pos == null? null : pos.getLocation());
            } else if( type == MonkeyTrapConstants.TYPE_OGRE ) {            
                randomType(1).createObject(death.getTime(), pos == null? null : pos.getLocation());
            } else if( type == MonkeyTrapConstants.TYPE_CHEST ) {
                randomType(2).createObject(death.getTime(), pos == null? null : pos.getLocation());
            } else if( type == MonkeyTrapConstants.TYPE_MONKEY ) {
                // Really should be everything that the monkey was carrying
                randomType(0).createObject(death.getTime(), pos == null? null : pos.getLocation());
            }
 
            // Remove the hitpoints completely from the object
            // because it no longer has any.  This is also the signal
            // that it is no longer solid.
            ed.removeComponent(e.getId(), HitPoints.class);
            
            // And if it is an AI then remove its AI type, also.
            AiType ai = ed.getComponent(e.getId(), AiType.class);
            if( ai != null ) {
                ed.removeComponent(e.getId(), AiType.class);
            }
        }
    }

    public void terminate( GameSystems systems ) {
        dead.release();
    }
    
    private class LootItem {
        String name;
        ModelType type;
        long decay;
        EntityComponent[] buffs;
        
        public LootItem( String name, ModelType type, long decay, EntityComponent... buffs ) {
            this.name = name;
            this.type = type;
            this.decay = decay;
            this.buffs = buffs;
        }
        
        public EntityId createObject( long time, Vector3f location ) {
        
            EntityId loot = EntityFactories.createObject(type, time, location, new Name(name));
            
            // Create the item buff entities for attachment
            // Right now none of our buffs conflict so we can just put
            // them all on one.  Not sure they will ever conflict in this
            // case so it's probably safe.
            EntityId itemBuffs = EntityFactories.createItemBuff(loot, decay, buffs);
 
            return loot;       
        }
    }
}
