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

package panic;

import com.jme3.input.KeyInput;

import com.simsilica.lemur.input.FunctionId;
import com.simsilica.lemur.input.InputMapper;
import com.simsilica.lemur.input.InputState;


/**
 *  Defines a set of standard input function IDs and their default
 *  control mappings.  The ShipControlState uses these function IDs
 *  as triggers to the ship control.
 *
 *  @author    Paul Speed
 */
public class ShipFunctions {

    /**
     *  The group to which these functions are assigned for
     *  easy grouping and also for easy activation and deactivation
     *  of the entire group.
     */
    public static final String GROUP = "Ship Controls";

    /**
     *  Turns the ship left or right.  Default controls are setup
     *  as 'a' and 'd'.
     */
    public static final FunctionId F_TURN = new FunctionId(GROUP, "Turn");

    /**
     *  Thrusts the ship forward in its current direction.  Default control
     *  mapping is 'w'.
     */
    public static final FunctionId F_THRUST = new FunctionId(GROUP, "Thrust");

    /**
     *  Shoots the pimary weapon of the ship.  Default control mapping
     *  is the space bar.
     */
    public static final FunctionId F_SHOOT = new FunctionId(GROUP, "Shoot");

    /**
     *  Initializes a default set of input mappings for the ship functions.
     *  These can be changed later without impact... or multiple input
     *  controls can be mapped to the same function.
     */
    public static void initializeDefaultMappings( InputMapper inputMapper ) {
        // Default key mappings
        inputMapper.map(F_TURN, KeyInput.KEY_A);
        inputMapper.map(F_TURN, InputState.NEGATIVE, KeyInput.KEY_D);
        inputMapper.map(F_THRUST, KeyInput.KEY_W);
        inputMapper.map(F_SHOOT, KeyInput.KEY_SPACE);
    }
}
