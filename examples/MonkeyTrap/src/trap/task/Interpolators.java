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

package trap.task;

import com.jme3.math.FastMath;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;


/**
 *  Factory methods for common Interpolator implementations.
 *
 *  @author    Paul Speed
 */
public class Interpolators {

    /**
     *  Composes a list of interpolators together such that they
     *  run in parallel.
     */
    public static Interpolator compose( Interpolator... list ) {
        return new Composite(list);
    }
    
    /**
     *  Composes a list of interpolators together such that they
     *  run in parallel.
     */
    public static Interpolator compose( List<Interpolator> list ) {
        return new Composite(list.toArray(new Interpolator[list.size()]));
    }

    /**
     *  Stretches the interpolation period over an entire list of
     *  Interpolator children.  For example, a two child list will
     *  provide the first child 0-1 for an input of 0-0.5 and the
     *  second child 0-1 for an input of 0.5 to 1.
     */
    public static Interpolator range( Interpolator... list ) {
        return new Range(list);
    }

    /**
     *  Rescales a delegate interpolator to operate in a different range of
     *  of inputs.  Input of low to high is scaled into 0 to 1 for the delegate
     *  interpolator.
     */
    public static Interpolator rescale( Interpolator i, double low, double high ) {
        return new Rescale(i, low, high);
    }

    /**
     *  Converts 0 to 1 input into a 0 to 1 sine wave before passing it
     *  onto the delegate.
     */
    public static Interpolator sineCurve( Interpolator i ) {
        return new SineCurve(i);
    }

    /**
     *  Converts 0 to 1 input into a 0 to 1 smooth-stepped Hermite interpolation before 
     *  passing it onto the delegate.  Essentially:  t * t * (3.0 - 2.0 * t)       
     */
    public static Interpolator smoothStep( Interpolator i ) {
        return new SmoothStep(i);
    }
 
    /**
     *  Inverts the input passed to a delegate interpolator so that 0-1 input
     *  becomes 1-0 passed to the delegate.
     */
    public static Interpolator invert( Interpolator i ) {
        return new Invert(i);
    }

    /**
     *  Inverts the input passed to a delegate interpolator so that 0-1 input
     *  becomes 1-0 passed to the delegate.
     */
    public static Interpolator invert( Interpolator... i ) {
        return new Invert(compose(i));
    }
 
    /**
     *  Calls a delegate method over the range min and max based on input between 0 and 1.
     *  For example, if min is 0 and max is ten then calling the interpolator with an input
     *  of 0.5 will call the delegate method with the value of 5.  The specified method
     *  must be a single argument method that takes a double parameter.
     */
    public static Interpolator callMethod( Object delegate, String method, 
                                           double min, double max ) {
        return new CallMethod( delegate, method, min, max );
    }
 
    private static class CallMethod implements Interpolator {
    
        private double min;
        private double max;
        private Object delegate;
        private Method method;
        
        public CallMethod( Object delegate, String method, double min, double max ) {
            this.delegate = delegate;
            this.min = min;
            this.max = max;
 
            try {           
                this.method = delegate.getClass().getMethod(method, Double.TYPE);
            } catch( NoSuchMethodException e ) {
                throw new RuntimeException("Error finding method:" + method + " on:" + delegate, e);
            } 
        }
         
        public void interpolate( double t ) {
            double value = min + (max - min) * t;
            try {
                method.invoke(delegate, value);
            } catch( IllegalAccessException e ) {
                throw new RuntimeException("Error calling:" + method, e);                
            } catch( InvocationTargetException e ) {
                throw new RuntimeException("Error calling:" + method, e);                
            }
        }
        
        @Override
        public String toString() {
            return "CallMethod[delegate=" + delegate 
                        + ", method=" + method 
                        + ", min=" + min 
                        + ", max=" + max + "]";  
        }
    }    
 
    private static class SineCurve implements Interpolator {
    
        private Interpolator delegate;
        
        public SineCurve( Interpolator delegate ) {
            this.delegate = delegate;
        }
         
        public void interpolate( double t ) {
            // 0 to 1 in a nice sin curve is really
            // -PI/2 to PI/2 and is +/- 1.
            double rads = -FastMath.HALF_PI + (t * FastMath.PI);
            double s = Math.sin(rads);
            s = (s + 1) * 0.5;
            delegate.interpolate(s);
        }
        
        @Override
        public String toString() {
            return "SineCurve[delegate=" + delegate + "]";  
        }
    }

    private static class SmoothStep implements Interpolator {
    
        private Interpolator delegate;
        
        public SmoothStep( Interpolator delegate ) {
            this.delegate = delegate;
        }
         
        public void interpolate( double t ) {
            t = t * t * (3.0 - 2.0 * t);       
            delegate.interpolate(t);
        }
        
        @Override
        public String toString() {
            return "SmoothStep[delegate=" + delegate + "]";  
        }
    }

    private static class Invert implements Interpolator {
    
        private Interpolator delegate;
        
        public Invert( Interpolator delegate ) {
            this.delegate = delegate;
        }
         
        public void interpolate( double t ) {
            delegate.interpolate(1.0 - t);
        }
        
        @Override
        public String toString() {
            return "Invert[delegate=" + delegate + "]";  
        }
    }

    private static class Rescale implements Interpolator {
    
        private Interpolator delegate;
        private double low;
        private double high;
        private double delta;
        
        public Rescale( Interpolator delegate, double low, double high ) {
            this.delegate = delegate;
            this.low = low;
            this.high = high;
            this.delta = high - low;
        }
         
        public void interpolate( double t ) {
            t = low + t * delta;
            delegate.interpolate(t);
        }
        
        @Override
        public String toString() {
            return "Rescale[delegate=" + delegate + ", low=" + low + ", high=" + high + "]";  
        }
    }
 
    private static class Composite implements Interpolator {
    
        private Interpolator[] list;
        
        public Composite( Interpolator... list ) {
            this.list = list;
        }
        
        public void interpolate( double t ) {
            for( Interpolator i : list ) {
                i.interpolate(t);
            }
        }
                 
        @Override
        public String toString() {
            return "Composite[delegates=" + Arrays.asList(list) + "]";  
        }
    }

    private static class Range implements Interpolator {
    
        private Interpolator[] list;
        private double[] lastValues;
        
        public Range( Interpolator... list ) {
            this.list = list;
            this.lastValues = new double[list.length];
        }
        
        public void interpolate( double t ) {
        
            // Calculate which interpolator we are targeting
            // and what the proper sub-value should be. 
            double delta = 1.0 / list.length;
            int index = Math.min( (int)(t / delta), list.length-1);
            double base = delta * index;
            double part = (t - base) / delta;

            // Make sure all elements are properly set
            for( int i = 0; i < list.length; i++ ) {
                if( list[i] == null ) {
                    continue;
                }                
                if( i < index ) {
                    if( lastValues[i] != 1 ) {
                        lastValues[i] = 1;
                        list[i].interpolate(1);
                    }
                } else if( i == index ) {
                    lastValues[i] = part;
                    list[i].interpolate(part);
                } else {
                    if( lastValues[i] != 0 ) {
                        lastValues[i] = 0;
                        list[i].interpolate(0);
                    }
                }
            } 
        }
                 
        @Override
        public String toString() {
            return "Range[delegates=" + Arrays.asList(list) + "]";  
        }
    }
 
}


