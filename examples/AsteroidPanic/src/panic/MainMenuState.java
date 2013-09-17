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

package panic;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.event.BaseAppState;
import com.simsilica.lemur.style.ElementId;


/**
 *  Lays out the main menu controls and handles the clicks.
 *  When enabled/disabled the main menu is shown/hidden.
 *
 *  @author    Paul Speed
 */
public class MainMenuState extends BaseAppState {

    private Container menu;

    private AudioNode selectUp;
    private AudioNode selectDown;
    private AudioNode selectNeutral;

    private AudioNode music;

    public MainMenuState() {
    }

    @Override
    protected void initialize( Application app ) {

        menu = new Container(new SpringGridLayout(), new ElementId(PanicStyles.MENU_ID), "retro");

        menu.addChild(new Label("Asteroid Panic", new ElementId(PanicStyles.MENU_TITLE_ID), "retro"));

        Button start = menu.addChild(new Button("Start Game", "retro"));
        start.addClickCommands(new Start());
        start.addCommands(Button.ButtonAction.HighlightOn, new Highlight());

        Button exit = menu.addChild(new Button("Exit", "retro"));
        exit.addClickCommands(new Exit());
        exit.addCommands(Button.ButtonAction.HighlightOn, new Highlight());

        Camera cam = app.getCamera();
        float menuScale = cam.getHeight()/720f;

        Vector3f pref = menu.getPreferredSize();
        menu.setLocalTranslation(cam.getWidth() * 0.5f - pref.x * 0.5f * menuScale,
                                 cam.getHeight() * 0.75f + pref.y * 0.5f * menuScale,
                                 10);
        menu.setLocalScale(menuScale);

        AssetManager assets = app.getAssetManager();
        selectUp = new AudioNode(assets, "Sounds/select-up.ogg", false);
        selectUp.setReverbEnabled(false);
        selectUp.setPositional(false);
        selectDown = new AudioNode(assets, "Sounds/select-down.ogg", false);
        selectDown.setReverbEnabled(false);
        selectDown.setPositional(false);
        selectNeutral = new AudioNode(assets, "Sounds/select-neutral.ogg", false);
        selectNeutral.setReverbEnabled(false);
        selectNeutral.setPositional(false);

    }

    @Override
    protected void cleanup( Application app ) {
    }

    @Override
    public void update( float tpf ) {
        startMusic();
    }

    protected void startMusic() {
        if( music == null || music.getStatus() == AudioSource.Status.Stopped ) {
            AssetManager assets = getApplication().getAssetManager();
            music = new AudioNode(assets, "Sounds/panic-menu-theme.ogg", true);
            music.setReverbEnabled(false);
            music.setPositional(false);
            music.play();
        }
    }

    protected void stopMusic() {
        if( music != null ) {
            music.stop();
            music = null;
        }
    }

    @Override
    protected void enable() {
        Main main = (Main)getApplication();
        main.getGuiNode().attachChild(menu);
        startMusic();
    }

    @Override
    protected void disable() {
        menu.removeFromParent();
        stopMusic();
    }

    private class Highlight implements Command<Button> {
        public void execute( Button source ) {
            selectNeutral.playInstance();
        }
    }

    private class Start implements Command<Button> {
        public void execute( Button source ) {
            selectUp.playInstance();
            getStateManager().attach(new SinglePlayerState());
            setEnabled(false);
        }
    }

    private class Exit implements Command<Button> {
        public void execute( Button source ) {
            selectDown.playInstance();
            getApplication().stop();
        }
    }
}
