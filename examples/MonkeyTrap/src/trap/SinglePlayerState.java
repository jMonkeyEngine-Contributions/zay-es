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
import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.BufferUtils;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.event.BaseAppState;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


/**
 *
 *  @author    Paul Speed
 */
public class SinglePlayerState extends BaseAppState
{
    private List<AppState> gameStates = new ArrayList<AppState>();

    // Kept so we can keep its time up to date... actually
    // we will need to do this for multiplayer, too.
    private SinglePlayerClient client;

    public SinglePlayerState() {
    }

    @Override
    protected void initialize(Application app) {
        EntityData ed = new DefaultEntityData();
        Maze maze = new Maze(48, 48);
        //long seed = 1379736430682L; 
        long seed = System.currentTimeMillis();
System.out.println( "Using seed:" + seed );         
        maze.setSeed(seed);
        maze.generate();

        gameStates.add(new EntityDataState(ed));
        gameStates.add(new ModelState(new TrapModelFactory()));
        gameStates.add(new CharacterAnimState());
        gameStates.add(new MazeState(maze));
 
        //gameStates.add(new FlyCamAppState());
 
        // Create a player entity here for now
        EntityId player = ed.createEntity();

        // Use the maze seed as starting position
        Vector3f location = new Vector3f(maze.getXSeed() * 2, 0, maze.getYSeed() * 2);
        System.out.println( "Setting player to location:" + location );
        ed.setComponent(player, new Position(location, -1, -1));        
        ed.setComponent(player, TrapModelFactory.TYPE_MONKEY);        
 
        client = new SinglePlayerClient(ed, player, maze);
        gameStates.add(new PlayerState(client));
 
        // Create a second entity just for testing stuff
        //EntityId test = ed.createEntity();

        // Use the maze seed as starting position
        //ed.setComponent(test, new Position(location, -1, -1));        
        //ed.setComponent(test, TrapModelFactory.TYPE_OGRE);        
        
        // Attach them all
        AppStateManager stateMgr = app.getStateManager();
        for( AppState state : gameStates ) {
            stateMgr.attach(state);   
        }        
    }

    @Override
    protected void cleanup(Application app) {
        // Detach all the states we added... in reverse order
        AppStateManager stateMgr = app.getStateManager();
        for( int i = gameStates.size() -1; i >= 0; i-- ) {
            AppState state = gameStates.get(i);
            stateMgr.attach(state);   
        }
        gameStates.clear();
    }

    @Override
    protected void enable() {
        getState(MusicState.class).setSong("Sounds/ambient-theme.ogg", 1);
        getState(MusicState.class).setVolume(0.5f);

        Node rootNode = ((SimpleApplication)getApplication()).getRootNode(); 
 
        /** A white, directional light source */
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection((new Vector3f(-0.75f, -0.95f, -0.5f)).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);

        /** A white ambient light source. */
        AmbientLight ambient = new AmbientLight();
        //ambient.setColor(ColorRGBA.DarkGray);
        ambient.setColor(ColorRGBA.White);
        rootNode.addLight(ambient);

                       
        //setupMazeVis(rootNode);               
    }

/*
    Maze testMaze = new Maze(48, 48);
    ImageRaster testRaster;
    double nextMaze = 0; 

    protected void setupMazeVis( Node parent ) {
    
        long seed = System.currentTimeMillis();
        //seed = 1379454737042L;
        System.out.println( "Using seed:" + seed );             
        testMaze.setSeed(seed);               
    
        GuiGlobals globals = GuiGlobals.getInstance();
        Quad maze = new Quad(2, 2);
        ByteBuffer data = BufferUtils.createByteBuffer(64*64*4);
        for( int i = 0; i < 64*64*4; i++ )
            data.put((byte)0xff);
        data.flip();
        Image img = new Image(Format.ABGR8, 64, 64, data);
        testRaster = ImageRaster.create(img);
        Texture2D tex = new Texture2D(img);
        tex.setMagFilter(MagFilter.Nearest);
        Geometry geom = new Geometry("maze", maze);
        geom.setLocalTranslation(0, 6, 2);
        Material mat = globals.createMaterial(tex, false).getMaterial();
        geom.setMaterial(mat);
        
        parent.attachChild(geom);
    }
    
    protected void updateMaze( float tpf ) {
        int ran = testMaze.generate(8);
        if( ran > 0 ) {
            for( int y = 0; y < testMaze.getHeight(); y++ ) {
                for( int x = 0; x < testMaze.getWidth(); x++ ) {
                    int v = testMaze.get(x,y);
                    ColorRGBA color = ColorRGBA.Black;
                    if( v == 1 ) {
                        color = ColorRGBA.Green;
                    } else if( v == -1 ) {
                        color = ColorRGBA.Red;
                    }
                    testRaster.setPixel(x, y, color);
                }
            }
        } else {
            nextMaze += tpf;
            if( nextMaze > 5.0 ) {
                long seed = System.currentTimeMillis();
                System.out.println( "Using seed:" + seed );             
                testMaze.setSeed(seed);
                nextMaze = 0;        
            }
        }
    }*/
    
    @Override
    public void update( float tpf ) {
 
        client.updateFrameTime();
        //updateMaze(tpf);
    }

    @Override
    protected void disable() {
        //test.removeFromParent();
        getState(MusicState.class).setSong(null);
    }
}
