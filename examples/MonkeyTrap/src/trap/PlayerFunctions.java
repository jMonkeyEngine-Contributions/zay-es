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

package trap;

import com.jme3.input.KeyInput;

import com.simsilica.lemur.input.FunctionId;
import com.simsilica.lemur.input.InputMapper;
import com.simsilica.lemur.input.InputState;


/**
 *  Defines a set of standard input function IDs and their default
 *  control mappings.  The PlayerControlState uses these function IDs
 *  as triggers to the player control.
 *
 *  @author    Paul Speed
 */
public class PlayerFunctions {

    /**
     *  The group to which these functions are assigned for
     *  easy grouping and also for easy activation and deactivation
     *  of the entire group.
     */
    public static final String GROUP = "Player Controls";

    public static final FunctionId F_NORTH = new FunctionId(GROUP, "North");
    public static final FunctionId F_SOUTH = new FunctionId(GROUP, "South");
    public static final FunctionId F_EAST = new FunctionId(GROUP, "East");
    public static final FunctionId F_WEST = new FunctionId(GROUP, "West");
    
    public static final FunctionId F_EXIT = new FunctionId(GROUP, "Exit");
    public static final FunctionId F_SCREENSHOT = new FunctionId("Screen Shot");

    /**
     *  Initializes a default set of input mappings for the ship functions.
     *  These can be changed later without impact... or multiple input
     *  controls can be mapped to the same function.
     */
    public static void initializeDefaultMappings( InputMapper inputMapper ) {
        // Default key mappings
        inputMapper.map(F_NORTH, KeyInput.KEY_W);
        inputMapper.map(F_SOUTH, KeyInput.KEY_S);
        inputMapper.map(F_EAST, KeyInput.KEY_D);
        inputMapper.map(F_WEST, KeyInput.KEY_A);
        inputMapper.map(F_EXIT, KeyInput.KEY_ESCAPE);
        inputMapper.map(F_SCREENSHOT, KeyInput.KEY_F2);
    }
}
