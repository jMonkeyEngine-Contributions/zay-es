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
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.audio.Environment;
import com.jme3.audio.Listener;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.es.ComponentFilter;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import com.simsilica.es.Filters;
import com.simsilica.es.Name;
import com.simsilica.lemur.event.BaseAppState;
import java.util.ArrayList;
import java.util.List;
import trap.game.ArmorStrength;
import trap.game.CombatStrength;
import trap.game.HitPoints;
import trap.game.MaxHitPoints;
import trap.game.Maze;
import trap.game.ModelType;
import trap.game.MonkeyTrapConstants;
import trap.game.TimeProvider;


/**
 *
 *  @author    Paul Speed
 */
public class GamePlayState extends BaseAppState {    
    private List<AppState> gameStates = new ArrayList<AppState>();

    private GameClient client;

    // There will be only one in single player but this way
    // it will get refreshed automatically.
    private EntitySet players;

    private Listener audioListener = new Listener(); 

    public GamePlayState( GameClient client ) { 
        this.client = client;
    }

    @Override
    protected void initialize(Application app) {

        // Move this to an audio manager state 
        app.getAudioRenderer().setListener(audioListener);
        
        // Setup the audio environment... here for now              
        app.getAudioRenderer().setEnvironment(Environment.Closet);
        
        Effects.initialize(client.getRenderTimeProvider(), app.getAssetManager());
 
        // Grab some client properties that we will need for
        // our client-side states.       
        Maze maze = client.getMaze();
        EntityData ed = client.getEntityData(); 

        TimeProvider time = client.getRenderTimeProvider(); 

        gameStates.add(new EntityDataState(ed));
        gameStates.add(new ModelState(time, new TrapModelFactory(app.getAssetManager(), audioListener, time)));
        gameStates.add(new CharacterAnimState());
        gameStates.add(new DeathState(time));
        gameStates.add(new MazeState(maze));
        gameStates.add(new PlayerState(client, audioListener));
        gameStates.add(new HudState());
   
        //gameStates.add(new FlyCamAppState());
 
 
        // We only care about the monkeys...
        ComponentFilter filter = Filters.fieldEquals(ModelType.class, 
                                                     "type", 
                                                     MonkeyTrapConstants.TYPE_MONKEY.getType());
        players = ed.getEntities(filter, ModelType.class, Name.class, 
                                         HitPoints.class, MaxHitPoints.class,
                                         CombatStrength.class, ArmorStrength.class);
        
        // Attach all of the child states
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

    }

    @Override
    public void update( float tpf ) {
 
        client.updateRenderTime();
        
        if( players.applyChanges() ) {
            getState(HudState.class).updatePlayer();
        }
    }

    @Override
    protected void disable() {
        getState(MusicState.class).setSong(null);
    }
}
