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

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.texture.Texture;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.event.BaseAppState;


/**
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class MazeState extends BaseAppState {

    private Node mazeRoot;
    private Maze maze;

    public MazeState( Maze maze ) {
        this.maze = maze;
    }
    
    public Maze getMaze() {
        return maze;
    }

    protected void generateMazeGeometry() {
        float mazeScale = 2;
        Mesh mesh = MeshGenerator.generateMesh(maze, mazeScale, mazeScale);
                
        Geometry geom = new Geometry("maze", mesh);

        GuiGlobals globals = GuiGlobals.getInstance();
        Texture tex = globals.loadTexture("Textures/trap-atlas.png", true, true );
        Material mat = globals.createMaterial(tex, true).getMaterial();
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        mat.setFloat("AlphaDiscardThreshold", 0.1f);
        geom.setMaterial(mat);
        geom.move(-mazeScale * 0.5f, 0, -mazeScale * 0.5f);

        mazeRoot.attachChild(geom); 
    }

    @Override
    protected void initialize( Application app ) {
        mazeRoot = new Node("mazeRoot");        
        generateMazeGeometry();                      
    }

    @Override
    protected void cleanup( Application app ) {
    }

    @Override
    protected void enable() {
        Node rootNode = ((SimpleApplication)getApplication()).getRootNode(); 
        rootNode.attachChild(mazeRoot);                                             
    }

    @Override
    protected void disable() {
        mazeRoot.removeFromParent();
    }
}
