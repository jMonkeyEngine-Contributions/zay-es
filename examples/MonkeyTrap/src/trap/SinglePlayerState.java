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

import trap.game.Position;
import trap.game.Maze;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.es.ComponentFilter;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.Filters;
import com.simsilica.es.Name;
import com.simsilica.lemur.event.BaseAppState;
import java.util.ArrayList;
import java.util.List;
import trap.game.ArmorStrength;
import trap.game.CombatStrength;
import trap.game.GameSystems;
import trap.game.HitPoints;
import trap.game.MaxHitPoints;
import trap.game.MazeService;
import trap.game.ModelType;
import trap.game.MonkeyTrapConstants;


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

    // There will be only one in single player but this way
    // it will get refreshed automatically.
    private EntitySet players;

    public SinglePlayerState() {
    }

    @Override
    protected void initialize(Application app) {
        GameSystems systems = new GameSystems();
 
        // Create the single player client and start it up               
        client = new SinglePlayerClient(systems);
        client.start();
 
        // Grab some service and client properties that we will need for
        // our client-side states.       
        Maze maze = systems.getService(MazeService.class).getMaze();        
        EntityData ed = client.getEntityData(); 

        gameStates.add(new EntityDataState(ed));
        gameStates.add(new ModelState(client.getRenderTimeProvider(), new TrapModelFactory()));
        gameStates.add(new CharacterAnimState());
        gameStates.add(new MazeState(maze));
        gameStates.add(new PlayerState(client));
        gameStates.add(new HudState());
   
        //gameStates.add(new FlyCamAppState());
 
 
        // We only care about the monkeys...
        ComponentFilter filter = Filters.fieldEquals(ModelType.class, 
                                                     "type", 
                                                     MonkeyTrapConstants.TYPE_MONKEY.getType());
        players = ed.getEntities(filter, ModelType.class, Name.class, 
                                         HitPoints.class, MaxHitPoints.class,
                                         CombatStrength.class, ArmorStrength.class);
        
        // Create a second entity just for testing stuff
        //EntityId test = ed.createEntity();

        // Use the maze seed as starting position
        //ed.setComponent(test, new Position(location, -1, -1));        
        //ed.setComponent(test, TrapModelFactory.TYPE_OGRE);        
 
        EntityId test;// = ed.createEntity();
        //ed.setComponent(test, new Position(new Vector3f(maze.getXSeed()*2, 0, maze.getYSeed()*2), -1, -1));        
        //ed.setComponents(test, MonkeyTrapConstants.TYPE_BARRELS, new HitPoints(MonkeyTrapConstants.BARREL_HITPOINTS));           
        //ed.setComponents(test, MonkeyTrapConstants.TYPE_RING1);           

        test = ed.createEntity();
        ed.setComponent(test, new Position(new Vector3f((maze.getXSeed()-1)*2, 0, maze.getYSeed()*2), -1, -1));        
        ed.setComponent(test, MonkeyTrapConstants.TYPE_BANANA);        

        test = ed.createEntity();
        ed.setComponent(test, new Position(new Vector3f((maze.getXSeed()+1)*2, 0, maze.getYSeed()*2), -1, -1));        
        //ed.setComponents(test, MonkeyTrapConstants.TYPE_CHEST, new HitPoints(MonkeyTrapConstants.CHEST_HITPOINTS));           
        ed.setComponents(test, MonkeyTrapConstants.TYPE_POTION1);           

        test = ed.createEntity();
        ed.setComponent(test, new Position(new Vector3f((maze.getXSeed()+2)*2, 0, maze.getYSeed()*2), -1, -1));        
        //ed.setComponents(test, MonkeyTrapConstants.TYPE_CHEST, new HitPoints(MonkeyTrapConstants.CHEST_HITPOINTS));           
        ed.setComponents(test, MonkeyTrapConstants.TYPE_RING1);           

        test = ed.createEntity();
        ed.setComponent(test, new Position(new Vector3f((maze.getXSeed()+3)*2, 0, maze.getYSeed()*2), -1, -1));        
        //ed.setComponents(test, MonkeyTrapConstants.TYPE_CHEST, new HitPoints(MonkeyTrapConstants.CHEST_HITPOINTS));           
        ed.setComponents(test, MonkeyTrapConstants.TYPE_RING2);           
        
        // Attach them all
        AppStateManager stateMgr = app.getStateManager();
        for( AppState state : gameStates ) {
            stateMgr.attach(state);   
        }
                
        // In single player we know we will be there already
        getState(HudState.class).setPlayer(players.getEntity(client.getPlayer()));  
    }

    @Override
    protected void cleanup(Application app) {
        players.release();
        
        // Detach all the states we added... in reverse order
        AppStateManager stateMgr = app.getStateManager();
        for( int i = gameStates.size() -1; i >= 0; i-- ) {
            AppState state = gameStates.get(i);
            stateMgr.attach(state);   
        }
        gameStates.clear();
        client.close();
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
 
        client.updateRenderTime();
        
        if( players.applyChanges() ) {
            getState(HudState.class).updatePlayer();
        }
        //updateMaze(tpf);
    }

    @Override
    protected void disable() {
        //test.removeFromParent();
        getState(MusicState.class).setSong(null);
    }
}
