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

import com.google.common.base.Objects;
import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  Translates general character animation names into model specific 
 *  animations and resets to idle, etc... basically 
 *  a logical character animation control where you don't have to worry
 *  about channels, speed, etc. to call it.
 *  This is generally just for looping animations as sequences of
 *  one-shot animations will be handled a different way. 
 *
 *  @author    Paul Speed
 */
public class CharAnimControl extends AbstractControl {

    private AnimControl anim;
    private Map<String, List<Mapping>> mappings = new HashMap<String, List<Mapping>>();    
    private String defaultAnimation = "Idle";
    
    private String animation;

    public CharAnimControl( AnimControl anim ) {
        this.anim = anim;
    }

    public void addMapping( String name, String animation, float speed ) {
        getMappings(name, true).add(new Mapping(animation, speed));       
    }
 
    protected boolean isDefaultAnimationRunning() {
        // See if the default animation is the one currently playing
        List<Mapping> mappings = getMappings(defaultAnimation, false);
        if( mappings ==  null ) {
            return false;  // no real way to tell
        }
        
        if( mappings.size() < anim.getNumChannels() ) {
            return false;
        }
         
        for( int i = 0; i < mappings.size(); i++ ) {
            Mapping m = mappings.get(i);
            AnimChannel channel = anim.getChannel(i);
            if( channel == null ) {
                return false;
            }
            if( !m.animation.equals(channel.getAnimationName()) ) {
                return false;
            }
        }
        return true;
    }
 
    /**
     *  Resets to the default animation if it is not already playing.
     *  A more advanced detection is done in this case to determine if
     *  the default animation is or isn't playing in case it was set
     *  externally to this control.
     */
    public void reset() {    
        if( !isDefaultAnimationRunning() ) {
            // Force a change even if we think the default animation
            // is running.
            animation = null;
        }               
        play(defaultAnimation);
    }     
 
    /**
     *  Plays the specified animation or does nothing if the
     *  animation is not mapped.
     */
    public void play( String animation ) {
        if( Objects.equal(this.animation, animation) ) {
            return;
        }
        
        List<Mapping> list = getMappings(animation, false);
        if( list == null ) {
            list = getMappings(defaultAnimation, false);
            if( list == null ) {
                // Just clear everything and hope for the best
                anim.clearChannels();
                return;
            }
        }
        
        // Make sure there are enough channels
        while( anim.getNumChannels() < list.size() ) {
            anim.createChannel();
        }
        
        // Set the channels to the appropriate animation
        int i;
        for( i = 0; i < list.size(); i++ ) {
            Mapping m = list.get(i);
            AnimChannel channel = anim.getChannel(i);
            channel.setAnim(m.animation);
            channel.setSpeed(m.speed); 
        }
        
        // Reset the rest
        for( ; i < anim.getNumChannels(); i++ ) {
            AnimChannel channel = anim.getChannel(i);
            channel.reset(true);   
        }
    }
 
    /**
     *  Stops the current animation if it is not the default animation
     *  and then switches to the default animation.
     */   
    public void stop() {
        play(defaultAnimation);
    }
    
    private List<Mapping> getMappings( String name, boolean create ) {
        List<Mapping> result = mappings.get(name);
        if( result == null && create ) {
            result = new ArrayList<Mapping>();
            mappings.put(name, result);            
        }
        return result;
    }

    @Override
    protected void controlUpdate( float f ) {
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
