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
        
        public SequenceTask( Task[] tasks ) {
            this.tasks = tasks;
        }

        public TaskStatus execute( double tpf ) {            
            TaskStatus result = TaskStatus.Done;
            
            // Execute each delegate task until we get to
            // one that isn't done... that's the new
            // current and the new status.
            for( ; current < tasks.length; current++ ) {
                result = tasks[current].execute(tpf);
                if( result != TaskStatus.Done ) {
                    break;
                }
            }
            return result;
        }

        public void pausing() {
            // Let the current task know
            if( current < tasks.length ) {
                tasks[current].pausing();
            }
        }

        public void stopping() {
            // Let the current task know
            if( current < tasks.length ) {
                tasks[current].stopping();
            }
            
            // And reset
            current = 0;
        }
        
        @Override
        public String toString() {
            return "SequenceTask[delegates=" + Arrays.asList(tasks) + "]";
        }
    }

    private static class ComposeTask implements Task {
    
        private Task[] tasks;
        private Task[] pending;
        
        public ComposeTask( Task[] tasks ) {
            this.tasks = tasks;
            this.pending = tasks.clone();
        }
        
        public TaskStatus execute( double tpf ) {
        
            TaskStatus result = TaskStatus.Done;
            
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
        }

        public void stopping() {
            for( Task t : pending ) {
                if( t == null ) {
                    continue;
                }
                t.stopping();
            }
            
            // And reset the pending array
            pending = tasks.clone();
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

        public void pausing() {
            // nothing to do
        }

        public void stopping() {
            // reset
            called = false;
        }        
    }    
} 


/*
public class AnimationTasks
{
    public static AnimationTask call( Object obj, String methodName )
    {
        return new CallTask( obj, methodName );
    }

    public static AnimationTask call( Object obj, String methodName, Object... parameters )
    {
        return new CallWithParametersTask( obj, methodName, parameters );
    }
    
    private static class CallTask implements AnimationTask
    {
        private Object obj;
        private Method method;
        private boolean ended;
        
        public CallTask( Object obj, String methodName )        
        {
            this.obj = obj;
            try
                {
                method = obj.getClass().getMethod(methodName);
                }
            catch( NoSuchMethodException e )
                {
                throw new RuntimeException( "Error looking up method:" + methodName + " on object type:" + obj.getClass(), e );
                }
        }
        
        public boolean animate( double seconds )
        {            
            end();
            return false;
        }
        
        public void end()
        {
            if( ended )
                return;
            ended = true;
            try
                {
                method.invoke( obj );
                }
            catch( IllegalAccessException e )
                {
                throw new RuntimeException( "Error invoking:" + method + " on:" + obj, e );
                }
            catch( InvocationTargetException e )
                {
                throw new RuntimeException( "Error invoking:" + method + " on:" + obj, e );
                }
        }
    }

    private static class CallWithParametersTask implements AnimationTask
    {
        private Object obj;
        private Method method;
        private Object[] parameters;
        
        public CallWithParametersTask( Object obj, String methodName, Object[] parameters )        
        {
            this.obj = obj;
            this.parameters = parameters;
            
            for( Method m : obj.getClass().getMethods() )
                {
                if( !m.getName().equals(methodName) )
                    continue;
                       
                method = m;
                // For now, a simple exact match, no fancy lookups
                if( method.getParameterTypes().length != parameters.length )
                    throw new RuntimeException( "Parameter count mismatch for:" + methodName + " expected:" + parameters.length + " found:" + method.getParameterTypes().length );
                }
            
            if( method == null )
                throw new RuntimeException( "Error looking up method:" + methodName + " on object type:" + obj.getClass() );
        }
        
        public boolean animate( double seconds )
        {
            end();
            return false;
        }
        
        public void end()
        {
            try
                {
                method.invoke( obj, parameters );
                }
            catch( IllegalAccessException e )
                {
                throw new RuntimeException( "Error invoking:" + method + " on:" + obj, e );
                }
            catch( InvocationTargetException e )
                {
                throw new RuntimeException( "Error invoking:" + method + " on:" + obj, e );
                }
        }
    }

    private static class DelayTask implements AnimationTask
    {
        private double duration;
        private double time;
    
        public DelayTask( double duration )
        {
            this.duration = duration;
        }
 
        public boolean animate( double seconds )
        {
            time += seconds;
            return time < duration;        
        }
    
        public void end()
        {
        }
    }

    private static class PlaySoundTask implements AnimationTask
    {
        private Sound sound;
        private boolean started = false;
 
        public PlaySoundTask( Sound sound )
        {
            this.sound = sound;
        }
    
        public boolean animate(double seconds)
        {
            if( !started )
                {
                started = true;
                sound.play();
                }
            return sound.isPlaying();
        }
        
        public void end()
        {
            if( started )
                sound.stop();
        }
    }

    private static class StopSoundTask implements AnimationTask
    {
        private Sound sound;
 
        public StopSoundTask( Sound sound )
        {
            this.sound = sound;
        }
    
        public boolean animate(double seconds)
        {
System.out.println( "Calling stop on:" + sound );        
            sound.stop();
            return false;
        }
        
        public void end()
        {
            sound.stop();
        }
    }
 
    private static class ComposeTask implements AnimationTask
    {
        private AnimationTask[] tasks;
        
        public ComposeTask( AnimationTask[] tasks )
        {
            this.tasks = tasks;
        }
        
        public boolean animate(double seconds)
        {
            boolean keepGoing = false;
            
            for( int i = 0; i < tasks.length; i++ )
                {
                AnimationTask t = tasks[i];
                if( t == null )
                    continue;
                if( t.animate(seconds) )
                    keepGoing = true;
                else
                    tasks[i] = null;
                }
            return keepGoing;
        }
        
        public void end()
        {
            for( int i = 0; i < tasks.length; i++ )
                {
                AnimationTask t = tasks[i];
                if( t == null )
                    continue;
                t.end();
                }
        }
    }   
}*/
