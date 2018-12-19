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
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;


/**
 *  A demo-style model factory that implements AsteroidPanic
 *  visuals as standard JME Box meshes of various colors.
 *  This is not currently used and was just the initial
 *  implementation and is provided for interest.
 *  See RetroPanicModelFactory for the one that is actually
 *  used currently.
 *
 *  @author    Paul Speed
 */
public class PanicModelFactory implements ModelFactory {

    public static final String MODEL_ASTEROID = "asteroid";
    public static final String MODEL_DEBRIS = "debris";
    public static final String MODEL_SHIP_DEBRIS = "shipDebris";
    public static final String MODEL_SHIP = "ship";
    public static final String MODEL_THRUST = "thrust";
    public static final String MODEL_BULLET = "bullet";

    private ModelState state;
    private AssetManager assets;
    private EntityData ed;

    @Override
    public void setState( ModelState state ) {
        this.state = state;
        this.assets = state.getApplication().getAssetManager();
        this.ed = state.getApplication().getStateManager().getState(EntityDataState.class).getEntityData();
    }

    @Override
    public Spatial createModel( Entity e ) {

        ModelType type = e.get(ModelType.class);
        if( MODEL_ASTEROID.equals(type.getType()) ) {
            CollisionShape cs = ed.getComponent(e.getId(), CollisionShape.class);
            float radius = cs == null ? 0.05f : cs.getRadius();

            Box b = new Box(radius, radius, radius);
            Geometry geom = new Geometry("Asteroid", b);

            Material mat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Blue);
            geom.setMaterial(mat);

            return geom;
        } else if( MODEL_DEBRIS.equals(type.getType()) ) {
            float radius = 0.05f;
            Box b = new Box(radius, radius, radius);
            Geometry geom = new Geometry("Debris", b);
            Material mat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Cyan);
            geom.setMaterial(mat);

            return geom;
        } else if( MODEL_SHIP_DEBRIS.equals(type.getType()) ) {
            float radius = 0.05f;
            Box b = new Box(radius, radius, radius);
            Geometry geom = new Geometry("Debris", b);
            Material mat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Cyan);
            geom.setMaterial(mat);

            return geom;
        } else if( MODEL_SHIP.equals(type.getType()) ) {
            CollisionShape cs = ed.getComponent(e.getId(), CollisionShape.class);
            float radius = cs == null ? 0.1f : cs.getRadius();

            Box b = new Box(radius, radius * 1.2f, radius);
            Geometry geom = new Geometry("Ship", b);

            Material mat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Green);
            geom.setMaterial(mat);

            return geom;
        } else if( MODEL_THRUST.equals(type.getType()) ) {
            Box b = new Box(0.01f, 0.01f, 0.01f);
            Geometry geom = new Geometry("Thrust", b);

            Material mat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Red);
            geom.setMaterial(mat);

            return geom;
        } else if( MODEL_BULLET.equals(type.getType()) ) {
            Box b = new Box(0.01f, 0.1f, 0.01f);
            Geometry geom = new Geometry("Bullet", b);

            Material mat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Yellow);
            geom.setMaterial(mat);

            return geom;
        } else {
            Box b = new Box(0.1f, 0.1f, 0.1f);
            Geometry geom = new Geometry("Test", b);

            Material mat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Blue);
            geom.setMaterial(mat);
            return geom;
        }
    }
}
