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

import com.jme3.animation.AnimChannel;
import java.util.*;

import com.jme3.animation.AnimControl;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;


/**
 *  Translates general timed character animation commands
 *  into model specific animations and tracks their time
 *  and resets to idle, etc... basically a logical character
 *  animation control 
 *
 *  @author    Paul Speed
 */
public class CharacterAnimControl extends AbstractControl {

    private AnimControl anim;
    private Map<String, List<Mapping>> mappings = new HashMap<String, List<Mapping>>();
    private String defaultAnimation = "Idle";
    private String animation;
    private double timeLeft;
    
    public CharacterAnimControl( AnimControl anim ) {
        this.anim = anim;
    }

    public void addMapping( String name, String animation, float speed ) {
        getMappings(name, true).add(new Mapping(animation, speed));       
    }

    protected List<Mapping> getMappings( String name, boolean create ) {
        List<Mapping> result = mappings.get(name);
        if( result == null && create ) {
            result = new ArrayList<Mapping>();
            mappings.put(name, result);            
        }
        return result;
    }

    public void setAnimation( String name, double duration ) {
        timeLeft = duration;
        if( Objects.equals(animation, name) ) {
            return;
        }
        
        // Else it's a new animation
        play(name);
    }
    
    protected void play( String name ) {
        List<Mapping> mappings = getMappings(name, false);
        if( mappings == null ) {
            throw new IllegalArgumentException( "No such animation:" + name );
        }
 
        this.animation = name;
        
        anim.clearChannels();
               
        for( int i = 0; i < mappings.size(); i++ ) {
            Mapping m = mappings.get(i);
            AnimChannel channel = anim.createChannel();
            channel.setAnim(m.animation);
            channel.setSpeed(m.speed); 
        }
    }

    @Override
    protected void controlUpdate( float tpf ) {
        if( timeLeft > 0 ) {
            timeLeft -= tpf;
            if( timeLeft <= 0 ) {
                play(defaultAnimation);
            }
        }
    }

    @Override
    protected void controlRender( RenderManager rm, ViewPort vp ) {
    }
    
    private class Mapping {
        String animation;
        float speed;
        
        public Mapping( String animation, float speed ) {
            this.animation = animation;
            this.speed = speed;
        }   
    }
}


