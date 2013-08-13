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

    public MainMenuState() {
    }

    @Override
    protected void initialize( Application app ) {

        menu = new Container(new SpringGridLayout(), new ElementId(PanicStyles.MENU_ID), "retro");

        menu.addChild(new Label("Asteroid Panic", new ElementId(PanicStyles.MENU_TITLE_ID), "retro"));

        Button start = menu.addChild(new Button("Start Game", "retro"));
        start.addClickCommands(new Start());

        Button exit = menu.addChild(new Button("Exit", "retro"));
        exit.addClickCommands(new Exit());

        Camera cam = app.getCamera();
        float menuScale = cam.getHeight()/720f;

        Vector3f pref = menu.getPreferredSize();
        menu.setLocalTranslation(cam.getWidth() * 0.5f - pref.x * 0.5f * menuScale,
                                 cam.getHeight() * 0.75f + pref.y * 0.5f * menuScale,
                                 10);
        menu.setLocalScale(menuScale);
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

    private class Start implements Command<Button> {
        public void execute( Button source ) {
            getStateManager().attach(new SinglePlayerState());
            setEnabled(false);
        }
    }

    private class Exit implements Command<Button> {
        public void execute( Button source ) {
            getApplication().stop();
        }
    }
}
