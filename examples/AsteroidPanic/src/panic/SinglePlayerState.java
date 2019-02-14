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
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.Filters;


/**
 *  Keeps track of the single-player game state and
 *  transitions through the state machine as game conditions
 *  change.
 *
 *  @author    Paul Speed
 */
public class SinglePlayerState extends BaseAppState {

    protected enum GameState { LoadLevel, Starting, Joining, Playing, Death, EndLevel, GameOver };

    /*
        States break down as follows
        1) LoadLevel
            init: Setup asteroids for current player level
            update: show messages until done
                -go to Starting
        2) Starting
            init: reset ship, invincible and unmoveable
            update: show messages until done
                -go to Joining
        3) Joining
            init: reset ship, invincible, moveable
            update: when delay is done
                -go to Playing
        4) Playing
            init: ship invincible = false
            update: check for asteroids = 0
                        -go to EndLevel
                    check for death
                        -go to Death
        5) Death
            init: check for ships remaining
                    -go to Starting or GameOver
        6) EndLevel
            init:
            update: show messages until done
                -increment level
                -go to LoadLevel
        7) GameOver
            init: message
    */

    private EntityData ed;
    private EntitySet asteroids;

    private PanicPlayer player;
    private EntityId    ship;

    private GameState state = GameState.GameOver;

    // Book-keeping for cycled text messages
    private String[] titles;
    private int titleIndex;
    private long lastMessageTime;

    private AudioNode music;

    public SinglePlayerState() {
    }

    protected void setState( GameState state, String... titles ) {
        this.titles = titles;
        this.titleIndex = 0;
        if( state == this.state ) {
            return;
        }
        this.state = state;
        initState();
    }

    protected void initState() {
        switch( state ) {
            case LoadLevel:
                setupLevel();
                break;
            case Starting:
                resetShip(false, true);
                startMusic();
                break;
            case Joining:
                resetShip(true, true);
                break;
            case Playing:
                player.setInvincible(false);
                break;
            case Death:
                stopMusic();
                break;
            case EndLevel:
                stopMusic();
                break;
            case GameOver:
                stopMusic();
                break;
        }
    }

    protected void updateState( float tpf ) {

        switch( state ) {
            case LoadLevel:
                if( !rollMessage(2000) ) {
                    setState(GameState.Starting, "Ready...", "Set...", "Go!");
                }
                break;
            case Starting:
                if( !rollMessage(1000) ) {
                    setState(GameState.Joining, "");
                }
                break;
            case Joining:
                if( !rollMessage(2000) ) {
                    setState(GameState.Playing);
                }
                break;
            case Playing:
                if( player.isDead() ) {
                    setState(GameState.Death, "");
                } else if( asteroids.applyChanges() ) {
                    if( asteroids.isEmpty() ) {
                        setState(GameState.EndLevel, "Level Cleared");
                    }
                } else {
                    // Have to keep doing it so it loops
                    startMusic();
                }
                break;
            case Death:
                if( !rollMessage(2000) ) {
                    if( player.getShipsRemaining() <= 0 ) {
                        setState(GameState.GameOver, "GAME OVER");
                    } else {
                        player.addShipsRemaining(-1);
                        setState(GameState.Starting, "Ready...", "Set...", "Go!");
                    }
                }
                break;
            case EndLevel:
                if( !rollMessage(2000) ) {
                    player.addLevel(1);
                    setState(GameState.LoadLevel);
                }
                break;
            case GameOver:
                if( !rollMessage(2000) ) {
                    getState(MainMenuState.class).setEnabled(true);
                    getStateManager().detach(this);
                }
                break;
        }
    }

    protected boolean rollMessage(long delta) {

        // Check for no text set at all... in which
        // case there is never a delay
        if( titles.length == 0 ) {
            getState(PanicHudState.class).setTitle("");
            return false;
        }

        // See if it's time to roll to the new text
        long time = System.currentTimeMillis();
        if( time - lastMessageTime > delta ) {
            if( titleIndex < titles.length ) {
                lastMessageTime = time;
                getState(PanicHudState.class).setTitle(titles[titleIndex]);
            } else {
                getState(PanicHudState.class).setTitle("");
            }
            titleIndex++;
        }
        return titleIndex <= titles.length;
    }

    protected void resetShip( boolean mobile, boolean invincible ) {
        player.setInvincible(invincible);
        player.setDead(false);
        ed.setComponents(ship,
                         new ModelType(PanicModelFactory.MODEL_SHIP),
                         new Position(new Vector3f(), new Quaternion()),
                         new Velocity(new Vector3f(), new Vector3f()),
                         new Mass(mobile ? 0.1 : 0.0));
        getState(ShipControlState.class).setEnabled(mobile);
    }

    protected void setupLevel() {

        // Get rid of any existing asteroids
        // This happens if we start a new game after we
        // left the old game's asteroids bouncing around.
        asteroids.applyChanges();
        for( Entity e : asteroids ) {
            ed.removeEntity(e.getId());
        }

        int bigCount = Math.min(2 + player.getLevel(), 12);
        float minRadius = PanicConstants.maxAsteroidRadius;
        float maxRadius = PanicConstants.maxAsteroidRadius;
        float maxSpeed = 1;
        float maxRotation = 1;
        for( int i = 0; i < bigCount; i++ ) {

            EntityId test = ed.createEntity();

            Vector3f loc = new Vector3f((float)Math.random() * 12 - 6f,
                                        (float)Math.random() * 8 - 4f,
                                        0);
            Vector3f vel = new Vector3f((float)Math.random() * maxSpeed * 2 - maxSpeed,
                                        (float)Math.random() * maxSpeed * 2 - maxSpeed,
                                        0);
            float rot = (float)Math.random() * maxRotation * 2 - maxRotation;

            float radius = (float)(minRadius + Math.random() * (maxRadius - minRadius));

            // Asteroid and other entity creation really needs to be
            // consolidated.  Currently asteroid entities are created
            // here and in the PanicContactHandler.
            ed.setComponents(test,
                             new Position(loc, new Quaternion()),
                             new Velocity(vel, new Vector3f(0,0,rot)),
                             new CollisionShape(radius),
                             new Mass(radius),
                             new ModelType(PanicModelFactory.MODEL_ASTEROID));
            }
    }

    protected void startMusic() {
        if( music == null || music.getStatus() == AudioSource.Status.Stopped ) {
            AssetManager assets = getApplication().getAssetManager();
            music = new AudioNode(assets, "Sounds/panic-ambient.ogg", DataType.Stream);
            music.setReverbEnabled(false);
            music.setPositional(false);
            music.setVolume(0.6f);
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
    public void update( float tpf ) {
        updateState(tpf);
    }

    @Override
    protected void initialize( Application app ) {

        // The player has the ship they are playing and 2
        // extra in reserve to start with.
        this.player = new PanicPlayer(2);

        PanicContactHandler contactHandler
                = (PanicContactHandler)getState(CollisionState.class).getContactHandler();
        contactHandler.setPlayer( player );

        getState(PanicHudState.class).setCurrentPlayer(player);

        this.ed = getState(EntityDataState.class).getEntityData();
        asteroids = ed.getEntities(Filters.fieldEquals(ModelType.class, "type",
                                                       PanicModelFactory.MODEL_ASTEROID),
                                   ModelType.class);

        ship = ed.createEntity();
        ed.setComponents(ship,
                         new Position(new Vector3f(), new Quaternion()),
                         new Velocity(new Vector3f(), new Vector3f()),
                         new CollisionShape(0.1f),
                         new Mass(0.1),
                         new ModelType(PanicModelFactory.MODEL_SHIP));

        getStateManager().attach(new ShipControlState(ship));
        getState(ShipControlState.class).setEnabled(false);

        setState(GameState.LoadLevel, "PLAYER 1 UP");
    }

    @Override
    protected void cleanup( Application app ) {
        asteroids.release();
        asteroids = null;
        getStateManager().detach(getState(ShipControlState.class));
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }
}
