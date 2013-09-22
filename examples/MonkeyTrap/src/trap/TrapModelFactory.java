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

import trap.game.MonkeyTrapConstants;
import trap.game.ModelType;
import trap.game.TimeProvider;
import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.bounding.BoundingBox;
import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.simsilica.es.Entity;


/**
 *
 *  @author    Paul Speed
 */
public class TrapModelFactory implements ModelFactory
{
    public static final ModelType TYPE_MONKEY = new ModelType("Monkey");
    public static final ModelType TYPE_OGRE = new ModelType("Ogre");

    private ModelState state;

    public void setState(ModelState state) {
        this.state = state;
    }

    protected Geometry createShadowBox( float xExtent, float yExtent, float zExtent ) {
        Box box = new Box(xExtent, yExtent, zExtent);
        Geometry shadowBox = new Geometry("shadowBounds", box) {
                    @Override
                    public int collideWith( Collidable other, CollisionResults results ) {
                        return 0;
                    }
                };
        shadowBox.move(0,yExtent,0);
            
        Material m = new Material(state.getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        m.setFloat("AlphaDiscardThreshold", 1.1f);  // Don't render it at all
        shadowBox.setMaterial(m);        
        shadowBox.setShadowMode( ShadowMode.Cast );
        return shadowBox;
    }

    public Spatial createModel(Entity e) {

        ModelType type = e.get(ModelType.class);
 
        TimeProvider time = state.getState(PlayerState.class).getClient().getRenderTimeProvider();
               
        if( TYPE_MONKEY.equals(type) ) {
            Node monkey = (Node)state.getApplication().getAssetManager().loadModel( "Models/Jaime/Jaime.j3o" );
            AnimControl anim = monkey.getControl(AnimControl.class);
            AnimChannel channel = anim.createChannel();
            channel.setAnim("Idle");
 
            // The monkey's shadow box is strangley off center so we
            // adjust it... it's also wide because of the splayed arms
            // and we can fix that too
            BoundingBox bounds = (BoundingBox)monkey.getWorldBound();
            monkey.attachChild(createShadowBox(bounds.getXExtent() * 0.7f, 
                                                bounds.getYExtent(), 
                                                bounds.getZExtent()));
                                                                                                
            monkey.addControl(new InterpolationControl(time));
            
            CharacterAnimControl cac = new CharacterAnimControl(time, anim);
            cac.addMapping("Idle", "Idle", 1);
            cac.addMapping("Walk", "Walk", 1.55f * (float)MonkeyTrapConstants.MONKEY_SPEED); 
            monkey.addControl(cac);
            return monkey;
        } else if( TYPE_OGRE.equals(type) ) {
            Spatial ogre = state.getApplication().getAssetManager().loadModel( "Models/Sinbad/Sinbad.mesh.j3o" );
            
            // Normalize the ogre to be 1.8 meters tall
            BoundingBox bounds = (BoundingBox)ogre.getWorldBound();                 
            ogre.setLocalScale( 1.8f / (bounds.getYExtent() * 2) );
            bounds = (BoundingBox)ogre.getWorldBound();
            ogre.setLocalTranslation(0, bounds.getYExtent() - bounds.getCenter().y, 0);
 
            AnimControl anim = ogre.getControl(AnimControl.class);
            AnimChannel channel = anim.createChannel();
            channel.setAnim("IdleTop");
            channel = anim.createChannel();
            channel.setAnim("IdleBase");
            
            // Wrap it in a node to keep its local translation adjustment
            Node wrapper = new Node("Ogre");
            wrapper.attachChild(ogre);
 
            // Because Sinbad is made up of lots of objects and the 
            // zExtent is fairly thin, his shadow looks strange so we
            // will tweak it.
            wrapper.attachChild(createShadowBox(bounds.getXExtent(), 
                                                bounds.getYExtent(), 
                                                bounds.getZExtent() * 1.5f));
            
            //wrapper.setShadowMode( ShadowMode.Cast );
            wrapper.addControl(new InterpolationControl(time));
                        
            CharacterAnimControl cac = new CharacterAnimControl(time, anim);
            cac.addMapping("Idle", "IdleTop", 1);
            cac.addMapping("Idle", "IdleBase", 1);
            cac.addMapping("Walk", "RunTop", 0.2f * (float)MonkeyTrapConstants.OGRE_SPEED);
            cac.addMapping("Walk", "RunBase", 0.2f * (float)MonkeyTrapConstants.OGRE_SPEED);
            wrapper.addControl(cac);
            return wrapper;         
        } 
        
        return null;
    }
}
