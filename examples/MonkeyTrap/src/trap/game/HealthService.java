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
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *  Watches entities with hitpoints and entities with health
 *  changes and applies them to the hitpoints of an entity,
 *  possibly causing death.
 *  
 *  @author    Paul Speed
 */
public class HealthService implements Service {
 
    static Logger log = LoggerFactory.getLogger(HealthService.class);
 
    private EntityData ed;    
    private EntitySet living;
    private EntitySet changes;
    private Map<EntityId, Integer> health = new HashMap<EntityId, Integer>();
       
    public HealthService() {
    }

    public void initialize( GameSystems systems ) {
        this.ed = systems.getEntityData();
        living = ed.getEntities(HitPoints.class);
        changes = ed.getEntities(Buff.class, HealthChange.class);
    }

    public void update( long gameTime ) {
 
        // We accumulate all health adjustments together that are 
        // in effect at this time... and then apply them all at once.       
 
        // Make sure our entity views are up-to-date as of
        // now.       
        living.applyChanges();
        changes.applyChanges();
 
        // Collect all of the relevant health updates       
        for( Entity e : changes ) {
            Buff b = e.get(Buff.class);
            
            // Does the buff apply yet
            if( b.getStartTime() > gameTime )
                continue;
            
            HealthChange change = e.get(HealthChange.class);
            Integer hp = health.get(b.getTarget());
            if( hp == null )
                hp = change.getDelta();
            else
                hp += change.getDelta();
            health.put(b.getTarget(), hp);
            
            // Delete the buff entity
            ed.removeEntity(e.getId());            
        }
        
        // Now apply all accumulated adjustments
        for( Map.Entry<EntityId, Integer> entry : health.entrySet() ) {
            Entity target = living.getEntity(entry.getKey());
            if( target == null ) {
                log.warn("No target for id:" + entry.getKey());
                continue;
            }
            
            HitPoints hp = target.get(HitPoints.class);
            hp = hp.newAdjusted(entry.getValue()); 
            if( log.isInfoEnabled() ) {
                log.info("Applying " + entry.getValue() + " to:" + target + " result:" + hp );
            } 
            target.set(hp); 
        }
        
        // Clear our health book-keeping map.
        health.clear();  
    }

    public void terminate( GameSystems systems ) {
        living.release();
        changes.release();
    }
    
}
