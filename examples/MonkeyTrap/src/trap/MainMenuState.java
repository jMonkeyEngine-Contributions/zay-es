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
 * SOFTWARE, Even IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package trap;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.component.IconComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.event.BaseAppState;
import com.simsilica.lemur.style.ElementId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trap.game.GameSystems;


/**
 *  Lays out the main menu controls and handles the clicks.
 *  When enabled/disabled the main menu is shown/hidden.
 *
 *  @author    Paul Speed
 */
public class MainMenuState extends BaseAppState {

    static Logger log = LoggerFactory.getLogger(MainMenuState.class);
    
    private Container menu;

    private Container network;
    private boolean networkPanelOpen = false;

    private TextField userField;
    private TextField hostField;
    private TextField portField;

    private AudioNode startSound;
    private AudioNode exitSound;
    private AudioNode selectSound;
    private AudioNode errorSound;

    private AudioNode music;

    public MainMenuState() {
    }

    @Override
    protected void initialize( Application app ) {

        menu = new Container(new SpringGridLayout(), new ElementId(DungeonStyles.MENU_ID), "dungeon");


        Label title = menu.addChild(new Label("", new ElementId(DungeonStyles.MENU_TITLE_ID), "dungeon"));
        
        IconComponent titleImage = new IconComponent( "Interface/logo-512x256.png", 
                                                      new Vector2f(1, 0.6f), 
                                                      5, 5, 0, false );
        title.setBackground(titleImage);
         
        Button single = menu.addChild(new Button("Single Player", "dungeon"));
        single.addClickCommands(new StartSingle());
        single.addCommands(Button.ButtonAction.HighlightOn, new Highlight());

        Button multi = menu.addChild(new Button("Multi-Player", "dungeon"));
        multi.addClickCommands(new OpenMulti());
        multi.addCommands(Button.ButtonAction.HighlightOn, new Highlight());

        // We'll add exit later in the grid to get some space for the
        // menu panel.
        Button exit = menu.addChild(new Button("Exit", "dungeon"), 10, 0);
        exit.addClickCommands(new Exit());
        exit.addCommands(Button.ButtonAction.HighlightOn, new Highlight());

        Camera cam = app.getCamera();
        float menuScale = cam.getHeight()/720f;

        Vector3f pref = menu.getPreferredSize();
        float bias = (cam.getHeight() - (pref.y*menuScale)) * 0.35f;
        menu.setLocalTranslation(cam.getWidth() * 0.5f - pref.x * 0.5f * menuScale,
                                 cam.getHeight() * 0.5f + pref.y * 0.5f * menuScale + bias,
                                 10);
        menu.setLocalScale(menuScale);


        // Create the dymamic network settings
        // panel
        network = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.Even, FillMode.Last), 
                                new ElementId(DungeonStyles.SUBMENU_ID), "dungeon");
        network.addChild(new Label("Player:", new ElementId(DungeonStyles.EDIT_LABEL_ID), "dungeon"));
        userField = network.addChild(new TextField("Random" + Math.round(Math.random() * 100), "dungeon"), 1);
        network.addChild(new Label("Host:", new ElementId(DungeonStyles.EDIT_LABEL_ID), "dungeon"));
        hostField = network.addChild(new TextField("", "dungeon"), 1);
        network.addChild(new Label("Port:", new ElementId(DungeonStyles.EDIT_LABEL_ID), "dungeon"));
        portField = network.addChild(new TextField("4284", "dungeon"), 1);       

        network.addChild(new Label("")); // just a spacer to create a new row
        Button startMulti = network.addChild(new Button("Connect", "dungeon"),1);
        startMulti.addClickCommands(new StartMulti());
        startMulti.addCommands(Button.ButtonAction.HighlightOn, new Highlight());
        
          
        // Just to test
        //menu.addChild(network, 9, 0);


        AssetManager assets = app.getAssetManager();
        startSound = new AudioNode(assets, "Sounds/gong.ogg", false);
        startSound.setReverbEnabled(false);
        startSound.setPositional(false);
        exitSound = new AudioNode(assets, "Sounds/rumble.ogg", false);
        exitSound.setReverbEnabled(false);
        exitSound.setPositional(false);
        selectSound = new AudioNode(assets, "Sounds/ting.ogg", false);
        selectSound.setReverbEnabled(false);
        selectSound.setPositional(false);
        errorSound = new AudioNode(assets, "Sounds/error.ogg", false);
        errorSound.setReverbEnabled(false);
        errorSound.setPositional(false);

    }

    @Override
    protected void cleanup( Application app ) {
    }

    @Override
    protected void enable() {
        Main main = (Main)getApplication();
        main.getGuiNode().attachChild(menu);
    }

    @Override
    protected void disable() {
        menu.removeFromParent();
    }

    private class Highlight implements Command<Button> {
        public void execute( Button source ) {
            float pitch = (float)(1.0 + (Math.random() * 0.2 - 0.1));
            selectSound.setPitch(pitch);
            selectSound.playInstance();
        }
    }

    private class StartSingle implements Command<Button> {
        public void execute( Button source ) {
            startSound.playInstance();
            
            // Create the single player client and start it up               
            SinglePlayerClient client = new SinglePlayerClient(new GameSystems());
            client.start();
            
            getStateManager().attach(new GamePlayState(client));
            setEnabled(false);
        }
    }

    private class OpenMulti implements Command<Button> {
        public void execute( Button source ) {
            startSound.playInstance();
            networkPanelOpen = !networkPanelOpen;
            if( networkPanelOpen ) {
                menu.addChild(network, 9, 0);
            } else {
                menu.removeChild(network);
            }
        }
    }
    
    private class StartMulti implements Command<Button> {
        public void execute( Button source ) {
            try {
                String player = userField.getText();
                String host = hostField.getText();
                int port = Integer.parseInt(portField.getText()); 
                ConnectionState cs = new ConnectionState(host, port, player);
                startSound.playInstance();
                getStateManager().attach(cs);
                setEnabled(false);
            } catch( Exception e ) {
                log.error("Connection Error", e);
                
                // Play the error sound and pop-up an error window
                errorSound.playInstance();
                ErrorState error = new ErrorState("Connection Error", e.getMessage(), "dungeon");
                getStateManager().attach(error);                
            }
        }
    }

    private class Exit implements Command<Button> {
        public void execute( Button source ) {
            exitSound.playInstance();
            try {
                Thread.sleep(3000);
            } catch( InterruptedException e ) {
            }
            getApplication().stop();
        }
    }
}
