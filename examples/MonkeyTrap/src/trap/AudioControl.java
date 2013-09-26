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
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;


/**
 *  Attached to a positional audio node to provide clamping.
 *
 *  @author    Paul Speed
 */
public class AudioControl extends AbstractControl {

    private AudioNode node;
    private Listener listener;
    private float duration;
    private float current;
    private float cutOff = 12;  // 6 blocks
    private float cutOffSq = cutOff * cutOff;
    private float baseVolume;  

    public AudioControl( Listener listener ) {
        this.listener = listener;
    }
    
    public void setSpatial( Spatial s ) {
        super.setSpatial(s);
        node = (AudioNode)s;
System.out.println( "AudioControl.setSpatial(" + s + ")" );
        duration = node.getAudioData().getDuration();         
System.out.println( "  duration:" + duration );
        baseVolume = node.getVolume();
    }

    @Override
    protected void controlUpdate( float tpf ) {
        //System.out.println( node.getWorldTranslation() + "   heard from:" + listener.getLocation() );
        current += tpf;

        Vector3f v1 = node.getWorldTranslation();
        node.setLocalTranslation(node.getLocalTranslation());
        //Vector3f v1 = node.getLocalTranslation();
        Vector3f v2 = listener.getLocation();
        float x = v1.x - v2.x;  
        float z = v1.z - v2.z;  
        float distSq = x * x + z * z; 
        boolean audible = distSq < cutOffSq;
//System.out.println( "distSq:" + distSq );        
        if( audible && node.getStatus() == Status.Stopped ) {        
            // Need to play it... but have to do it at the right time
            float seek = current % duration;
System.out.println( "Playing at:" + seek );            
            node.setTimeOffset(seek);
            node.play();
        } else if( !audible && node.getStatus() != Status.Stopped ) {
System.out.println( "Stopping." );        
            node.stop();
        }
          
        if( audible ) {
            float dist = FastMath.sqrt(distSq);
            float volume = Math.max(0, 1 - dist/cutOff);
            node.setVolume(volume * baseVolume);
        }
    }

    @Override
    protected void controlRender( RenderManager rm, ViewPort vp ) {
    }
}
