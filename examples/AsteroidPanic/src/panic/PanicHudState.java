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
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.DynamicInsetsComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.event.BaseAppState;
import com.simsilica.lemur.style.ElementId;


/**
 *  Implements the in-game HUD that monitors the player state
 *  and updates displays for score, level, etc..  There are also
 *  a few message areas for general use by other code.
 *
 *  @author    Paul Speed
 */
public class PanicHudState extends BaseAppState {

    private Container hud;
    private Label score;
    private Label ships;
    private Label level;

    private Label titleMessage;
    private Label bottomMessage;

    private VersionedReference<Integer> scoreRef;
    private VersionedReference<Integer> shipsRef;
    private VersionedReference<Integer> levelRef;

    private PanicPlayer player;

    public PanicHudState() {
    }

    public void addTitle( String s ) {
        titleMessage.setText(titleMessage.getText() + "\n" + s);
    }

    public void setTitle( String s ) {
        titleMessage.setText(s);
    }

    public void setCurrentPlayer( PanicPlayer player ) {
        this.player = player;
        if( player == null ) {
            scoreRef = null;
            shipsRef = null;
            levelRef = null;
        } else {
            scoreRef = player.getScoreRef();
            shipsRef = player.getShipsRemainingRef();
            levelRef = player.getLevelRef();
        }

        if( isInitialized() ) {
            resetScore();
            resetShips();
            resetLevel();
        }
    }

    protected void resetScore() {
        if( scoreRef == null ) {
            score.setText("Score: 0");
        } else {
            score.setText("Score: " + scoreRef.get());
        }
    }

    protected void resetLevel() {
        if( levelRef == null ) {
            level.setText("Level: 1");
        } else {
            level.setText("Level: " + levelRef.get());
        }
    }

    protected void resetShips() {
        if( shipsRef == null ) {
            ships.setText("x3");
        } else {
            ships.setText("x" + shipsRef.get());
        }
    }

    @Override
    protected void initialize( Application app ) {

        hud = new Container(new BorderLayout());

        Container statsPanel = new Container(new SpringGridLayout(Axis.Y, Axis.X,
                                                                  FillMode.EVEN, FillMode.FORCED_EVEN));
        hud.addChild(statsPanel, BorderLayout.Position.North);
        statsPanel.setInsets(new Insets3f(2,5,0,5));

        score = statsPanel.addChild(new Label("Score: 0", "retro"));
        level = statsPanel.addChild(new Label("Level: 1", "retro"), 1);
        level.setInsetsComponent(new DynamicInsetsComponent(0, 0.5f, 0, 0.5f));
        ships = statsPanel.addChild(new Label("x3", "retro"), 2);
        ships.setInsetsComponent(new DynamicInsetsComponent(0, 1, 0, 0));

        titleMessage = hud.addChild(new Label("", new ElementId(PanicStyles.TITLE_ID), "retro"),
                                     BorderLayout.Position.Center);
        titleMessage.setInsetsComponent(new DynamicInsetsComponent(0.5f, 0.5f, 0.5f, 0.5f));

        bottomMessage = hud.addChild(new Label("", new ElementId(PanicStyles.MESSAGE_ID), "retro"),
                                     BorderLayout.Position.South);
        bottomMessage.setInsetsComponent(new DynamicInsetsComponent(0f, 0.5f, 0f, 0.5f));

        Camera cam = app.getCamera();
        hud.setPreferredSize(new Vector3f(cam.getWidth(), cam.getHeight(), 1));
        hud.setLocalTranslation(0, cam.getHeight(), 0);
    }

    @Override
    protected void cleanup( Application app ) {
    }

    @Override
    public void update( float tpf ) {
        if( levelRef == null ) {
            return;
        }

        if( levelRef.update() ) {
            resetLevel();
        }
        if( scoreRef.update() ) {
            resetScore();
        }
        if( shipsRef.update() ) {
            resetShips();
        }
    }

    @Override
    protected void enable() {
        Main main = (Main)getApplication();
        main.getGuiNode().attachChild(hud);
    }

    @Override
    protected void disable() {
        hud.removeFromParent();
    }
}
