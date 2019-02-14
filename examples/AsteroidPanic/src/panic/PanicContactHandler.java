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

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;


/**
 *  Asteroid Panic-specific contact handler.  Performs simple
 *  contact resolution and checks for game state conditions such
 *  as ship-asteroid collisions and missile-asteroid collisions.
 *  It updates the PanicPlayer object accordingly with either a
 *  score or a death.
 *  The resolveCollision() method is general for any frictionless
 *  contact resolution scheme.
 *
 *  @author    Paul Speed
 */
public class PanicContactHandler implements ContactHandler {

    private static int[] scores = PanicConstants.scores;

    private EntityData ed;
    private PanicPlayer player;

    private float minRadius = PanicConstants.minAsteroidRadius;

    // For now... included here directly
    private AudioNode bump1;
    private AudioNode bump2;
    private AudioNode boom1;
    private AudioNode boom2;

    public PanicContactHandler() {
    }

    public void setPlayer( PanicPlayer player ) {
        this.player = player;
    }

    public void setCollisionState( CollisionState state )
    {
        if( state == null ) {
            this.ed = null;
            return;
        }

        this.ed = state.getApplication().getStateManager().getState(EntityDataState.class).getEntityData();

        AssetManager assets = state.getApplication().getAssetManager();
        bump1 = new AudioNode(assets, "Sounds/low-bang.ogg", DataType.Buffer);
        bump1.setReverbEnabled(false);
        bump2 = new AudioNode(assets, "Sounds/high-bang.ogg", DataType.Buffer);
        bump2.setReverbEnabled(false);
        boom1 = new AudioNode(assets, "Sounds/boom1.ogg", DataType.Buffer);
        boom1.setReverbEnabled(false);
        boom2 = new AudioNode(assets, "Sounds/boom2.ogg", DataType.Buffer);
        boom2.setReverbEnabled(false);
    }

    protected float getInvMass( Entity e ) {
        Mass m = ed.getComponent(e.getId(), Mass.class);
        if( m != null ) {
            return (float)m.getInvMass();
        }
        CollisionShape shape = e.get(CollisionShape.class);
        if( shape != null ) {
            return 1.0f/shape.getRadius();
        }
        return 0;
    }

    protected void resolveCollision( Entity e1, Entity e2, Vector3f cp, Vector3f cn, float penetration )
    {
        Position p1 = e1.get(Position.class);
        Position p2 = e2.get(Position.class);
        float invMass1 = getInvMass(e1);
        float invMass2 = getInvMass(e2);

        if( penetration > 0 ) {
            // Resolve the penetration
            Vector3f np1 = p1.getLocation().subtract(cn.mult(penetration));
            Vector3f np2 = p2.getLocation().add(cn.mult(penetration));
            e1.set(new Position(np1,p1.getFacing()));
            e2.set(new Position(np2,p2.getFacing()));
        }

        Velocity v1 = ed.getComponent(e1.getId(), Velocity.class);
        Vector3f vl1 = v1.getLinear();
        Velocity v2 = ed.getComponent(e2.getId(), Velocity.class);
        Vector3f vl2 = v2.getLinear();

        Vector3f vRel = vl2.subtract(vl1);

        float relNormalVel = vRel.dot(cn);
        if( relNormalVel > 0 ) {
            // Already separating
            return;
        }

        // Calculate the change in velocity and we'll ignore
        // penetration for the moment.
        float restitution = 0.99f;

        float impulse = (-(1+restitution) * relNormalVel)
                        / (invMass1 + invMass2);

        // Apply the impulse to the velocities
        vl1.subtractLocal(cn.mult(impulse * invMass1));
        vl2.addLocal(cn.mult(impulse * invMass2));

        e1.set(new Velocity(vl1, v1.getAngular()));
        e2.set(new Velocity(vl2, v2.getAngular()));
    }

    protected void shipCollision( Entity ship, Entity other, ModelType type, Vector3f cp, Vector3f cn, float penetration )
    {
        Velocity v1 = ed.getComponent(ship.getId(), Velocity.class);
        Vector3f vl1 = v1.getLinear();
        Velocity v2 = ed.getComponent(other.getId(), Velocity.class);
        Vector3f vl2 = v2.getLinear();

        Vector3f vRel = vl1.subtract(vl2);

        float relNormalVel = vRel.dot(cn);

        // If the player is invincible right now then no explosion...
        if( player.isInvincible() )
            return;

        // Could calculate damage based on relNormalVel
        System.out.println( "relNormalVel:" + relNormalVel );

        // Kill the ship
        player.setDead(true);
        ed.removeComponent(ship.getId(), ModelType.class);

        // Create some explosive debris from fake asteroids with no
        // collision shapes
        int debrisCount = (int)((Math.random() * 5) + 5);
        float angleOffset = (float)Math.random();
        for( int i = 0; i < debrisCount; i++ ) {
            EntityId debris = ed.createEntity();
            float angle = angleOffset + ((float)i / debrisCount) * FastMath.TWO_PI;
            float x = FastMath.cos(angle) * 2;
            float y = FastMath.sin(angle) * 2;
            float spin = (float)Math.random() * FastMath.PI * 4 - FastMath.PI * 2;
            ed.setComponents(debris,
                             new Position(cp, new Quaternion()),
                             new Velocity(new Vector3f(x,y,0), new Vector3f(0,0,spin)),
                             new ModelType(PanicModelFactory.MODEL_SHIP_DEBRIS),
                             new Decay(500));
        }

        // Make an explosion sound
        boom1.playInstance();
    }

    protected void bulletCollision( Entity bullet, Entity other, ModelType type, Vector3f cp, Vector3f cn, float penetration )
    {
        // Don't let us shoot ourselves for now
        if( PanicModelFactory.MODEL_SHIP.equals(type.getType()) ) {
            return;
        }

        // Blow it up!

        // First we gather the properties of the asteroid we will be
        // destroying
        Position pos = other.get(Position.class);
        Vector3f loc = pos.getLocation();
        CollisionShape shape = other.get(CollisionShape.class);
        float radius = shape.getRadius();

        // Increase the score
        int size = Math.min((int)(0.6 / radius), scores.length-1);
        player.addScore( scores[size] );

        // If the radius is greater than minRadius then we split it
        if( radius > minRadius ) {

            // Create two new smaller asteroids and destroy the original
            Velocity vel = ed.getComponent(other.getId(), Velocity.class);
            Vector3f v = vel.getLinear();

            // Calculate new positions perpendicular to the impact point
            Vector3f cross = cn.cross(Vector3f.UNIT_Z);
            //System.out.println( "Cross:" + cross );
            float newRadius = radius * 0.5f;
            Vector3f loc1 = loc.add(cross.mult(newRadius));
            Vector3f loc2 = loc.subtract(cross.mult(newRadius));

            float explosiveEnergy = 1;
            float explosiveAngularEnergy = 1;
            Vector3f v1 = v.add(cross.mult(explosiveEnergy));
            v1.addLocal(cn.mult(explosiveEnergy));
            Vector3f v2 = v.subtract(cross.mult(explosiveEnergy));
            v2.addLocal(cn.mult(explosiveEnergy));
            Vector3f a1 = vel.getAngular().add(0, 0, explosiveAngularEnergy);
            Vector3f a2 = vel.getAngular().add(0, 0, -explosiveAngularEnergy);

            // Need to consolidate asteroid entity creation somewhere.
            EntityId asteroid1 = ed.createEntity();
            ed.setComponents(asteroid1,
                            new Position( loc1, new Quaternion() ),
                            new Velocity( v1, a1),
                            new CollisionShape( newRadius ),
                            new Mass( newRadius ),
                            new ModelType( PanicModelFactory.MODEL_ASTEROID ) );
            EntityId asteroid2 = ed.createEntity();
            ed.setComponents(asteroid2,
                            new Position( loc2, new Quaternion() ),
                            new Velocity( v2, a2),
                            new CollisionShape( newRadius ),
                            new Mass( newRadius ),
                            new ModelType( PanicModelFactory.MODEL_ASTEROID ) );
            }

        ed.removeEntity(other.getId());

        // And destroy the bullet... it's job is done
        ed.removeEntity(bullet.getId());

        // Create some explosive debris from fake asteroids with no
        // collision shapes
        int debrisCount = (int)((Math.random() * 5) + 5);
        float angleOffset = (float)Math.random();
        for( int i = 0; i < debrisCount; i++ ) {
            EntityId debris = ed.createEntity();
            float angle = angleOffset + ((float)i / debrisCount) * FastMath.TWO_PI;
            float x = FastMath.cos(angle) * 2;
            float y = FastMath.sin(angle) * 2;
            float spin = (float)Math.random() * FastMath.PI * 4 - FastMath.PI * 2;
            ed.setComponents(debris,
                             new Position(cp, new Quaternion()),
                             new Velocity(new Vector3f(x,y,0), new Vector3f(0,0,spin)),
                             new ModelType(PanicModelFactory.MODEL_DEBRIS),
                             new Decay(500));
        }

        // Make an explosion sound
        boom2.playInstance();
    }

    @Override
    public void handleContact( Entity e1, Entity e2, Vector3f cp, Vector3f cn, float penetration )
    {
        resolveCollision(e1, e2, cp, cn, penetration);

        // Now, if it's a specific kind of collision then we
        // will do more specific things.
        ModelType t1 = ed.getComponent(e1.getId(), ModelType.class);
        ModelType t2 = ed.getComponent(e2.getId(), ModelType.class);
        if( t1 == null || t2 == null )  {
            return;
        }

        if( PanicModelFactory.MODEL_SHIP.equals(t1.getType()) ) {
            shipCollision(e1, e2, t2, cp, cn, penetration);
        } else if( PanicModelFactory.MODEL_SHIP.equals(t2.getType()) ) {
            shipCollision(e2, e1, t1, cp, cn.mult(-1), penetration);
        } else if( PanicModelFactory.MODEL_BULLET.equals(t1.getType()) ) {
            bulletCollision(e1, e2, t2, cp, cn, penetration);
        } else if( PanicModelFactory.MODEL_BULLET.equals(t2.getType()) ) {
            bulletCollision(e2, e1, t1, cp, cn.mult(-1), penetration);
        } else {
            // Assume asteroid to asteroid

            // Check the sizes
            CollisionShape shape1 = e1.get(CollisionShape.class);
            float r1 = shape1 == null ? 0.01f : shape1.getRadius();
            CollisionShape shape2 = e2.get(CollisionShape.class);
            float r2 = shape2 == null ? 0.01f : shape2.getRadius();

            boolean smallImpact = false;
            if( r1 < 0.3 || r2 < 0.3 ) {
                smallImpact = true;
            }
            if( r1 < 0.6 && r2 < 0.6 ) {
                smallImpact = true;
            }

            if( smallImpact ) {
                bump2.playInstance();
            } else {
                bump1.playInstance();
            }
        }
    }
}
