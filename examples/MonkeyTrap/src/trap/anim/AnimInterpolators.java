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
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import trap.task.Interpolator;


/**
 *
 *  @author    Paul Speed
 */
public class AnimInterpolators {

    private AnimInterpolators() {
    }

    public static Interpolator move(Spatial s, Vector3f start, Vector3f end ) {
        return new MoveSpatial(s, start, end);
    }

    public static Interpolator rotate( Spatial s, Quaternion start, Quaternion end ) {
        return new RotateSpatial(s, start, end);
    }
     
    public static Interpolator scale( Spatial s, Vector3f start, Vector3f end ) {
        return new ScaleSpatial(s, start, end);
    }
     
    public static Interpolator move( Camera s, Vector3f start, Vector3f end ) {
        return new MoveCamera(s, start, end);
    }
     
    public static Interpolator rotate( Camera s, Quaternion start, Quaternion end ) {
        return new RotateCamera(s, start, end);
    }

    public static Interpolator mix( ColorRGBA target, ColorRGBA start, ColorRGBA end ) {    
        return new LerpColor(target, start, end);
    }
 
    public static Interpolator animate( Spatial target, String animation, int channel, double start, double end ) {
        return new Animate(target.getControl(AnimControl.class), animation, channel, start, end);
    }

    public static Interpolator animate( AnimControl target, String animation, int channel, double start, double end ) {
        return new Animate(target, animation, channel, start, end);
    }

    private static class Animate implements Interpolator {
        private AnimControl target;
        private String animation; 
        private int channel; 
        private double start;
        private double end;
        private AnimChannel chan;
                
        public Animate( AnimControl target, String animation, 
                        int channel, double start, double end ) {
            this.target = target;
            this.animation = animation;
            this.channel = channel;
            this.start = start;
            this.end = end;                
        }

        public void interpolate( double t ) {
            if( t == 0 || chan == null ) {
                chan = target.getChannel(channel);
                if( chan == null ) {
                    chan = target.createChannel();
                } 
            }
 
            // Set the animation we are supposed to be playing if
            // we haven't already
            if( !animation.equals(chan.getAnimationName()) ) {
                chan.setAnim(animation);
                if( end == 0 ) {
                    end = chan.getAnimMaxTime(); 
                }
            }
            
            // Calculate where in the animation we are supposed to be
            double interp = start + (end - start) * t;
            double relative = interp % chan.getAnimMaxTime();
            chan.setTime((float)relative);           
        }
 
        @Override
        public String toString() {
            return "Animate[animation=" + animation + ", channel=" + channel
                        + ", start=" + start + ", end=" + end + ", target=" + target + "]";
        }       
    }
    
    private static abstract class LerpVector3f implements Interpolator {

        private Vector3f start;
        private Vector3f end;
        private Vector3f last;
 
        public LerpVector3f( Vector3f start, Vector3f end ) {
            this.start = start;
            this.end = end;
            this.last = new Vector3f();
        }       

        protected Vector3f getStart() {
            return start;
        }
        
        protected Vector3f getEnd() {
            return end;
        }

        protected abstract Vector3f loadStart();
        protected abstract void apply( Vector3f value );       

        public void interpolate( double t ) {
            if( start == null )
                start = loadStart();
            last.x = (float)(start.x + (end.x - start.x) * t); 
            last.y = (float)(start.y + (end.y - start.y) * t); 
            last.z = (float)(start.z + (end.z - start.z) * t);
            
            apply(last);
        }
    }

    private static class MoveSpatial extends LerpVector3f {
        
        private Spatial target;
        
        public MoveSpatial( Spatial target, Vector3f start, Vector3f end ) {
            super( start, end );
            this.target = target;            
        }
         
        protected Vector3f loadStart() {
            return target.getLocalTranslation().clone();
        }
        
        protected void apply( Vector3f value ) {
            target.setLocalTranslation(value);
        }
               
        @Override
        public String toString() {
            return "MoveSpatial[spatial=" + target + ", start=" + getStart() 
                                + ", end=" + getEnd() + "]";
        }       
    }

    private static class ScaleSpatial extends LerpVector3f {
        
        private Spatial target;
        
        public ScaleSpatial( Spatial target, Vector3f start, Vector3f end ) {
            super( start, end );
            this.target = target;            
        }
         
        protected Vector3f loadStart() {
            return target.getLocalScale().clone();
        }
        
        protected void apply( Vector3f value ) {
            target.setLocalScale(value);
        }       
               
        @Override
        public String toString() {
            return "ScaleSpatial[spatial=" + target + ", start=" + getStart() 
                                + ", end=" + getEnd() + "]";
        }       
    }

    private static class MoveCamera extends LerpVector3f {
        
        private Camera target;
        
        public MoveCamera( Camera target, Vector3f start, Vector3f end ) {
            super( start, end );
            this.target = target;            
        }
         
        protected Vector3f loadStart() {
            return target.getLocation().clone();
        }
        
        protected void apply( Vector3f value ) {
            target.setLocation(value);
        }       
               
        @Override
        public String toString() {
            return "MoveCamera[camera=" + target + ", start=" + getStart() 
                                + ", end=" + getEnd() + "]";
        }       
    }
 
    
    private static abstract class Slerp implements Interpolator {

        private Quaternion start;
        private Quaternion end;
        private Quaternion last;
 
        public Slerp( Quaternion start, Quaternion end ) {
            this.start = start;
            this.end = end;
            this.last = new Quaternion();
        }       

        protected Quaternion getStart() {
            return start;
        }
        
        protected Quaternion getEnd() {
            return end;
        }

        protected abstract Quaternion loadStart();
        protected abstract void apply( Quaternion value );       

        public void interpolate( double t ) {
            if( start == null ) {
                start = loadStart();
            }
 
            last.slerp(start, end, (float)t);
            apply(last);           
        }
    }
    
    private static class RotateSpatial extends Slerp {
    
        private Spatial target;
        
        public RotateSpatial( Spatial target, Quaternion start, Quaternion end ) {
            super( start, end );
            this.target = target;
        }
        
        protected Quaternion loadStart() {
            return target.getLocalRotation().clone();
        }
        
        protected void apply( Quaternion value ) {
            target.setLocalRotation(value);    
        }       
               
        @Override
        public String toString() {
            return "RotateSpatial[spatial=" + target + ", start=" + getStart() 
                                + ", end=" + getEnd() + "]";
        }       
    }    

    private static class RotateCamera extends Slerp {
    
        private Camera target;
        
        public RotateCamera( Camera target, Quaternion start, Quaternion end ) {
            super( start, end );
            this.target = target;
        }
        
        protected Quaternion loadStart() {
            return target.getRotation().clone();
        }
        
        protected void apply( Quaternion value ) {
            target.setRotation(value);    
        }       
               
        @Override
        public String toString() {
            return "RotateCamera[camera=" + target + ", start=" + getStart() 
                                + ", end=" + getEnd() + "]";
        }       
    }    
 
    private static class LerpColor implements Interpolator {

        private ColorRGBA target;
        private ColorRGBA start;
        private ColorRGBA end;
        private ColorRGBA last;
 
        public LerpColor( ColorRGBA target, ColorRGBA start, ColorRGBA end ) {
            this.target = target;
            this.start = start;
            this.end = end;
            this.last = new ColorRGBA();
        }
        
        public void interpolate(double t) {
            if( start == null ) {
                start = target.clone();
            }
            
            last.r = (float)(start.r + (end.r - start.r) * t); 
            last.g = (float)(start.g + (end.g - start.g) * t); 
            last.b = (float)(start.b + (end.b - start.b) * t);
            last.a = (float)(start.a + (end.a - start.a) * t);
            
            target.set(last);
        }
               
        @Override
        public String toString() {
            return "MixColor[target=" + target + ", start=" + start 
                                + ", end=" + end + "]";
        }       
    }
}

