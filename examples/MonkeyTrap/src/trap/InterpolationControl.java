/*
 * $Id$
 *
 * Copyright (c) 2012, Paul Speed
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1) Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2) Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3) Neither the names "Progeeks", "Meta-JB", nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package trap;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;


/**
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class InterpolationControl extends AbstractControl {

    private Vector3f start = new Vector3f();
    private Vector3f end = new Vector3f();
    private double speed;
    private double tpfScale;
    private double step = 1.0;

    public InterpolationControl( double speed ) {
        this.speed = speed;
    } 

    public void setTarget( Vector3f target ) {
        if( end.equals(target) ) {
            return;
        }
        // Calculate how fast we should move to acheive the
        // desired speed.
        Vector3f current = spatial.getLocalTranslation();
        if( current.equals(Vector3f.ZERO) ) {
            // Just set the value and move on
            spatial.setLocalTranslation(target);
            return;
        }
        
        // Otherwise, figure out how long it will take to
        // get there.
        float distance = target.distance(current);
 
        // Speed is meters / second
        // Speed is also distance / travel time
        // ...travel time = distance / speed
        // To make step's 0 - 1.0 match travel time we
        // multiply total time by 1 / travel time...
        // ie: speed / distance
        tpfScale = speed / distance;
        step = 0;
        start.set(current);
        end.set(target);         
    }

    public double getStep() {
        return step;
    }

    @Override
    protected void controlUpdate( float tpf ) {
        if( step < 1.0 ) {
            step += tpf * tpfScale;
            step = Math.min(step, 1.0);
            
            // Reuse the spatial's vector but set it back to 
            // make sure it updates
            Vector3f v = spatial.getLocalTranslation(); 
            v.interpolateLocal(start, end, (float)step);
            spatial.setLocalTranslation(v);            
        }
    }

    @Override
    protected void controlRender( RenderManager rm, ViewPort vp ) {
    }
}
