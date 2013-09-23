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

import java.util.*;
import trap.game.GameSystems;

/**
 *  A set of states and a default state, and so on, that can be used
 *  to create a state machine of a particular setup. 
 *
 *  @author    Paul Speed
 */
public class StateMachineConfig {
    public static final State NULL_STATE = new NullState();

    private State defaultState;
    private Map<String, State> states = new HashMap<String, State>();
    
    public StateMachineConfig() {
        this(NULL_STATE);
    }
    
    public StateMachineConfig( State defaultState ) {
        this.defaultState = defaultState;
    }
 
    public void addState( String id, State state ) {
        states.put(id, state);
    }
    
    public State getState( String id ) {
        State s = states.get(id);
        if( s == null ) {
            throw new RuntimeException("State not found for:" + id); 
        }
        return s;        
    }
 
    public State getDefaultState() {
        return defaultState;
    }
    
    public StateMachine create( GameSystems systems, Mob mob ) {
        return new StateMachine(systems, mob, this);
    }
    
    protected static class NullState implements State {

        public void enter( StateMachine fsm, Mob mob ) {
        }

        public void execute( StateMachine fsm, long time, Mob mob ) {
        }

        public void leave( StateMachine fsm, Mob mob ) {
        }
    }
}
