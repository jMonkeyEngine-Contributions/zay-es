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

import com.jme3.animation.AnimControl;
import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import trap.task.Task;

// Kind of like a DSL
import static trap.task.Interpolators.*;
import static trap.task.Tasks.*;
import static trap.anim.AnimInterpolators.*;

/**
 *
 *  @author    Paul Speed
 */
public class AnimationFactories {
 
    private static AssetManager assets;
    
    public static void initialize( AssetManager assets ) {
        AnimationFactories.assets = assets;
    }

    public static Task createMonkeyAttack( Spatial monkey, Vector3f start ) {
 
        start = start.clone();
        Vector3f target = monkey.getLocalRotation().mult(Vector3f.UNIT_Z);
        target.addLocal(start);

        AnimControl anim = monkey.getControl(AnimControl.class);
        SoundControl sounds = monkey.getControl(SoundControl.class);
        
        Task result = sequence(
                        call(sounds, "play", "Walk"),
                        duration(0.25, move(monkey, start, target),
                                       animate(anim, "Walk", 0, 0, 1.55)),
                        call(sounds, "play", "Attack", 0.2),
                        duration(0.5, animate(anim, "Punches", 0, 0, 1)),
                        call(sounds, "play", "Walk"),
                        duration(0.25, move(monkey, target, start),
                                       invert(animate(anim, "Walk", 0, 0, 1.55))),
                        call(sounds, "play", ""),
                        duration(0.1, animate(anim, "Idle", 0, 0, 0.1))
                        );
    
        return result;
    }

    public static Task createMonkeyMiss( Spatial monkey, Vector3f start ) {
 
        start = start.clone();
        Vector3f target = monkey.getLocalRotation().mult(Vector3f.UNIT_Z);
        target.addLocal(start);

        AnimControl anim = monkey.getControl(AnimControl.class);
        SoundControl sounds = monkey.getControl(SoundControl.class);
        
        Task result = sequence(
                        call(sounds, "play", "Walk"),
                        duration(0.25, move(monkey, start, target),
                                       animate(anim, "Walk", 0, 0, 1.55)),
                        call(sounds, "play", "Miss"),
                        duration(0.5, animate(anim, "Punches", 0, 0, 1)),
                        call(sounds, "play", "Walk"),
                        duration(0.25, move(monkey, target, start),
                                       invert(animate(anim, "Walk", 0, 0, 1.55))),
                        call(sounds, "play", ""),
                        duration(0.1, animate(anim, "Idle", 0, 0, 0.1))
                        );
    
        return result;
    }

    public static Task createOgreAttack( Spatial ogre, Vector3f start ) {
 
        start = start.clone();
        Vector3f target = ogre.getLocalRotation().mult(Vector3f.UNIT_Z);
        target.addLocal(start);

        AnimControl anim = ((Node)ogre).getChild(0).getControl(AnimControl.class);
        SoundControl sounds = ogre.getControl(SoundControl.class);
        
        Task result = sequence(
                        call(sounds, "play", "Walk"),
                        duration(0.25, move(ogre, start, target),
                                       animate(anim, "RunTop", 0, 0, 0.2),
                                       animate(anim, "RunBase", 1, 0, 0.2)),
                        call(sounds, "play", "Attack", 0.2),
                        duration(0.5, animate(anim, "SliceVertical", 0, 0, 0.5),
                                      animate(anim, "IdleBase", 1, 0, 1)),
                        call(sounds, "play", "Walk"),
                        duration(0.25, move(ogre, target, start),
                                       invert(animate(anim, "RunTop", 0, 0, 0.2),
                                              animate(anim, "RunBase", 1, 0, 0.2))),
                        call(sounds, "play", ""),
                        duration(0.1, animate(anim, "IdleTop", 0, 0, 0.3),
                                      animate(anim, "IdleBase", 1, 0, 0.3))
                        );
    
        return result;
    }

    public static Task createOgreMiss( Spatial ogre, Vector3f start ) {
 
        start = start.clone();
        Vector3f target = ogre.getLocalRotation().mult(Vector3f.UNIT_Z);
        target.addLocal(start);

        AnimControl anim = ((Node)ogre).getChild(0).getControl(AnimControl.class);
        SoundControl sounds = ogre.getControl(SoundControl.class);
        
        Task result = sequence(
                        call(sounds, "play", "Walk"),
                        duration(0.25, move(ogre, start, target),
                                       animate(anim, "RunTop", 0, 0, 0.2),
                                       animate(anim, "RunBase", 1, 0, 0.2)),
                        call(sounds, "play", "Miss"),
                        duration(0.5, animate(anim, "SliceHorizontal", 0, 0, 0.5),
                                      animate(anim, "IdleBase", 1, 0, 1)),
                        call(sounds, "play", "Walk"),
                        duration(0.25, move(ogre, target, start),
                                       invert(animate(anim, "RunTop", 0, 0, 0.2),
                                              animate(anim, "RunBase", 1, 0, 0.2))),
                        call(sounds, "play", ""),
                        duration(0.1, animate(anim, "IdleTop", 0, 0, 0.3),
                                      animate(anim, "IdleBase", 1, 0, 0.3))
                        );
    
        return result;
    }
    
    public static Task createMonkeyDefend( Spatial monkey, Vector3f start ) {
 
        start = start.clone();
        Vector3f target = monkey.getLocalRotation().mult(Vector3f.UNIT_Z);
        target.multLocal(-0.5f);
        target.addLocal(start);
        
        AnimControl anim = monkey.getControl(AnimControl.class);
        SoundControl sounds = monkey.getControl(SoundControl.class);
        
        Task result = sequence(
                        duration(0.3, animate(anim, "Idle", 0, 0, 0.3)),
                        call(sounds, "play", "Walk"),
                        duration(0.25, move(monkey, start, target),
                                    invert(animate(anim, "Walk", 0, 0, 1.55 * 0.5))),
                        duration(0.25, move(monkey, target, start),
                                    animate(anim, "Walk", 0, 0, 1.55 * 0.5)),
                        call(sounds, "play", ""),
                        duration(0.2, animate(anim, "Idle", 0, 0, 0.2))
                        );
    
        return result;
    }
    
    public static Task createOgreDefend( Spatial ogre, Vector3f start ) {
 
        start = start.clone();
        Vector3f target = ogre.getLocalRotation().mult(Vector3f.UNIT_Z);
        target.multLocal(-0.5f);
        target.addLocal(start);

        AnimControl anim = ((Node)ogre).getChild(0).getControl(AnimControl.class);
        SoundControl sounds = ogre.getControl(SoundControl.class);
        
        Task result = sequence(
                        duration(0.3, animate(anim, "IdleTop", 0, 0, 0.3),
                                       animate(anim, "IdleBase", 1, 0, 0.3)),
                        call(sounds, "play", "Walk"),
                        duration(0.25, move(ogre, start, target),
                                    invert(animate(anim, "RunTop", 0, 0, 0.2 * 0.5),
                                           animate(anim, "RunBase", 1, 0, 0.2 * 0.5))),
                        duration(0.25, move(ogre, target, start),
                                    animate(anim, "RunTop", 0, 0, 0.2 * 0.5),
                                    animate(anim, "RunBase", 1, 0, 0.2 * 0.5)),
                        call(sounds, "play", ""),
                        duration(0.2, animate(anim, "IdleTop", 0, 0, 0.2),
                                       animate(anim, "IdleBase", 1, 0, 0.2))
                        );
    
        return result;
    }
    
    public static Task createMonkeyDeath( Spatial monkey ) {
 
        AnimControl anim = monkey.getControl(AnimControl.class);
 
        //Node test = Effects.playBling( (Node)monkey, -1, ColorRGBA.Red, ColorRGBA.Green );

        // Need a node to put the explosion on since we will
        // remove the monkey during the process
        Node explosion = new Node("Explosion");
        explosion.setLocalTranslation(monkey.getLocalTranslation());
        SoundControl sounds = monkey.getControl(SoundControl.class).clone();
        explosion.addControl(sounds);
        monkey.getParent().attachChild(explosion);                      
        
        Task result = sequence(
                        duration(0.3, animate(anim, "Idle", 0, 0, 0.3)),
                        call(Effects.class, "playGib", explosion, -1, "Textures/monkey-gib.png"),
                        call(sounds, "play", "Death"),
                        call(monkey, "removeFromParent")
                        );
    
        return result;
    }
    
    public static Task createOgreDeath( Spatial ogre ) {
 
        AnimControl anim = ((Node)ogre).getChild(0).getControl(AnimControl.class);

        // Need a node to put the explosion on since we will
        // remove the monkey during the process
        Node explosion = new Node("Explosion");
        SoundControl sounds = ogre.getControl(SoundControl.class).clone();
        explosion.addControl(sounds);
        explosion.setLocalTranslation(ogre.getLocalTranslation());
        ogre.getParent().attachChild(explosion);                      
        
        Task result = sequence(
                        duration(0.3, animate(anim, "IdleTop", 0, 0, 0.3),
                                       animate(anim, "IdleBase", 1, 0, 0.3)),
                        call(Effects.class, "playGib", explosion, -1, "Textures/ogre-gib.png"),
                        call(sounds, "play", "Death"),
                        call(ogre, "removeFromParent")
                        );
    
        return result;
    }
    
    public static Task createBarrelsDeath( Spatial target ) {
 
System.out.println( "createBarrelsDatah()" );    
        // Need a node to put the explosion on since we will
        // remove the monkey during the process
        Node explosion = new Node("Explosion");
        SoundControl sounds = target.getControl(SoundControl.class).clone();
        explosion.addControl(sounds);
        explosion.setLocalTranslation(target.getLocalTranslation());
        target.getParent().attachChild(explosion);
        
        Task result = sequence(
                        call(Effects.class, "playExplosion", explosion, -1, "Textures/barrel-debris.png"),
                        call(sounds, "play", "Death"),
                        call(target, "removeFromParent")
                        );
        
        return result;                      
    }
    
    public static Task createChestDeath( Spatial target ) {
    
System.out.println( "createChestDeath()" );    
        // Need a node to put the explosion on since we will
        // remove the monkey during the process
        Node explosion = new Node("Explosion");
        SoundControl sounds = target.getControl(SoundControl.class).clone();
        explosion.addControl(sounds);
        explosion.setLocalTranslation(target.getLocalTranslation());
        target.getParent().attachChild(explosion);
        
        Task result = sequence(
                        call(Effects.class, "playExplosion", explosion, -1, "Textures/chest-debris.png"),
                        call(sounds, "play", "Death"),
                        call(target, "removeFromParent")
                        );
        
        return result;                      
    }
}
