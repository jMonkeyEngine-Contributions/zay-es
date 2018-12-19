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

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.input.AnalogFunctionListener;
import com.simsilica.lemur.input.FunctionId;
import com.simsilica.lemur.input.InputMapper;
import com.simsilica.lemur.input.InputState;
import com.simsilica.lemur.input.StateFunctionListener;


/**
 *  Maps player input into ship control.  Note: this state
 *  takes over the job of applying acceleration to the ship's
 *  velocity.  This could easily be moved into the physics
 *  system by adding an Acceleration component.
 *
 *  @author    Paul Speed
 */
public class ShipControlState extends BaseAppState
                              implements AnalogFunctionListener, StateFunctionListener {

    private EntityData ed;
    private EntityId ship;
    private long lastFrame;

    private float rotateSpeed = 3;
    private float speed = 1;
    private float maxSpeed = 3; // units/sec
    private float dampening = 0.9f;
    private float bulletSpeed = 4;
    private long bulletDecay = 1500;
    private float bulletOffset = 0.2f;

    private double lastThrustTime = 0.25;
    private double thrustInterval = 0.25;

    private AudioNode shoot;
    private AudioNode thrust;

    private Vector3f accel = new Vector3f();

    private EntityId lastBullet;

    public ShipControlState( EntityId ship ) {
        this.ship = ship;
    }

    @Override
    protected void initialize( Application app ) {
        ed = getState(EntityDataState.class).getEntityData();

        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.addAnalogListener(this,
                                      ShipFunctions.F_TURN,
                                      ShipFunctions.F_THRUST);
        inputMapper.addStateListener(this,
                                     ShipFunctions.F_THRUST,
                                     ShipFunctions.F_SHOOT);

        shoot = new AudioNode(app.getAssetManager(), "Sounds/shoot.ogg", DataType.Buffer);
        shoot.setReverbEnabled(false);
        thrust = new AudioNode(app.getAssetManager(), "Sounds/thrust.ogg", DataType.Buffer);
        thrust.setReverbEnabled(false);
        thrust.setLooping(true);
    }

    @Override
    protected void cleanup( Application app ) {
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.removeAnalogListener(this,
                                         ShipFunctions.F_TURN,
                                         ShipFunctions.F_THRUST);
        inputMapper.removeStateListener(this,
                                        ShipFunctions.F_THRUST,
                                        ShipFunctions.F_SHOOT);
    }

    @Override
    protected void onEnable() {
        lastFrame = System.nanoTime();

        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.activateGroup(ShipFunctions.GROUP);
    }

    @Override
    protected void onDisable() {
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.deactivateGroup(ShipFunctions.GROUP);
    }

    @Override
    public void valueActive( FunctionId func, double value, double tpf ) {
        if( func == ShipFunctions.F_TURN ) {

            Velocity vel = ed.getComponent(ship, Velocity.class);
            float rotate = (float)(value * rotateSpeed);
            ed.setComponent(ship, new Velocity(vel.getLinear(), new Vector3f(0,0,rotate)));
        } else if( func == ShipFunctions.F_THRUST ) {

            Position pos = ed.getComponent(ship, Position.class);
            accel.set(0,(float)(speed * value),0);
            pos.getFacing().multLocal(accel);

            lastThrustTime += tpf;
            if( value != 0 && lastThrustTime >= thrustInterval ) {

                lastThrustTime = 0;

                // Create a thrust entity
                EntityId thrust = ed.createEntity();
                Vector3f thrustVel = accel.mult(-1);
                Vector3f thrustPos = pos.getLocation().add(thrustVel.normalize().multLocal(0.1f));
                ed.setComponents(thrust,
                                 new Position(thrustPos, new Quaternion()),
                                 new Velocity(thrustVel),
                                 new ModelType(PanicModelFactory.MODEL_THRUST),
                                 new Decay(1000));

            } else if( value == 0 ) {
                lastThrustTime = thrustInterval;
            }
        }
    }

    @Override
    public void valueChanged( FunctionId func, InputState value, double tpf ) {

        if( func == ShipFunctions.F_SHOOT && value == InputState.Positive ) {

            // See if the last bullet still exists
            if( lastBullet != null && ed.getComponent(lastBullet, Position.class) != null ) {
                // No new bullet allowed
                return;
            }

            // Create the bullet and orient it properly.
            Position pos = ed.getComponent(ship, Position.class);

            Vector3f bulletVel = new Vector3f(0,1,0);
            pos.getFacing().multLocal(bulletVel);

            // Find the tip of the ship
            Vector3f bulletPos = bulletVel.mult(bulletOffset);
            bulletPos.addLocal(pos.getLocation());

            lastBullet = ed.createEntity();
            ed.setComponents(lastBullet,
                             new Position(bulletPos, pos.getFacing()),
                             new Velocity(bulletVel.multLocal(bulletSpeed)),
                             new ModelType(PanicModelFactory.MODEL_BULLET),
                             new CollisionShape(0.01f),
                             new Decay(bulletDecay));

            // Play the sound effect
            shoot.playInstance();
        } else if( func == ShipFunctions.F_THRUST ) {
            if( value == InputState.Positive ) {
                thrust.play();
            } else {
                thrust.stop();
            }
        }
    }

    protected void integrate( double tpf ) {

        // We only integrate for thrust

        Velocity vel = ed.getComponent(ship, Velocity.class);
        Vector3f linear = vel.getLinear();
        if( accel.lengthSquared() == 0 && linear.lengthSquared() == 0 ) {
            return;
        }

        linear.addLocal( (float)(accel.x * tpf),
                         (float)(accel.y * tpf),
                         (float)(accel.z * tpf) );
        if( linear.lengthSquared() > maxSpeed * maxSpeed ) {
            // Clamp it
            float len = linear.length();
            float scale = maxSpeed/len;
            linear.multLocal(scale);
        } else if( accel.lengthSquared() == 0 ) {
            // Dampen it
            linear.multLocal((float)Math.pow(dampening, tpf));
        }
        ed.setComponent(ship, new Velocity(linear, vel.getAngular()));
    }

    @Override
    public void update( float tpf ) {

        // Use our own tpf calculation in case frame rate is
        // running away making this tpf unstable
        long time = System.nanoTime();
        long delta = time - lastFrame;
        lastFrame = time;
        if( delta == 0 ) {
            return; // no update to perform
        }

        double seconds = delta / 1000000000.0;

        // Clamp frame time to no bigger than a certain amount
        // to prevent physics errors.  A little jitter for slow frames
        // is better than tunneling/ghost objects
        if( seconds > 0.1 ) {
            seconds = 0.1;
        }

        integrate(seconds);
    }

}
