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

import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import java.util.HashMap;
import java.util.Map;


/**
 *  When attached to a node, it will find all of the 
 *  geometries, collect their ambient and diffuse colors,
 *  and keep track of them for later setting and resetting
 *  alpha values and so on.
 *
 *  @author    Paul Speed
 */
public class ColorControl extends AbstractControl {

    // Note: the map keys are really only valid during setup...   
    //       then just the values() are used.
    private Map<ColorRGBA, ColorEntry> entryMap = new HashMap<ColorRGBA, ColorEntry>();

    private ColorRGBA baseDiffuse;
    private ColorRGBA baseAmbient;
    private ColorRGBA color;

    public ColorControl( ColorRGBA baseDiffuse, ColorRGBA baseAmbient ) {
        this.baseDiffuse = baseDiffuse;
        this.baseAmbient = baseAmbient;
    }

    @Override
    public void setSpatial( Spatial s ) {
        super.setSpatial(s);
System.out.println( "Extracting colors from:" + s );        
        setupMaterials(s);
        s.setQueueBucket(Bucket.Transparent);
    }

    public void setColor( ColorRGBA color ) {
        if( this.color != null && !this.color.equals(color) ) {
            return;
        }
        
        this.color = color.clone();
        
        // Update all of the colors
        for( ColorEntry e : entryMap.values() ) {
System.out.println( "Before:" + e );        
            e.update(color);
System.out.println( " after:" + e );            
        } 
    } 

    protected ColorRGBA getColor( Material m, String name ) {
        MatParam mp = m.getParam(name);
        if( mp == null ) {
            return null;
        }
        return (ColorRGBA)mp.getValue();
    }

    private ColorEntry updateEntry( ColorRGBA c, ColorRGBA base ) {
System.out.println( "updateEntry(" + c + ", " + base + ")" );    
        if( c == null ) {
            c = base;
        } else {
            c = c.mult(base);
        }
        
        ColorEntry result = entryMap.get(c);
        if( result == null ) {
System.out.println( "New entry..." );        
            result = new ColorEntry(c);
            entryMap.put(c, result);
        }
        return result;
    }

    protected void setupMaterials( Spatial s ) {
        if( s instanceof Node ) {
            Node n = (Node)s;
            for( Spatial child : n.getChildren() ) {
                setupMaterials(child);
            }
        } else if( s instanceof Geometry ) {
            Geometry geom = (Geometry)s;
            Material m = geom.getMaterial();
 
System.out.println( "Material name:" + m.getName() + "  asset:" + m.getAssetName() ); 
            // If the blend mode is originally Alpha then we will
            // push it up a layer since we eventually set the blend
            // mode for everything to alpha
            if( m.getAdditionalRenderState().getBlendMode() == BlendMode.Alpha ) {
                geom.setUserData("layer", 2);
            }

            String defName = m.getMaterialDef().getAssetName();
System.out.println( "Checking defName:" + defName );            
            if( "Common/MatDefs/Light/Lighting.j3md".equals(defName)
                || "MatDefs/Glass.j3md".equals(defName) ) {
                
                // Get the original values if they exist
                ColorEntry diffuse = updateEntry(getColor(m, "Diffuse"), baseDiffuse);
                m.setColor("Diffuse", diffuse.current);
                
                ColorEntry ambient = updateEntry(getColor(m, "Ambient"), baseAmbient);
                m.setColor("Ambient", ambient.current);
                               
System.out.println( "  diffuse: " + diffuse + "  result:" + getColor(m, "Diffuse") );            
System.out.println( "  ambient: " + ambient + "  result:" + getColor(m, "Ambient") );            
System.out.println( "  useMatColors: " + m.getParam("UseMaterialColors") );            

                m.setBoolean("UseMaterialColors", true);
System.out.println( "  after: useMatColors: " + m.getParam("UseMaterialColors") );            
                
                m.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);                
            }                
        }
    }

    @Override
    protected void controlUpdate( float tpf ) {
    }

    @Override
    protected void controlRender( RenderManager rm, ViewPort vp ) {
    }
 
    private class ColorEntry {
        ColorRGBA original;
        ColorRGBA current;
        
        public ColorEntry( ColorRGBA original ) {
            this.original = original;
            this.current = original.clone();
        }
        
        public void update( ColorRGBA color ) {
            current.set(original.mult(color));
        }
        
        @Override
        public String toString() {
            return "ColorEntry[" + original + ", " + current + "]";
        }
    }   
}
