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

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import trap.task.Task;

/**
 *  Static utility methods for creating standard SpatialTaskFactory
 *  implementations.
 *
 *  @author    Paul Speed
 */
public class SpatialTaskFactories {
    
    public static SpatialTaskFactory singleton( Task task ) {
        return new SingletonFactory(task);
    }

    public static SpatialTaskFactory callMethod( Object delegate, String methodName ) {
        return new CallMethodFactory(delegate, methodName);
    }
    
    private static class SingletonFactory implements SpatialTaskFactory {

        private Task task;
    
        public SingletonFactory( Task task ) {
            this.task = task;
        } 

        public Task createTask( Spatial spatial, Vector3f target ) {
            return task;
        }
    }
    
    private static class CallMethodFactory implements SpatialTaskFactory {

        private Object delegate;
        private Method method;
        private Class[] parmTypes;
        
        public CallMethodFactory( Object delegate, String methodName ) {
            Class type;
            if( delegate instanceof Class ) {
                type = (Class)delegate;
            } else {
                type = delegate.getClass();
            }
            
            Method[] methods = type.getMethods();
            for( Method m : methods ) {
                if( !methodName.equals(m.getName()) ) {
                    continue;
                }
                // Check the return type
                if( !Task.class.isAssignableFrom(m.getReturnType()) ) {
                    continue;
                }
                
                parmTypes = m.getParameterTypes();
                
                // We could check them but I'm lazy
                this.method = m;
                break;
            } 
            if( method == null ) {
                throw new RuntimeException("Method not found:" + methodName);
            }
        } 
    
        public Task createTask( Spatial spatial, Vector3f target ) {
        
            Object[] parms;
            if( parmTypes == null || parmTypes.length == 0 ) {
                parms = null;
            } else {
                // Fill them in
                parms = new Object[parmTypes.length];
                for( int i = 0; i < parmTypes.length; i++ ) {
                    if( Spatial.class.isAssignableFrom(parmTypes[i]) ) {
                        parms[i] = spatial;
                    } else if( Vector3f.class.isAssignableFrom(parmTypes[i]) ) {
                        parms[i] = target;
                    } 
                }
            }
            
            try {
                Task result = (Task)method.invoke(delegate, parms);
                return result;
            } catch( IllegalAccessException e ) {
                throw new RuntimeException( "Error invoking factory method", e );    
            } catch( InvocationTargetException e ) {
                throw new RuntimeException( "Error invoking factory method", e );    
            }
        }
    }    
}
