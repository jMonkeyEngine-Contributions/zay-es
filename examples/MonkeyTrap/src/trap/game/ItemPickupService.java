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

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Collections2;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trap.game.ItemBuff;
import trap.game.ai.AiType;


/**
 *  Watches for collision entities and does the appropriate
 *  pickup behavior if it has ItemBuffs.
 *  
 *  @author    Paul Speed
 */
public class ItemPickupService implements Service {
 
    static Logger log = LoggerFactory.getLogger(ItemPickupService.class);
 
    private static final Function<Entity, EntityId> ITEM_ID = new ItemIdFunction(); 
 
    private EntityData ed;    
    private EntitySet collisions;
    private EntitySet itemBuffs;
    private Set<EntityId> items = new HashSet<EntityId>();
       
    public ItemPickupService() {
    }

    public void initialize( GameSystems systems ) {
        this.ed = systems.getEntityData();
        
        collisions = ed.getEntities(Collision.class);
        itemBuffs = ed.getEntities(ItemBuff.class);        
        handleCollisions(collisions);
    }
    
    protected void handleCollisions( Set<Entity> set ) {
        for( Entity e : set ) {
            Collision c = e.get(Collision.class);
            EntityId mob = c.getCollider1();
            EntityId item = c.getCollider2();
            if( items.contains(item) ) {
                // It's one of ours
            
                // We'll assume for the moment that only monkeys
                // are players and that all players are monkeys
                if( !MonkeyTrapConstants.TYPE_MONKEY.equals(c.getType1()) ) {
                    continue;
                }
 
                if( log.isDebugEnabled() ) {
                    log.debug( "Picking up:" + c.getCollider2() + "  for player:" + mob );
                }
                
                // We will apply the item buffs directly though
                // we could/should attach the item to the player and
                // let some other service apply them and unapply them
                // on add/remove
                // Apply all item buffs for this item
                for( Entity buff : itemBuffs ) {
                    ItemBuff b = buff.get(ItemBuff.class);
                    if( Objects.equal(b.getTarget(), item) ) {
                        // Set the buff to the player
                        buff.set(new Buff(mob, c.getTime()));
 
                        // Remove the item buff so we stop watching this
                        // entity.                        
                        ed.removeComponent(buff.getId(), ItemBuff.class);
                        
                        // And if there is a decay time on the item then
                        // set the decay too
                        if( b.getDecay() >= 0 ) {
                            buff.set(new Decay(c.getTime() + b.getDecay()));
                        }
                    }
                }
 
                // Remove the item               
                ed.removeEntity(item);
                
                // And remove the collision since we dealt with it
                ed.removeEntity(e.getId());  
            }
        }
    }
     
    public void update( long gameTime ) {
        if( itemBuffs.applyChanges() ) {
            items.addAll(Collections2.transform(itemBuffs.getAddedEntities(), ITEM_ID));
            items.removeAll(Collections2.transform(itemBuffs.getRemovedEntities(), ITEM_ID));
        }
        
        collisions.applyChanges();
        handleCollisions(collisions);
    }

    public void terminate( GameSystems systems ) {
        collisions.release();
    }
 
    private static class ItemIdFunction implements Function<Entity, EntityId> {
        public EntityId apply( Entity e ) {
            if( e == null ) {
                return null;
            }
            ItemBuff buff = e.get(ItemBuff.class);
            return buff != null ? buff.getTarget() : null;
        }
    }   
}
