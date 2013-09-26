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

import trap.game.Direction;
import trap.game.Position;
import trap.game.SensorArea;
import com.jme3.app.Application;
import com.jme3.audio.Listener;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
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
    
    private Node interpNode;
    private Position lastPos;
    private Quaternion cameraAngle;
    private Vector3f cameraDelta;
    private Vector3f audioDelta;
    private float cameraDistance = 20; //12;
 
    // Here for the moment
    private SensorArea sensor;
    private int xLast = -1;
    private int yLast = -1;
 
    private Listener audioListener = new Listener();
 
    public PlayerState( GameClient client ) {
        this.client = client;
    }

    public GameClient getClient() {
        return client;
    }

    public Listener getAudioListener() {
        return audioListener;
    }

    @Override
    protected void initialize( Application app ) {
 
        app.getAudioRenderer().setListener(audioListener);
         
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
        cameraDelta.multLocal(-cameraDistance);

        audioListener.setRotation(cameraAngle);
        audioDelta = cameraAngle.mult(Vector3f.UNIT_Z);
        audioDelta.multLocal(4);        
        
        // Back it up a little so the framing is more even
        cameraDelta.addLocal(0, -1, 0);
        
        sensor = new SensorArea(getState(MazeState.class).getMaze(), 4);                
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
     
        Position pos = ed.getComponent(player, Position.class);

        if( interpNode != null ) {
 
            if( pos != lastPos ) {
                lastPos = pos;
                if( pos != null ) {
                    interpNode.getControl(InterpolationControl.class).setTarget(pos.getLocation(), pos.getFacing(), pos.getChangeTime(), pos.getTime());
                }
            }
            
            // Make sure it is up to date
            interpNode.updateLogicalState(tpf);
        
            Vector3f loc = cam.getLocation();
            loc.set(interpNode.getLocalTranslation());
            loc.addLocal(cameraDelta);
            cam.setLocation(loc);
 
            loc = audioListener.getLocation(); 
            loc.set(interpNode.getLocalTranslation());
            loc.addLocal(audioDelta);
            audioListener.setLocation(loc);
        }                
        
        if( pos != null ) {        
            Vector3f loc = pos.getLocation();
            
            int x = (int)loc.x / 2; 
            int y = (int)loc.z / 2;
            
            if( x != xLast || y != yLast ) {
                xLast = x;
                yLast = y;
                sensor.setCenter(x, y);
 
                getState(MazeState.class).clearVisibility(MazeState.PLAYER_VISIBLE);
                getState(MazeState.class).setVisibility(sensor, MazeState.PLAYER_VISIBLE | MazeState.PLAYER_VISITED); 
            }            
        }       
    }

    @Override
    protected void enable() {
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();    
        inputMapper.activateGroup(PlayerFunctions.GROUP);
        
        // Create a node that we will use for interpolation... this
        // way we get to reuse the interpolation control.
        interpNode = new Node("interp");
        Position pos = ed.getComponent(player, Position.class);
        if( pos != null ) {
            interpNode.setLocalTranslation(pos.getLocation().mult(2));
        }
        interpNode.addControl(new InterpolationControl(client.getRenderTimeProvider()));
 
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
