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
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *  Watches entities with CombatStrength and ArmorStrength buffs and
 *  applies them to the target entity.
 *  
 *  @author    Paul Speed
 */
public class CombatBuffService implements Service {
 
    static Logger log = LoggerFactory.getLogger(CombatBuffService.class);
 
    private EntityData ed;    
    private EntitySet combatBuffs;
    private EntitySet armorBuffs;
       
    private Map<EntityId, ActiveCombatBuff> activeCombatBuffs = new HashMap<EntityId, ActiveCombatBuff>();
    private Set<Entity> delayedCombatBuffs = new HashSet<Entity>();
    private Map<EntityId, ActiveArmorBuff> activeArmorBuffs = new HashMap<EntityId, ActiveArmorBuff>();
    private Set<Entity> delayedArmorBuffs = new HashSet<Entity>();
       
    public CombatBuffService() {
    }

    public void initialize( GameSystems systems ) {
        this.ed = systems.getEntityData();
        combatBuffs = ed.getEntities(Buff.class, CombatStrength.class);
        armorBuffs = ed.getEntities(Buff.class, ArmorStrength.class);
        
        addCombatBuffs(combatBuffs, -1);
        addArmorBuffs(armorBuffs, -1);
    }
    
    protected void removeCombatBuffs( Set<Entity> set, long gameTime ) {
        for( Entity e : set ) {
            if( log.isDebugEnabled() ) {
                log.debug( "Remove buff:" + e );
            }
            ActiveCombatBuff buff = activeCombatBuffs.remove(e.getId());
            if( buff == null ) {
                log.warn( "Buff:" + e + " did not have an active entry." );
                continue;  
            }
            
            CombatStrength existing = ed.getComponent(buff.mob, CombatStrength.class);
            ed.setComponent(buff.mob, existing.newRemoved(buff.buff)); 
        }
    }
     
    protected void addCombatBuffs( Set<Entity> set, long gameTime ) {
        for( Entity e : set ) {
            Buff b = e.get(Buff.class);                        
            if( b.getStartTime() > gameTime ) {
                delayedCombatBuffs.add(e);
                continue;
            }
                           
            if( log.isDebugEnabled() ) {
                log.debug("Add buff:" + e);
            }                
            
            EntityId mob = b.getTarget();
            CombatStrength existing = ed.getComponent(mob, CombatStrength.class);
            CombatStrength change = e.get(CombatStrength.class);
            ed.setComponent(mob, existing.newAdded(change));
            
            // Remember the change for later.  If decay removes
            // the entity then we won't be able to query it for the
            // strength it had.  So we must keep track.
            activeCombatBuffs.put(e.getId(), new ActiveCombatBuff(mob, change)); 
        }
    } 

    protected void removeArmorBuffs( Set<Entity> set, long gameTime ) {
        for( Entity e : set ) {
            if( log.isDebugEnabled() ) {
                log.debug( "Remove buff:" + e );
            }   
            ActiveArmorBuff buff = activeArmorBuffs.remove(e.getId());
            if( buff == null ) {
                log.warn( "Buff:" + e + " did not have an active entry." );
                continue;  
            }
            
            ArmorStrength existing = ed.getComponent(buff.mob, ArmorStrength.class);
            ed.setComponent(buff.mob, existing.newRemoved(buff.buff)); 
        }
    }
     
    protected void addArmorBuffs( Set<Entity> set, long gameTime ) {
        for( Entity e : set ) {
            Buff b = e.get(Buff.class);               
            if( b.getStartTime() > gameTime ) {
                delayedArmorBuffs.add(e);
                continue;
            }
            
            if( log.isDebugEnabled() ) {
                log.debug( "Add buff:" + e );
            }
            
            EntityId mob = b.getTarget();
            ArmorStrength existing = ed.getComponent(mob, ArmorStrength.class);
            ArmorStrength change = e.get(ArmorStrength.class);
            ed.setComponent(mob, existing.newAdded(change));
            
            // Remember the change for later.  If decay removes
            // the entity then we won't be able to query it for the
            // strength it had.  So we must keep track.
            activeArmorBuffs.put(e.getId(), new ActiveArmorBuff(mob, change)); 
        }
    } 
    
    public void update( long gameTime ) {
        if( !delayedCombatBuffs.isEmpty() ) {
            // Not entirely efficient but easy to code
            Set<Entity> temp = new HashSet<Entity>(delayedCombatBuffs);
            delayedCombatBuffs.clear();
            addCombatBuffs(temp, gameTime);
        }
        
        if( !delayedArmorBuffs.isEmpty() ) {
            // Not entirely efficient but easy to code
            Set<Entity> temp = new HashSet<Entity>(delayedArmorBuffs);
            delayedArmorBuffs.clear();
            addArmorBuffs(temp, gameTime);
        }
        
        if( combatBuffs.applyChanges() ) {
            removeCombatBuffs(combatBuffs.getRemovedEntities(), gameTime);
            addCombatBuffs(combatBuffs.getAddedEntities(), gameTime);
        }
        
        if( armorBuffs.applyChanges() ) {
            removeArmorBuffs(armorBuffs.getRemovedEntities(), gameTime);
            addArmorBuffs(armorBuffs.getAddedEntities(), gameTime);
        }            
    }

    public void terminate( GameSystems systems ) {
        combatBuffs.release();
        armorBuffs.release();
    }
 
    private class ActiveCombatBuff {
        EntityId mob;
        CombatStrength buff;
        
        public ActiveCombatBuff( EntityId mob, CombatStrength buff ) {
            this.mob = mob;
            this.buff = buff;
        }
        
        @Override
        public String toString() {
            return "ActiveBuff[" + mob + ", " + buff + "]";
        }         
    }
       
    private class ActiveArmorBuff {
        EntityId mob;
        ArmorStrength buff;
        
        public ActiveArmorBuff( EntityId mob, ArmorStrength buff ) {
            this.mob = mob;
            this.buff = buff;
        }
        
        @Override
        public String toString() {
            return "ActiveBuff[" + mob + ", " + buff + "]";
        }         
    }   
}
