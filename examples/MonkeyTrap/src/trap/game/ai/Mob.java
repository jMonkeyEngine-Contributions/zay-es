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
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import java.util.HashMap;
import java.util.Map;
import trap.game.Position;


/**
 *  Keeps track of the state and other state-related
 *  attributes for a given entity within the AI service.
 *
 *  @author    Paul Speed
 */
public class Mob {
    private EntityData ed;
    private Entity entity;
    private AiType aiType;
    private StateMachine fsm;
    private Map<String, Object> attributes = new HashMap<String, Object>();
    
    public Mob( EntityData ed, Entity e ) {
        this.ed = ed;
        this.entity = e;
    }
 
    public <T extends EntityComponent> T getComponent( Class<T> type ) {
        return ed.getComponent(entity.getId(), type);
    }
    
    public void setComponents( EntityComponent... components ) {
        ed.setComponents(entity.getId(), components);
    }
 
    public Entity getEntity() {
        return entity;
    }
 
    public Position getPosition() {
        return entity.get(Position.class);
    }
 
    public boolean setAiType( AiType type ) {
        if( this.aiType == type ) {
            return false;
        }
        this.aiType = type;
        return true;
    }
    
    public AiType getAiType() {
        return aiType;
    }
 
    public void setStateMachine( StateMachine fsm ) {
        if( this.fsm != null ) {
            this.fsm.stop();
        }
        this.fsm = fsm;
        if( this.fsm != null ) {
            this.fsm.start();
        }
    }
    
    public StateMachine getStateMachine() {
        return fsm;
    }
    
    public void set( String s, Object o ) {
        attributes.put(s, o);
    }
    
    public <T> T get( String s ) {
        return (T)attributes.get(s);
    }  
}
