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

import com.jme3.util.SafeArrayList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 *  Performs a set of scheduled tasks at specific times
 *  for whatever duration those tasks choose to run.
 *
 *  @author    Paul Speed
 */
public class Timeline implements Task {

    private List<ScheduledTask> schedule = new ArrayList<ScheduledTask>();
    
    private int nextRun = 0;
    private double time;
    private SafeArrayList<ScheduledTask> running = new SafeArrayList<ScheduledTask>(ScheduledTask.class);  

    public Timeline() {
    }
 
    /**
     *  Adds a task that will be run at the specified time
     *  in seconds relative to the timeline's beginning.  The
     *  task will then be executed until it returns TaskStatus.Done.
     */   
    public void addEvent( double time, Task task ) {
        ScheduledTask event = new ScheduledTask(time, task);
        int index = Collections.binarySearch(schedule, event);
        if( index < 0 ) {
            index = -index - 1;
        }
        if( index < schedule.size() ) {
            schedule.add(index, event);
        } else {
            schedule.add(event);
        }
    }

    /**
     *  Moves the timeline forward by the specified time in seconds,
     *  performing any newly pending tasks.
     */
    public TaskStatus execute( double tpf ) {
        time += tpf;
        
        // Move any scheduled tasks to running
        for( ; nextRun < schedule.size(); nextRun++ ) {
            ScheduledTask event = schedule.get(nextRun);
            if( event.time > time ) {
                // Still in the future
                break;
            }
            
            // Else move it to running
            running.add(event);
        }
        
        // Run the events
        for( ScheduledTask event : running.getArray() ) {
            TaskStatus status = event.task.execute(tpf);
            if( status == TaskStatus.Done ) {
                running.remove(event);
            }
        }
 
        if( running.isEmpty() && nextRun >= schedule.size() ) {
            return TaskStatus.Done;
        }       
        return TaskStatus.Continue;
    }
 
    /**
     *  Lets the timeline know that it is pausing.  All currently 
     *  running tasks will also have their pausing() methods called
     *  but the pending tasks will not.
     *  Subsequent execute() calls will continue the timeline where
     *  it left off.     
     */   
    public void pausing() {
        // Let the running tasks know
        for( ScheduledTask event : running.getArray() ) {
            event.task.pausing();
        }
    }
 
    /**
     *  Lets the timeline know that it is stopping and that the
     *  internal state should be reset.  All currently running tasks
     *  will also have their stopping() methods called but the pending
     *  tasks will not.
     *  Subsequent execute calls will restart the timeline from the
     *  beginning. 
     */   
    public void stopping() {
        // Let the running tasks know
        for( ScheduledTask event : running.getArray() ) {
            event.task.stopping();
        }
        
        // And reset everything to 0 state
        running.clear();
        nextRun = 0;
    }
 
    @Override
    public String toString() {
        return "TimeLine[schedule=" + schedule + "]";
    }
 
    private class ScheduledTask implements Comparable<ScheduledTask> {
        double time;
        Task task;
        
        public ScheduledTask( double time, Task task ) {
            this.time = time;
            this.task = task;
        }

        public int compareTo(ScheduledTask o) {
            return (int)(time - o.time);
        }
        
        @Override
        public String toString() {
            return "ScheduledTask[time=" + time + ", task=" + task + "]";
        }
    }          
}



