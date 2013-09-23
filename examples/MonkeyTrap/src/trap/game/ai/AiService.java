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

package trap.game.ai;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import trap.game.GameSystems;
import trap.game.MazeService;
import trap.game.MonkeyTrapConstants;
import trap.game.Position;
import trap.game.Service;


/**
 *
 *  @author    Paul Speed
 */
public class AiService implements Service {

    private GameSystems systems;
    private EntityData ed;
    private MazeService mazeService;

    private Map<AiType, StateMachineConfig> configs = new HashMap<AiType, StateMachineConfig>();

    private EntitySet mobs;
    private Map<EntityId, Mob> mobMap = new HashMap<EntityId, Mob>();

    private long lastTime; 
    
    public AiService() {
    
        // Some default configurations
        StateMachineConfig cfg = new StateMachineConfig(new RandomWanderState());        
        configs.put( MonkeyTrapConstants.AI_DRUNK, cfg ); 
    }

    public void initialize( GameSystems systems ) {
        this.systems = systems;
        this.ed = systems.getEntityData();
        this.mazeService = systems.getService(MazeService.class); 
 
        // Right now we will grab all AI types but we should eventually
        // maybe be selective.
        mobs = ed.getEntities(AiType.class, Position.class);        
    }

    protected void removeMobs( Set<Entity> set ) {
        for( Entity e : set ) {
            mobMap.remove(e.getId());
        }
    }

    protected Mob getMob( Entity e, boolean create ) {
        Mob result = mobMap.get(e.getId());
        if( result == null && create ) {
            result = new Mob(ed, e);
            mobMap.put(e.getId(), result);
        }
        return result;
    }
    
    protected void updateMobs( Set<Entity> set ) {
        for( Entity e : set ) {
            Mob m = getMob(e, true);
            AiType type = e.get(AiType.class);
 
            if( m.setAiType(type) ) {           
                // Reset this Mob's FSM to the one for this AI type
                StateMachineConfig config = configs.get(type);
                StateMachine fsm = config.create(systems, m);
                m.setStateMachine(fsm); 
            }
        }
    }

    protected void refreshMobs() {
        if( mobs.applyChanges() ) {
            removeMobs(mobs.getRemovedEntities());
            updateMobs(mobs.getAddedEntities());
            updateMobs(mobs.getChangedEntities());
        }
    }

    public void update( long gameTime ) {
        refreshMobs();
        
        // And now update the state machines
        for( Mob mob : mobMap.values() ) {
            mob.getStateMachine().update(gameTime);
        }
    }

    public void terminate( GameSystems systems ) {
        removeMobs(mobs);
        mobs.release();
    }
}
