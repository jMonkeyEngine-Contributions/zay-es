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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;


/**
 *  Factory methods for common Task implementations.
 *
 *  @author    Paul Speed
 */
public class Tasks {

    /**
     *  Creates a task that will stretch one or more interpolators over
     *  a specified duration.  All delegate interpolators get the full 
     *  range of 0 to 1 as time goes from 0 to duration.
     */
    public static DurationTask duration( double duration, Interpolator... interps ) {
        return new DurationTask(duration, Interpolators.compose(interps));
    }

    /**
     *  Creates a task that will stretch one or more interpolators over
     *  a specified duration.  All delegate interpolators get the full 
     *  range of 0 to 1 as time goes from 0 to duration.
     */
    public static DurationTask duration( double duration, List<Interpolator> interps ) {
        return new DurationTask(duration, Interpolators.compose(interps));
    }

    /**
     *  Creates a task that will execute a set of delegate tasks in parallel.
     */    
    public static Task compose( Task... tasks ) {
        return new ComposeTask(tasks);
    }

    /**
     *  Creates a task that will execute a set of delegate tasks in parallel.
     */    
    public static Task compose( List<Task> tasks ) {
        Task[] array = new Task[tasks.size()];
        array = tasks.toArray(array);
        return new ComposeTask(array);
    }

    /**
     *  Creates a task that will execute a set of delegate tasks in sequence.
     */    
    public static Task sequence( Task... tasks ) {
        return new SequenceTask(tasks);
    }
    
    /**
     *  Creates a task that will execute a set of delegate tasks in sequence.
     */    
    public static Task sequence( List<Task> tasks ) {
        Task[] array = new Task[tasks.size()];
        array = tasks.toArray(array);
        return new SequenceTask(array);
    }

    /**
     *  Creates a task that, when invoked, will call the specified no-arg method 
     *  on the specified delegate object.
     */
    public static Task call( Object obj, String methodName ) {
        return new CallTask( obj, methodName );
    }

    /**
     *  Creates a task that, when invoked, will call the specified method 
     *  on the specified delegate object and parameters.
     */
    public static Task call( Object obj, String methodName, Object... parameters ) {
        return new CallTask( obj, methodName, parameters );
    }

    private static class SequenceTask implements Task {
    
        private Task[] tasks;
        private int current;
        private double time;
        private Double duration;
        
        public SequenceTask( Task[] tasks ) {
            this.tasks = tasks;
        }

        public double getDuration() {
            if( duration == null ) {
                // Calculate the duration
                double d = 0;
                for( Task t : tasks ) {
                    double td = t.getDuration();
                    if( td < 0 ) {
                        // Then the duration is unknown
                        d = -1;
                        break;
                    }
                    d += td;
                }
                duration = d;
            }
            return duration;    
        }
        
        public double getTimeRemaining() {
            double d = getDuration();
            if( d < 0 ) {
                return -1;
            }
            return Math.max(0, d - time);
        }

        public TaskStatus execute( double tpf ) {            
            TaskStatus result = TaskStatus.Done;
 
            time += tpf;
            
            // Execute each delegate task until we get to
            // one that isn't done... that's the new
            // current and the new status.
            for( ; current < tasks.length; current++ ) {
                double d = tasks[current].getTimeRemaining(); 
                result = tasks[current].execute(tpf);
                if( result != TaskStatus.Done ) {
                    break;
                } else {
                    // Chip away at the tpf for the next execution
                    if( d > 0 ) {
                        tpf -= d;
                    }
                }
                if( tpf <= 0 ) {
                    return TaskStatus.Continue;
                }
            }
            return result;
        }

        public void pausing() {
            // Let the current task know
            if( current < tasks.length ) {
                tasks[current].pausing();
            }
            
            // Let duration recalculate when we restart
            duration = null;
        }

        public void stopping() {
            // Let all tasks run so far know
            for( int i = 0; i < tasks.length; i++ ) {
                if( i > current ) {
                    break;
                }
                tasks[i].stopping();
            }
            
            // Let duration recalculate when we restart
            duration = null;
            
            // And reset
            current = 0;
            time = 0;
        }
        
        @Override
        public String toString() {
            return "SequenceTask[delegates=" + Arrays.asList(tasks) + "]";
        }
    }

    private static class ComposeTask implements Task {
    
        private Task[] tasks;
        private Task[] pending;
        private double time;
        private Double duration;
        
        public ComposeTask( Task[] tasks ) {
            this.tasks = tasks;
            this.pending = tasks.clone();
        }
        
        public double getDuration() {
            if( duration == null ) {
                // Calculate the duration
                double d = 0;
                for( Task t : tasks ) {
                    double td = t.getDuration();
                    if( td < 0 ) {
                        // Then the duration is unknown
                        d = -1;
                        break;
                    }
                    d = Math.max(d, td);
                }
                duration = d;
            }
            return duration;    
        }
        
        public double getTimeRemaining() {
            double d = getDuration();
            if( d < 0 ) {
                return -1;
            }
            return Math.max(0, d - time);
        }
        
        public TaskStatus execute( double tpf ) {
        
            TaskStatus result = TaskStatus.Done;
 
            time += tpf;           
            for( int i = 0; i < pending.length; i++ ) {
                Task t = pending[i];
                if( t == null )
                    continue;
                TaskStatus status = t.execute(tpf);
                if( status == TaskStatus.Done ) {
                    pending[i] = null;
                } else {
                    result = TaskStatus.Continue;
                }
            }
            return result;
        }

        public void pausing() {
            for( Task t : pending ) {
                if( t == null ) {
                    continue;
                }
                t.pausing();
            }
            
            // Let duration recalculate when we restart
            duration = null;
        }

        public void stopping() {
        
            // Let all tasks know we are stopping
            for( Task t : tasks ) {
                t.stopping();
            }
            
            // Let duration recalculate when we restart
            duration = null;
            
            // And reset the pending array
            pending = tasks.clone();
            time = 0;
        }
        
        @Override
        public String toString() {
            return "ComposeTask[delegates=" + Arrays.asList(tasks) + "]";
        }
    }   

    private static class CallTask implements Task {
    
        private Object obj;
        private Method method;
        private Object[] parameters;
        private boolean called;

        public CallTask( Object obj, String methodName, Object... parameters ) {
            if( obj == null ) {
                throw new IllegalArgumentException("Delegate object cannot be null.");
            }
            if( methodName == null ) {
                throw new IllegalArgumentException("Method name cannot be null.");
            }
            
            this.obj = obj;
            this.parameters = parameters;
            
            Class type = obj instanceof Class ? (Class)obj : obj.getClass();
            if( parameters == null || parameters.length == 0 ) {
                try {
                    method = type.getMethod(methodName);
                } catch( NoSuchMethodException e ) {
                    throw new RuntimeException( "Error looking up method:" + methodName + " on object type:" + obj.getClass(), e );
                }
            } else {
                // Find the appropriate method for the parameters given
                for( Method m : type.getMethods() ) {
                    if( !m.getName().equals(methodName) ) {
                        continue;
                    }
 
                    // Check the parameter count.  We could do more
                    // but it's error-prone and we aren't trying to handle
                    // every case here.
                    if( m.getParameterTypes().length != parameters.length ) {
                        continue;
                    }
                    
                    this.method = m;                                      
                }
            
                if( method == null ) {
                    throw new RuntimeException("Error looking up method:" + methodName 
                                                + " on type:" + type);
                }                
            }
            
        }

        public TaskStatus execute( double tpf ) {
            if( !called ) {
                called = true;
                try {
                    method.invoke( obj, parameters );
                } catch( IllegalAccessException e ) {
                    throw new RuntimeException("Error invoking:" + method + " on:" + obj, e);
                } catch( InvocationTargetException e ) {
                    throw new RuntimeException("Error invoking:" + method + " on:" + obj, e);
                }
            }       
            return TaskStatus.Done;
        }
        
        public double getDuration() {
            return 0;
        }
        
        public double getTimeRemaining() {
            return 0;
        }

        public void pausing() {
            // nothing to do
        }

        public void stopping() {
            // reset
            called = false;
        }        
    }    
} 


