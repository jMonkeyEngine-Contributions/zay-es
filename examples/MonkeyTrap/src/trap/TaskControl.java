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
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import java.util.HashMap;
import java.util.Map;
import trap.anim.SpatialTaskFactories;
import trap.anim.SpatialTaskFactory;
import trap.game.TimeProvider;
import trap.task.Task;
import trap.task.TaskStatus;


/**
 *  Translates logical timed task names to actual
 *  tasks and manages their runtime according to the game
 *  clock.
 *
 *  @author    Paul Speed
 */
public class TaskControl extends AbstractControl {

    private TimeProvider time;
    private Map<String, SpatialTaskFactory> mappings = new HashMap<String, SpatialTaskFactory>();

    private String defaultTask = "Idle";
    private TaskTime current;
    private TaskTime next;
 
    private long frameSmoothing = 1000000000L / 10;  // 1/10th of a second.
    
    private String runningTask; 
    private Task running;

    public TaskControl( TimeProvider time ) {
        this.time = time;
    }
 
    public void setMapping( String name, Task task ) {
        setMapping(name, SpatialTaskFactories.singleton(task));   
    }

    public void setMapping( String name, SpatialTaskFactory factory ) {
        mappings.put(name, factory);
    }
    
    public void addMapping( String name, Object delegate, String methodName ) {
        setMapping(name, SpatialTaskFactories.callMethod(delegate, methodName));
    }

    public void playTask( String name, long startTime, long endTime ) {
        this.next = new TaskTime(name, startTime, endTime);
        
        // The "old" character animation control had kind of a "bug"
        // where the new animation was always set and overrode the currently
        // playing one.  This maked a "bug" where if there were gaps in
        // the time less than the frame delay then they would get smoothed
        // over.  Well, technically none of those are the real bug but
        // they hid the fact that when walking continuously, there are
        // gaps in the activity.  If these gaps are sufficiently small
        // then I think it is ok to smooth them over... we can just slide
        // the end time of the current TaskTime ahead to match the start
        // of this one.
        if( current != null ) {
            // Marry up task windows less than "frameSmoothing" 
            long delta = startTime - current.endTime;
//System.out.println( "Delta:" + (delta /1000000.0) );            
            if( delta > 0 && delta < frameSmoothing ) {
                current.endTime = startTime;
            }
        }
    }    

    private void setCurrent( TaskTime tt ) {
        if( current == tt ) {
            return;
        }
        
        if( setRunningTask(tt.taskName) ) {
            this.current = tt;
        } else {
            // We didn't actually change tasks so 
            // just move the end time forward
            if( current.endTime < tt.endTime )
                current.endTime = tt.endTime;
        } 
    }
    
    protected boolean setRunningTask( String name ) {
 
        if( Objects.equal(name, runningTask) ) {
            return false;
        }
        
        // So we are definitely switching tasks...
        
        // If we had tasks running before then we need to
        // tell them to top.    
        if( running != null ) {
            running.stopping();
            running = null;
        }
        this.runningTask = name;
        SpatialTaskFactory factory = mappings.get(runningTask);
        if( factory == null ) {
            return setRunningTask(defaultTask);
        }
        
        Vector3f loc = spatial.getLocalTranslation();
        running = factory.createTask(spatial, loc);
        return true;
    }
    
    @Override
    protected void controlUpdate( float f ) {

        long now = time.getTime();
        
        // See if it's time to switch over to the next task
        if( next != null && now >= next.startTime ) {
            // Need to switch to next
            setCurrent(next);
            next = null;
        }
        if( current != null && now > current.endTime ) {
            current = null;
            setRunningTask(defaultTask);
        }
        
        // If there is a running task then keep it up to date
        if( running != null ) {
            TaskStatus status = running.execute(f);
            if( status == TaskStatus.Done ) {
                // No reason to keep spinning on it... but we are technically
                // still "running" it from the task control's perspective
                running.stopping();
                running = null;
            }
        }
    }

    @Override
    protected void controlRender( RenderManager rm, ViewPort vp ) {
    }

    private class TaskTime {
        String taskName;
        long startTime;
        long endTime;
        
        public TaskTime( String taskName, long startTime, long endTime ) {
            this.taskName = taskName;
            this.startTime = startTime;
            this.endTime = endTime;
        }
        
        @Override
        public String toString() {
            return "TaskTime[" + taskName + ", " + startTime + ", " + endTime + "]";
        }
    }
}

