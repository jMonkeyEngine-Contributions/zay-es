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
import trap.game.TimeProvider;
import com.jme3.animation.AnimChannel;

import com.jme3.animation.AnimControl;
import com.jme3.audio.AudioNode;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *  Translates general timed character animation commands
 *  into model specific animations and tracks their time
 *  and resets to idle, etc... basically a logical character
 *  animation control 
 *
 *  @author    Paul Speed
 */
public class CharacterAnimControl extends AbstractControl {
 
    private TimeProvider time;
    private AnimControl anim;
    private Map<String, List<Mapping>> mappings = new HashMap<String, List<Mapping>>();
    private Map<String, SoundMapping> sounds = new HashMap<String, SoundMapping>();
    private String defaultAnimation = "Idle";
    private String animation;
    private AnimationTime current;
    private SoundMapping currentSound;
 
    public CharacterAnimControl( TimeProvider time, AnimControl anim ) {
        this.time = time;
        this.anim = anim;
    }

    public void addMapping( String name, String animation, float speed ) {
        getMappings(name, true).add(new Mapping(animation, speed));       
    }
    
    public void addMapping( String name, AudioNode node ) {
        addMapping(name, node, 0);
    }

    public void addMapping( String name, AudioNode node, float delay ) {
        sounds.put(name, new SoundMapping(node, delay));
    }

    protected List<Mapping> getMappings( String name, boolean create ) {
        List<Mapping> result = mappings.get(name);
        if( result == null && create ) {
            result = new ArrayList<Mapping>();
            mappings.put(name, result);            
        }
        return result;
    }

    public void setAnimation( String name, long startTime, long endTime ) {
        this.current = new AnimationTime(name, startTime, endTime);
    }

    public void reset() {
    
        boolean isDefault = true;
        // See if the default animation is the one currently playing
        List<Mapping> mappings = getMappings(defaultAnimation, false);
        for( int i = 0; i < mappings.size(); i++ ) {
            Mapping m = mappings.get(i);
            AnimChannel channel = anim.getChannel(i);
            if( channel == null ) {
                isDefault = false;
                break;
            }
            if( !m.animation.equals(channel.getAnimationName()) ) {
                isDefault = false;
                break;
            }
        }
        if( !isDefault ) {
            play(defaultAnimation);
        }            
    }
    
    protected void play( String name ) {
        List<Mapping> mappings = getMappings(name, false);
        if( mappings == null ) {
            throw new IllegalArgumentException( "No such animation:" + name );
        }
 
        this.animation = name;
        
        anim.clearChannels();
               
        if( currentSound != null ) {
            currentSound.reset();
            currentSound.sound.removeFromParent();
        }
        currentSound = sounds.get(name);
        if( currentSound != null ) {
            currentSound.reset(); // just in case
            ((Node)spatial).attachChild(currentSound.sound);
        }                
               
        for( int i = 0; i < mappings.size(); i++ ) {
            Mapping m = mappings.get(i);
            AnimChannel channel = anim.createChannel();
            channel.setAnim(m.animation);
            channel.setSpeed(m.speed); 
        }
    }

    @Override
    protected void controlUpdate( float tpf ) {
        if( current != null ) {
            long now = time.getTime();
            if( now > current.endTime ) {
                current = null;
                play(defaultAnimation);    
            } else if( now >= current.startTime && !Objects.equal(animation, current.animation) ) {
                play(current.animation);
            }
        }
        if( currentSound != null ) {
            currentSound.update(tpf);
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

    private class SoundMapping {
        AudioNode sound;
        AudioControl control;
        boolean started;
        float delay;
        float time;
        
        public SoundMapping( AudioNode sound, float delay ) {
            this.sound = sound;
            this.delay = delay;
            this.control = sound.getControl(AudioControl.class);
        }
        
        public void reset() {
            time = 0;
            if( control != null ) {
                control.stop();
            } else {
                sound.stop();
            }
            started = false;
        }
        
        public void update( float tpf ) {
            time += tpf;
            if( !started && time > delay ) {
                if( control != null ) {
                    control.play();
                } else {
                    sound.play();
                }
                    
                started = true;
            } 
        }
    }
    
    private class AnimationTime {
        String animation;
        long startTime;
        long endTime;
        
        public AnimationTime( String animation, long startTime, long endTime ) {
            this.animation = animation;
            this.startTime = startTime;
            this.endTime = endTime;
        }
        
        @Override
        public String toString() {
            return "AnimationTime[" + animation + ", " + startTime + ", " + endTime + "]";
        }
    }
}


