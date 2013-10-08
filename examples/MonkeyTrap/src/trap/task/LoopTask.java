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


/**
 *  A variable-duration task that delegates to another task
 *  that it will loop over.  
 *
 *  @author    Paul Speed
 */
public class LoopTask implements Task {

    private Task delegate;
    private double time;
    private double duration;
    
    public LoopTask( Task delegate, double duration ) {
        this.delegate = delegate;
        this.duration = duration;
    }

    protected double executePart( double tpf ) {
    
        double d = delegate.getTimeRemaining();
        TaskStatus result = delegate.execute(tpf);
        
        double actual = Math.min(tpf, d);
        time += actual;
        
        if( result == TaskStatus.Done && time < duration ) {
            // Reset for next time
            delegate.stopping();
        } else if( time >= duration && result != TaskStatus.Done ) {
            // We are done and need to tell the delegate to
            // at least pause
            delegate.pausing();
            
            // Let the caller know we used everything, basically.
            return tpf;
        }
        return actual;
    }

    public TaskStatus execute( double tpf ) {
        if( time >= duration ) {
            return TaskStatus.Done;
        }
        while( tpf > 0 ) {
            tpf -= executePart(tpf);
        }
        return time < duration ? TaskStatus.Continue : TaskStatus.Done;        
    }

    public double getDuration() {
        return duration;
    }

    public double getTimeRemaining() {
        return Math.max(0, time - duration); 
    }

    public void pausing() {
        delegate.pausing();
    }

    public void stopping() {
        delegate.stopping();
        time = 0;
    }
}
