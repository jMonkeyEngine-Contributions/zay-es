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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;


/**
 *  Bobbing and floating... for powerups.
 *
 *  @author    Paul Speed
 */
public class FloatControl extends AbstractControl {

    private float bobRate = 4f;
    private float turnRate = 1f;
    private float baseY;
    
    private float bobScale = 0.1f;
    private float bobAngle;
    private float angle;

    @Override
    public void setSpatial( Spatial s ) {
        super.setSpatial(s);
        baseY = s.getLocalTranslation().y;
    }

    @Override
    protected void controlUpdate( float tpf ) {
        bobAngle += tpf * bobRate;
        if( bobAngle > FastMath.TWO_PI ) {
            bobAngle -= FastMath.TWO_PI; 
        }
        angle += tpf * turnRate;
        if( angle > FastMath.TWO_PI ) {
            angle -= FastMath.TWO_PI; 
        }
        Vector3f loc = spatial.getLocalTranslation();         
        loc.y = baseY + bobScale + FastMath.sin(bobAngle) * bobScale; 
        spatial.setLocalTranslation(loc);
        spatial.setLocalRotation(spatial.getLocalRotation().fromAngles(0, angle, 0));
    }

    @Override
    protected void controlRender( RenderManager rm, ViewPort vp ) {
    }
    
}
