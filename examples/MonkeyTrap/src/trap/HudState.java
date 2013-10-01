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
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.simsilica.es.Entity;
import com.simsilica.es.Name;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.IconComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.event.BaseAppState;
import java.util.ArrayList;
import java.util.List;
import trap.game.ArmorStrength;
import trap.game.CombatStrength;
import trap.game.HitPoints;
import trap.game.MaxHitPoints;


/**
 *
 *  @author    Paul Speed
 */
public class HudState extends BaseAppState {

    private Node hudRoot = new Node("HUD");
    private Container playerPanel;
    private Container topRow;
    private Label playerName;
    private Label health;
    private Container combatStats;
    private List<IconComponent> statIcons = new ArrayList<IconComponent>();
    private List<Label> stats = new ArrayList<Label>();
    private IconComponent[] icons;
    
    private Entity player;
    private boolean dirty;
    
    private int lastHP = 100;
    private CombatStrength lastCombat;
    private ArmorStrength lastArmor; 

    @Override
    protected void initialize( Application app ) {
    
        playerPanel = new Container();
        playerPanel.setInsets(new Insets3f(5, 5, 5, 5));
 
        topRow = playerPanel.addChild(new Container(new SpringGridLayout(Axis.X, Axis.Y, FillMode.NONE, FillMode.NONE)));
               
        playerName = topRow.addChild(new Label("Player"));
        playerName.setInsets(new Insets3f(0, 5, 0, 10));
        
        Label healthIcon = topRow.addChild(new Label(""));                
        health = topRow.addChild(new Label("100%"));         
         
        combatStats = playerPanel.addChild(new Container(new SpringGridLayout(Axis.X, Axis.Y, FillMode.NONE, FillMode.NONE)));
                
        IconComponent icon = new IconComponent( "Interface/health-icon.png", 0.5f, 0, 0, 0.01f, false );
        healthIcon.setBackground(icon);
 
        icons = new IconComponent[] {
            new IconComponent( "Interface/attack-icon.png", 0.75f, 0, 0, 0.01f, false ),
            new IconComponent( "Interface/defense-icon.png", 0.75f, 0, 0, 0.01f, false ),
            new IconComponent( "Interface/damage-icon.png", 0.75f, 0, 0, 0.01f, false ),
            new IconComponent( "Interface/armor-icon.png", 0.75f, 0, 0, 0.01f, false )
        };
        
        Camera cam = app.getCamera();
        playerPanel.setLocalTranslation(0, cam.getHeight(), 0);
        
        
        hudRoot.attachChild(playerPanel);
    }

    public void setPlayer( Entity player ) {
        this.player = player;
        lastCombat = player.get(CombatStrength.class);        
        lastArmor = player.get(ArmorStrength.class);
        updatePlayer();
    }
    
    public void updatePlayer() {
        dirty = true;
    }
 
    protected int addIcons( int start, IconComponent icon, int count ) {
        for( int i = 0; i < count; i++ ) {
            int index = start + i;
            if( index < statIcons.size() ) {
                if( statIcons.get(index) == icon ) {
                    continue;
                }
//System.out.println( "Setting " + index + " to icon:" + icon );                
                statIcons.set(index, icon);
                stats.get(index).setBackground(icon.clone());
            } else {
//System.out.println( "Adding " + index + " as icon:" + icon );                
                // Need to expand
                statIcons.add(icon);
                Label l = new Label("");
                stats.add(l);
                l.setBackground(icon.clone()); 
                combatStats.addChild(l);
            }
        }
        return start + count;       
    }
    
    protected void refreshPlayer() {
        dirty = false;
        
        playerName.setText(player.get(Name.class).getName());
         
        int hp = player.get(HitPoints.class).getHealth() * 100;
        hp = hp / player.get(MaxHitPoints.class).getMaxHealth();
        health.setText(hp + "%");
        if( hp > lastHP ) {
            // Play some bling
            Node playerNode = (Node)getState(ModelState.class).getSpatial(player.getId());
System.out.println( "Playing bling for node:" + playerNode );


            // All of these effects should really be part of a
            // general app state that watches all entities that might
            // trigger these effects.
            
            Effects.playBling(playerNode, -1, ColorRGBA.Yellow, new ColorRGBA(0.2f, 0.2f, 0.1f, 0)); 
        }
        lastHP = hp;
 
        int armorDelta = 0;
        int attackDelta = 0;
               
        // Update the buffs
        CombatStrength cs = player.get(CombatStrength.class);               
        ArmorStrength as = player.get(ArmorStrength.class);
        
        armorDelta += cs.getDefense() - lastCombat.getDefense();
        attackDelta += cs.getAttack() - lastCombat.getAttack();
        attackDelta += cs.getDamage() - lastCombat.getDamage();
        armorDelta += as.getStrength() - lastArmor.getStrength();
 
        if( armorDelta > 0 ) {
            Node playerNode = (Node)getState(ModelState.class).getSpatial(player.getId());
            Effects.playBling(playerNode, -1, ColorRGBA.Cyan, new ColorRGBA(0.1f, 0.2f, 0.2f, 0)); 
        }
        if( attackDelta > 0 ) {
            Node playerNode = (Node)getState(ModelState.class).getSpatial(player.getId());
            Effects.playBling(playerNode, -1, ColorRGBA.Red, new ColorRGBA(0.2f, 0.1f, 0.1f, 0)); 
        }
        
        lastCombat = cs;
        lastArmor = as;
        
        int index = 0;
        index = addIcons(index, icons[0], cs.getAttack());
        index = addIcons(index, icons[1], cs.getDefense());
        index = addIcons(index, icons[2], cs.getDamage());
        index = addIcons(index, icons[3], as.getStrength());
        
        // Not necessarily the most efficient way but it will
        // do for now.
        while( index < stats.size() ) {
//System.out.println( "Removing excess stats icons.");        
            Label l = stats.remove(stats.size()-1);
            statIcons.remove(statIcons.size()-1);
            combatStats.removeChild(l);
        }
    }

    @Override
    protected void cleanup( Application app ) {
    }

    @Override
    public void update( float tpf ) {
        if( dirty ) {
            refreshPlayer();
        }
    }

    @Override
    protected void enable() {
        ((SimpleApplication)getApplication()).getGuiNode().attachChild(playerPanel);
    }

    @Override
    protected void disable() {
        hudRoot.removeFromParent();
    }
}
