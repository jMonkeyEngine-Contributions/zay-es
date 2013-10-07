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

import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource.Status;
import com.jme3.audio.Listener;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import java.util.HashMap;
import java.util.Map;


/**
 *  Attached to a node to allow playing from a catalog
 *  of mapped sounds, including cutoff and attentuation
 *  based on position.
 *
 *  @author    Paul Speed
 */
public class SoundControl extends AbstractControl {

    private Map<String, AudioNode> sounds = new HashMap<String, AudioNode>();
    private Listener listener;
    private float cutOff = 12;  // 6 blocks
    private float cutOffSq = cutOff * cutOff;

    private AudioNode current;
    private float duration;
    private float time;
    private float baseVolume;
    private boolean stopped;  

    public SoundControl( Listener listener ) {
        this.listener = listener;
    }
    
    public void setSpatial( Spatial s ) {
        super.setSpatial(s);
    }
 
    public void addSound( String name, AudioNode sound ) {
        sounds.put(name, sound);
    }
 
    public void play( String sound ) {
        playSound(sounds.get(sound));
    }
 
    public void play() {
        stopped = false;
        time = 0;
    }
    
    public void stop() {
        stopped = true;
        if( current != null ) {
            current.stop();
        }
    }

    protected void playSound( AudioNode audio ) {
        if( current != null && current.equals(audio) ) {
            // So the sound is the same but maybe we should
            // still let it through.
            if( current.isLooping() || time < duration ) {
                // Still playing from last time
                return;
            } 
        }
        
        if( current != null ) {
            current.stop();
            current.removeFromParent();
        }
        
        this.current = audio != null ? audio.clone() : null;
        
        if( current != null ) {
            ((Node)spatial).attachChild(current);
            duration = current.getAudioData().getDuration();         
            baseVolume = current.getVolume();
            time = 0;
            stopped = false;
        }
    }

    private float getListenerDistSq() {
        Vector3f v1 = spatial.getWorldTranslation();
        Vector3f v2 = listener.getLocation();
        float x = v1.x - v2.x;  
        float z = v1.z - v2.z;  
        float distSq = x * x + z * z;
        return distSq; 
    }

    @Override
    protected void controlUpdate( float tpf ) {
        if( current == null ) {
            return;
        }
        
        time += tpf;
        
        float distSq = getListenerDistSq();
        boolean audible = distSq < cutOffSq;        
        if( !stopped && audible && current.getStatus() == Status.Stopped ) {
            if( current.isLooping() || time < duration ) {
                // Need to play it... but have to do it at the right time
                float seek = time % duration;
                current.setTimeOffset(seek);
                current.play();
            }
        } else if( !audible && current.getStatus() != Status.Stopped ) {
            // We stop the sound but we'll keep accumulating time so
            // that we can start it in the right place when we are
            // audible again.
            current.stop();
        }
        
        // Set the volume if we are audible
        if( audible ) {
            float dist = FastMath.sqrt(distSq);
            float volume = Math.max(0, 1 - dist/cutOff);
            current.setVolume(volume * baseVolume);            
        }   
    }

    @Override
    protected void controlRender( RenderManager rm, ViewPort vp ) {
    }
}
