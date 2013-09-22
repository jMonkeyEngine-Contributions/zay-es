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

import trap.game.TimeProvider;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import java.util.LinkedList;


/**
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class InterpolationControl extends AbstractControl {

    private TimeProvider time;
    private Vector3f end = new Vector3f();
    private long startTime;
    private long endTime;

    private TimeSpan current;
    private LinkedList<TimeSpan> pending = new LinkedList<TimeSpan>();

    public InterpolationControl( TimeProvider time ) {
        this.time = time;
    } 

    public void setTarget( Vector3f target, Quaternion rotation, long startTime, long endTime ) {
        // We support a one-deep stack because we likely get game
        // events slightly ahead of when we render them.  We need
        // to let the last "tween" finish.
        if( current != null ) {
            if( endTime < current.endTime ) {
                throw new RuntimeException( "Interpolation step goes back in time:" 
                                            + target + ", " + startTime + ", " + endTime );
            }
        
            pending.add(new TimeSpan(target, rotation, startTime, endTime));
        } else {
            current = new TimeSpan(target, rotation, startTime, endTime);
        }
    }

    @Override
    protected void controlUpdate( float tpf ) {
        
        while( current != null ) {
            long now = time.getTime();
            
            // Cycle through current and all pending events
            // until we get one with more to go.
            if( current.apply(now) ) {
                break;
            }
 
            if( pending.isEmpty() ) {
                current = null;
            } else {           
                // Else grab the next one
                TimeSpan next = pending.removeFirst();
                current = next;
            }
        }        
    }

    @Override
    protected void controlRender( RenderManager rm, ViewPort vp ) {
    }
    
    private class TimeSpan {
        long startTime;
        long endTime;
        Vector3f startPos;
        Vector3f endPos;
        Quaternion startRot;
        Quaternion endRot;
        
        public TimeSpan( Vector3f endPos, Quaternion endRot, long startTime, long endTime ) {
            this.endPos = endPos;
            this.endRot = endRot;
            this.startTime = startTime;
            this.endTime = endTime;
            if( startTime == endTime ) {
                startPos = endPos;
            }
        }
        
        public boolean apply( long now ) {
            if( now < startTime ) {
                return true;
            }                
 
            if( startPos == null ) {
                startPos = spatial.getLocalTranslation().clone();
                startRot = spatial.getLocalRotation().clone();
            }
 
            if( now >= endTime ) {
                // Force the spatial to the last position
                spatial.setLocalTranslation(endPos);
                spatial.setLocalRotation(endRot);
                
                return false; // no more to go               
            } else {
                // Interpolate... guaranteed to have a non-zero time delta here
                double part = (now - startTime) / (double)(endTime - startTime);                
                
                // Do our own interp calculation because Vector3f's is inaccurate and
                // can return values out of range... especially in cases where part is
                // small and delta between coordinates is 0.  (Though this probably
                // wasn't the issue I was trying to fix, it is worrying in general.)                
                Vector3f v = spatial.getLocalTranslation();
                double x = v.x;
                double y = v.y;
                double z = v.z;
                x = startPos.x + (endPos.x - startPos.x) * part;
                y = startPos.y + (endPos.y - startPos.y) * part;
                z = startPos.z + (endPos.z - startPos.z) * part;
                spatial.setLocalTranslation((float)x, (float)y, (float)z);
 
                Quaternion rot = startRot.clone();
                rot.nlerp(endRot, (float)part);
                spatial.setLocalRotation(rot);
                
                return true; // still have more to go
            }
        }
    }
}
