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

import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.audio.Environment;
import com.jme3.audio.Listener;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.input.InputMapper;
import com.simsilica.lemur.style.ElementId;
import com.simsilica.lemur.style.Styles;
import trap.anim.SpatialTaskFactories;
import trap.anim.SpatialTaskFactory;
import trap.filter.PostProcessingState;
import trap.game.Maze;
import trap.game.SensorArea;
import trap.game.TimeProvider;
import trap.task.Task;
import trap.task.TaskStatus;
import trap.task.Tasks;

/**
 *
 *  @author Paul Speed
 */
public class AnimationTester extends SimpleApplication {

    private Quaternion cameraAngle;
    private Vector3f cameraDelta;
    private Vector3f audioDelta;
    private float cameraDistance = 10; //20; //12;
    private Node interpNode;
    
    private Listener audioListener = new Listener();
    private TimeProvider timeProvider = new GameTimeProvider();

    private int xMain;
    private int yMain;
        
    private Spatial monkey;
    private Spatial ogre;
    private Spatial barrels1;
    private Spatial chest1;
    private Spatial barrels2;
    private Spatial chest2;

    public static void main(String[] args) {
        AnimationTester app = new AnimationTester();
        app.start();
    }

    public AnimationTester() {
        super( new StatsAppState(),
               new ScreenshotAppState("", System.currentTimeMillis()),
               new PostProcessingState() );
    }

    @Override
    public void simpleInitApp() {

        // Initialize the Lemur helper instance
        GuiGlobals.initialize(this);
        AnimationFactories.initialize(assetManager);
        Effects.initialize(timeProvider, assetManager);

        // Move this to an audio manager state 
        getAudioRenderer().setListener(audioListener);
        
        // Setup the audio environment... here for now              
        getAudioRenderer().setEnvironment(Environment.Closet);
        

        // Setup default key mappings
        PlayerFunctions.initializeDefaultMappings(GuiGlobals.getInstance().getInputMapper());

        // Setup the dungeon style for our HUD and GUI elements
        Styles styles = GuiGlobals.getInstance().getStyles();
        DungeonStyles.initializeStyles(styles);
        
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.addDelegate(PlayerFunctions.F_SCREENSHOT, 
                                this, 
                                "takeScreenshot" );
 
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
                               
System.out.println( "Creating maze..." );                                
        // Setup a scene to test animations
        Maze maze = new Maze(16, 16);
        maze.setSeed(0);
System.out.println( "Generating maze..." );                                
        maze.generate();       
        xMain = maze.getXSeed();
        yMain = maze.getYSeed();
        maze.getCells()[xMain-1][yMain+1] = 0;
        maze.getCells()[xMain+1][yMain+1] = 0;
        maze.getCells()[xMain+1][yMain-1] = 0;
        MazeState mazeState = new MazeState(maze);
        stateManager.attach(mazeState);
        
        for( int x = 0; x < 16; x++ ) {
            for( int y = 0; y < 16; y++ ) {
                mazeState.setVisited(x, y);
            }
        }
 
System.out.println( "Creating sensor area..." );                                
        SensorArea sensor = new SensorArea(maze, xMain, yMain, 4);
System.out.println( "Setting visibility..." );                                
        mazeState.setVisibility(sensor, MazeState.PLAYER_VISIBLE); 
 
        // Setup a camera position and angle       
        cameraAngle = new Quaternion().fromAngles(FastMath.QUARTER_PI * 1.3f, FastMath.PI, 0);
        cameraDelta = cameraAngle.mult(Vector3f.UNIT_Z);
        cameraDelta.multLocal(-cameraDistance);
        cameraDelta.addLocal(0, -1, 0);
        cam.setRotation(cameraAngle);

        audioListener.setRotation(cameraAngle);
        audioDelta = cameraAngle.mult(Vector3f.UNIT_Z);
        audioDelta.multLocal(4);        

        // Until we need to attach it to an actor
        interpNode = new Node("camera follow");
        interpNode.setLocalTranslation(xMain * 2, 0, yMain * 2);
 
System.out.println( "Creating actors..." ); 
        // Some characters to animate
        TrapModelFactory factory = new TrapModelFactory(assetManager, audioListener, timeProvider); 
        monkey = factory.createMonkey();
        monkey.setLocalTranslation(xMain * 2, 0, yMain * 2);
        monkey.setLocalRotation(new Quaternion().fromAngles(0, FastMath.HALF_PI, 0));
        rootNode.attachChild(monkey);
           
        ogre = factory.createOgre();  
        ogre.setLocalTranslation((xMain + 1)* 2, 0, yMain * 2);   
        ogre.setLocalRotation(new Quaternion().fromAngles(0, -FastMath.HALF_PI, 0));
        rootNode.attachChild(ogre);
 
        barrels1 = factory.createBarrels();
        barrels1.setLocalTranslation((xMain-1) * 2, 0, (yMain-1)*2);
        rootNode.attachChild(barrels1);

        chest1 = factory.createChest();
        chest1.setLocalTranslation((xMain-1) * 2, 0, (yMain+1)*2);
        rootNode.attachChild(chest1);

        barrels2 = factory.createBarrels();
        barrels2.setLocalTranslation((xMain+1) * 2, 0, (yMain+1)*2);
        rootNode.attachChild(barrels2);

        chest2 = factory.createChest();
        chest2.setLocalTranslation((xMain+1) * 2, 0, (yMain-1)*2);
        rootNode.attachChild(chest2);
        
System.out.println( "Creating menu..." ); 
        createMenus();   
    }
 
    protected void createMenus() {
        
        Container monkeyControls = createMenu("Monkey Controls", monkey);
        monkeyControls.setLocalTranslation( 10, cam.getHeight() - 10, 0 );
        monkeyControls.setLocalScale(0.6f);        
        guiNode.attachChild(monkeyControls);

        Container ogreControls = createMenu("Ogre Controls", ogre);
        Vector3f pref = ogreControls.getPreferredSize();
        ogreControls.setLocalTranslation( cam.getWidth() - 10 - (pref.x * 0.6f), cam.getHeight() - 10, 0 );
        ogreControls.setLocalScale(0.6f);        
        guiNode.attachChild(ogreControls);
        
        Container deathMenu = createDeathMenu();
        pref = monkeyControls.getPreferredSize();
        deathMenu.setLocalTranslation(10, cam.getHeight() - (pref.y * 0.6f) - 20, 0);
        deathMenu.setLocalScale(0.6f);        
        guiNode.attachChild(deathMenu);
    }
    
    protected Container createMenu( String name, Spatial actor ) {
       
        Container menu = new Container(new SpringGridLayout(), 
                                                 new ElementId(DungeonStyles.MENU_ID), 
                                                 "dungeon");       
        Label title = menu.addChild(new Label(name, 
                                              new ElementId(DungeonStyles.MENU_TITLE_ID), 
                                              "dungeon"));
        title.setBackground(null);   
        title.setColor(ColorRGBA.Black);
        title.setShadowColor(ColorRGBA.White);
        title.setShadowOffset(new Vector3f(2, -2, 0.1f));                                            
        title.setFontSize(40);
         
        Button attack = menu.addChild(new Button("Attack", "dungeon"));
        attack.setShadowOffset(new Vector3f(2, -2, 0.1f));                                            
        attack.setShadowColor(ColorRGBA.Black);
        attack.addClickCommands(new AttackCommand(actor));

        Button dodge = menu.addChild(new Button("Dodge", "dungeon"));
        dodge.setShadowOffset(new Vector3f(2, -2, 0.1f));                                            
        dodge.setShadowColor(ColorRGBA.Black);
        dodge.addClickCommands(new DodgeCommand(actor));

        Button attackDodge = menu.addChild(new Button("Attack+Dodge", "dungeon"));
        attackDodge.setShadowOffset(new Vector3f(2, -2, 0.1f));                                            
        attackDodge.setShadowColor(ColorRGBA.Black);
        attackDodge.addClickCommands(new AttackDodgeCommand(actor));

        Button death = menu.addChild(new Button("Death", "dungeon"));
        death.setShadowOffset(new Vector3f(2, -2, 0.1f));                                            
        death.setShadowColor(ColorRGBA.Black);
        death.addClickCommands(new DeathCommand(actor));
 
        return menu;     
    }

    protected Container createDeathMenu() {
       
        Container menu = new Container(new SpringGridLayout(), 
                                                 new ElementId(DungeonStyles.MENU_ID), 
                                                 "dungeon");       
        Label title = menu.addChild(new Label("Destroy", 
                                              new ElementId(DungeonStyles.MENU_TITLE_ID), 
                                              "dungeon"));
        title.setBackground(null);   
        title.setColor(ColorRGBA.Black);
        title.setShadowColor(ColorRGBA.White);
        title.setShadowOffset(new Vector3f(2, -2, 0.1f));                                            
        title.setFontSize(40);
         
        Button destroy;
        destroy = menu.addChild(new Button("Barrels 1", "dungeon"));
        destroy.setShadowOffset(new Vector3f(2, -2, 0.1f));                                            
        destroy.setShadowColor(ColorRGBA.Black);
        destroy.addClickCommands(new DeathCommand(barrels1));

        destroy = menu.addChild(new Button("Barrels 2", "dungeon"));
        destroy.setShadowOffset(new Vector3f(2, -2, 0.1f));                                            
        destroy.setShadowColor(ColorRGBA.Black);
        destroy.addClickCommands(new DeathCommand(barrels2));

        destroy = menu.addChild(new Button("Chest 1", "dungeon"));
        destroy.setShadowOffset(new Vector3f(2, -2, 0.1f));                                            
        destroy.setShadowColor(ColorRGBA.Black);
        destroy.addClickCommands(new DeathCommand(chest1));

        destroy = menu.addChild(new Button("Chest 2", "dungeon"));
        destroy.setShadowOffset(new Vector3f(2, -2, 0.1f));                                            
        destroy.setShadowColor(ColorRGBA.Black);
        destroy.addClickCommands(new DeathCommand(chest2));
 
        return menu;     
    }
    
    public void takeScreenshot() {
        System.out.println( "Taking screen shot..." );       
        stateManager.getState(ScreenshotAppState.class).takeScreenshot();    
    }

Task test;
double time = 0;

    @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code
        
        Vector3f loc = cam.getLocation();
        loc.set(interpNode.getLocalTranslation());
        loc.addLocal(cameraDelta);
        cam.setLocation(loc);
 
        loc = audioListener.getLocation(); 
        loc.set(interpNode.getLocalTranslation());
        loc.addLocal(audioDelta);
        audioListener.setLocation(loc);
  
        if( test != null ) {
            if( test.execute(tpf) == TaskStatus.Done ) {
                test = null;
                
                // And reset both to idle state just in case
                monkey.getControl(CharAnimControl.class).reset();
                ogre.getControl(CharAnimControl.class).reset();
                time = 0;                
            }
        } else {
            if( monkey.getParent() == null ) {
                time += tpf;
                if( time > 2 ) {
                    rootNode.attachChild(monkey);
                }                    
            }
            if( ogre.getParent() == null ) {
                time += tpf;
                if( time > 2 ) {
                    rootNode.attachChild(ogre);
                }                    
            }        
            if( barrels1.getParent() == null ) {
                time += tpf;
                if( time > 2 ) {
                    rootNode.attachChild(barrels1);
                }                    
            }
            if( chest1.getParent() == null ) {
                time += tpf;
                if( time > 2 ) {
                    rootNode.attachChild(chest1);
                }                    
            }
            if( barrels2.getParent() == null ) {
                time += tpf;
                if( time > 2 ) {
                    rootNode.attachChild(barrels2);
                }                    
            }
            if( chest2.getParent() == null ) {
                time += tpf;
                if( time > 2 ) {
                    rootNode.attachChild(chest2);
                }                    
            }
        }       
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
 
    protected Task createAttack( Spatial actor ) {
        // Start needs to be provided because the actor may
        // be in between "gigs" when we query it's position.
        // In the real game this actually may not ever matter...
        // but if everything is always done relative then I worry
        // it never self-corrects.  Though I guess regular movement
        // will fix it.
        Vector3f start = new Vector3f();
        if( actor == monkey ) {
            start.x = xMain * 2;
            start.z = yMain * 2;
        } else {
            start.x = (xMain+1) * 2;
            start.z = yMain * 2;
        }
 
        if( actor == monkey ) {              
            return AnimationFactories.createMonkeyAttack(actor, start);
        } else if( actor == ogre ) {
            return AnimationFactories.createOgreAttack(actor, start);
        }
        return null;          
    }

    protected Task createMiss( Spatial actor ) {
        // Start needs to be provided because the actor may
        // be in between "gigs" when we query it's position.
        // In the real game this actually may not ever matter...
        // but if everything is always done relative then I worry
        // it never self-corrects.  Though I guess regular movement
        // will fix it.
        Vector3f start = new Vector3f();
        if( actor == monkey ) {
            start.x = xMain * 2;
            start.z = yMain * 2;
        } else {
            start.x = (xMain+1) * 2;
            start.z = yMain * 2;
        }
 
        if( actor == monkey ) {              
            return AnimationFactories.createMonkeyMiss(actor, start);
        } else if( actor == ogre ) {
            return AnimationFactories.createOgreMiss(actor, start);
        }
        return null;          
    }
    
    protected Task createDefend( Spatial actor ) {
        Vector3f start = new Vector3f();
        if( actor == monkey ) {
            start.x = xMain * 2;
            start.z = yMain * 2;
        } else {
            start.x = (xMain+1) * 2;
            start.z = yMain * 2;
        }
               
        SpatialTaskFactory factory;
        if( actor == monkey ) {              
            factory = SpatialTaskFactories.callMethod(AnimationFactories.class, "createMonkeyDefend");       
            //return AnimationFactories.createMonkeyDefend(actor, start);
        } else if( actor == ogre ) {
            factory = SpatialTaskFactories.callMethod(AnimationFactories.class, "createOgreDefend");       
            //return AnimationFactories.createOgreDefend(actor, start);
        } else {
            return null;
        }          
        return factory.createTask(actor, start); 
    }

    protected Task createDeath( Spatial actor ) {
        SpatialTaskFactory factory;
        
        if( actor == monkey ) {
            factory = SpatialTaskFactories.callMethod(AnimationFactories.class, "createMonkeyDeath");       
            //return factory.createTask(actor, null); //AnimationFactories.createMonkeyDeath(actor);
        } else if( actor == ogre ) {
            factory = SpatialTaskFactories.callMethod(AnimationFactories.class, "createOgreDeath");       
            //return AnimationFactories.createOgreDeath(actor);
        } else if( actor == barrels1 || actor == barrels2 ) {
            factory = SpatialTaskFactories.callMethod(AnimationFactories.class, "createBarrelDeath");       
            //return AnimationFactories.createBarrelDeath(actor);
        } else if( actor == chest1 || actor == chest2 ) {
            factory = SpatialTaskFactories.callMethod(AnimationFactories.class, "createChestDeath");       
            //return AnimationFactories.createChestDeath(actor);
        } else {
            return null;
        }
        return factory.createTask(actor, null); 
    }
    
    private class AttackCommand implements Command<Button> {
        
        private Spatial actor;
        
        public AttackCommand( Spatial actor ) {
            this.actor = actor;
        }
        
        public void execute( Button source ) {
            System.out.println( actor + " attack" );
 
            if( actor.getParent() == null ) {
                return;
            }        
            test = createAttack(actor);
        }
    }

    private class DodgeCommand implements Command<Button> {
        
        private Spatial actor;
        
        public DodgeCommand( Spatial actor ) {
            this.actor = actor;
        }
        
        public void execute( Button source ) {            
            System.out.println( actor + " dodge" );
            
            if( actor.getParent() == null ) {
                return;
            }        
            test = createDefend(actor);
        }
    }

    private class AttackDodgeCommand implements Command<Button> {
        
        private Spatial actor;
        
        public AttackDodgeCommand( Spatial actor ) {
            this.actor = actor;
        }
        
        public void execute( Button source ) {
            System.out.println( actor + " attack" );
            
            if( monkey.getParent() == null ) {
                return;
            }        
            if( ogre.getParent() == null ) {
                return;
            }        
            Task task1 = createMiss(actor);
            Task task2 = createDefend(actor == ogre ? monkey : ogre);
            test = Tasks.compose(task1, task2);
        }
    }

    private class DeathCommand implements Command<Button> {
        
        private Spatial actor;
        
        public DeathCommand( Spatial actor ) {
            this.actor = actor;
        }
        
        public void execute( Button source ) {            
            System.out.println( actor + " death" );            
            if( actor.getParent() == null ) {
                return;
            }        
            test = createDeath(actor);
        }
    }
    
    private class GameTimeProvider implements TimeProvider {    
        private long start = System.nanoTime();
    
        public long getTime() {
            return System.nanoTime() - start;           
        }  
    }
    
}
