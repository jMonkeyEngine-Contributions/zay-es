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

import com.jme3.app.Application;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.event.BaseAppState;
import com.simsilica.lemur.input.AnalogFunctionListener;
import com.simsilica.lemur.input.FunctionId;
import com.simsilica.lemur.input.InputMapper;


/**
 *
 *  @author    Paul Speed
 */
public class PlayerState extends BaseAppState
                         implements AnalogFunctionListener {
                         
    private GameClient client;
    private EntityData ed;
    private EntityId player;
    
    private Spatial monkey;
    private Quaternion cameraAngle;
    private Vector3f cameraDelta;
 
    public PlayerState( GameClient client ) {
        this.client = client;
    }

    @Override
    protected void initialize( Application app ) {
 
        this.ed = client.getEntityData();
        this.player = client.getPlayer();
    
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.addAnalogListener(this,
                                      PlayerFunctions.F_NORTH,
                                      PlayerFunctions.F_SOUTH,
                                      PlayerFunctions.F_EAST,
                                      PlayerFunctions.F_WEST);
 
        cameraAngle = new Quaternion().fromAngles(FastMath.QUARTER_PI * 1.3f, FastMath.PI, 0);
        cameraDelta = cameraAngle.mult(Vector3f.UNIT_Z);
        cameraDelta.multLocal(-12);
        
        // Back it up a little so the framing is more even
        cameraDelta.addLocal(0, -1, 0);                
    }

    @Override
    protected void cleanup( Application app ) {
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.removeAnalogListener(this,
                                         PlayerFunctions.F_NORTH,
                                         PlayerFunctions.F_SOUTH,
                                         PlayerFunctions.F_EAST,
                                         PlayerFunctions.F_WEST);
    }

    @Override
    public void update( float tpf ) {
        Camera cam = getApplication().getCamera();
 
        // Set the camera relative to the monkey
        if( monkey != null ) {
            Vector3f loc = cam.getLocation();
            loc.set(monkey.getLocalTranslation());
            //loc.addLocal(0, 7, 7);
            loc.addLocal(cameraDelta);
            cam.setLocation(loc);
        }       
    }

    @Override
    protected void enable() {
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();    
        inputMapper.activateGroup(PlayerFunctions.GROUP);
        
        // Grab our monkey
        monkey = getState(ModelState.class).getSpatial(player);
 
        Camera cam = getApplication().getCamera();       
        cam.setRotation(cameraAngle);
    }

    @Override
    protected void disable() {
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();    
        inputMapper.deactivateGroup(PlayerFunctions.GROUP);
    }

    public void valueActive( FunctionId func, double value, double tpf ) {
        if( Math.abs(value) < 0.5 ) {
            return;
        }
        
        if( func == PlayerFunctions.F_NORTH ) {
            client.move(Direction.North);
        } else if( func == PlayerFunctions.F_SOUTH ) {
            client.move(Direction.South);
        } else if( func == PlayerFunctions.F_EAST ) {
            client.move(Direction.East);
        } else if( func == PlayerFunctions.F_WEST ) {
            client.move(Direction.West);
        }
    }
    
}
