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

package trap.anim;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.scene.Spatial;
import trap.task.Task;
import trap.task.TaskStatus;


/**
 *
 *  @author    Paul Speed
 */
public class AnimTasks {
 
 /*
    Seems like an interpolator works better in many cases
    and can be curved, reversed, etc....
       
    public static Task playAnimation( double duration, Spatial target, String name, int channel, double speed ) {
        return new PlayAnimationTask(target, name, channel, speed, duration);       
    }
    
    private static class PlayAnimationTask implements Task {
    
        private Spatial target;
        private AnimControl anim;        
        private String name;
        private int channel;
        private double speed;
        private double duration;
        private double time;
        private AnimChannel chan;
                
        public PlayAnimationTask( Spatial target, String name, int channel, 
                                  double speed, double duration ) {
            this.target = target;
            this.name = name;
            this.channel = channel;
            this.speed = speed;
            this.duration = duration;
            
            this.anim = target.getControl(AnimControl.class);
            if( anim == null ) {
                throw new IllegalArgumentException("Target spatial has no AnimControl");
            }
        }

        private AnimChannel getChannel() {
            if( chan != null ) {
                return chan;
            }
            chan = anim.getChannel(channel);
            if( chan != null ) {
                return chan;
            }
            chan = anim.createChannel();
            return chan;
        }

        public TaskStatus execute( double tpf ) {
                            
            time += tpf;
            AnimChannel c = getChannel();
            
            if( time >= duration ) {
                // Stop
                c.reset(false);
                return TaskStatus.Done;
            } else {
                // Keep going
                if( !name.equals(c.getAnimationName()) ) {
                    c.setAnim(name);
                }
                double relativeTime = time % c.getAnimMaxTime();
                c.setTime((float)relativeTime);                
                return TaskStatus.Continue;
            }                      
        }

        public void pausing() {
            AnimChannel c = getChannel();
            c.reset(false);
        }

        public void stopping() {
            AnimChannel c = getChannel();
            c.reset(true);
            time = 0;
        }
        
        @Override
        public String toString() {
            return "PlayAnimationTask[name=" + name + ", channel=" + channel + ", speed=" + speed + "]";
        }
    }*/
    
}
