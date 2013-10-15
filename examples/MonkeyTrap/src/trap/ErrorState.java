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
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.event.BaseAppState;
import com.simsilica.lemur.event.ConsumingMouseListener;
import com.simsilica.lemur.style.ElementId;


/**
 *  State that takes over the screen and pops up an
 *  error dialog.
 *
 *  @author    Paul Speed
 */
public class ErrorState extends BaseAppState {
 
    public static final ElementId POPUP_ID = new ElementId("error.popup");
    public static final ElementId TITLE_ID = new ElementId("error.title");
    public static final ElementId MESSAGE_ID = new ElementId("error.message");
    public static final ElementId CLOSE_ID = new ElementId("error.close.button");
 
    private Container blocker;
    private Container popup;   
    private String title;
    private String message;
    private String style;
    
    public ErrorState( String title, String message, String style ) {
        this.title = title;
        this.message = message;
        this.style = style;
    }

    @Override
    protected void initialize(Application app) {
 
        blocker = new Container();
        blocker.setBackground(new QuadBackgroundComponent(new ColorRGBA(0, 0, 0, 0.5f)));         
        blocker.addMouseListener(ConsumingMouseListener.INSTANCE);       
 
        popup = new Container(new BorderLayout(), POPUP_ID, style);        
        popup.addChild(new Label(title, TITLE_ID, style), BorderLayout.Position.North);                      
        popup.addChild(new Label(message, MESSAGE_ID, style), BorderLayout.Position.Center);                      
        Button ok = popup.addChild(new Button("Ok", CLOSE_ID, style), BorderLayout.Position.South);
        ok.addClickCommands(new Close());
        
        Camera cam = app.getCamera();
        
        blocker.setPreferredSize(new Vector3f(cam.getWidth(), cam.getHeight(), 0));
        blocker.setLocalTranslation(0, cam.getHeight(), 99);
        
        float menuScale = cam.getHeight()/720f;
        popup.setLocalScale(menuScale);
        
        Vector3f pref = popup.getPreferredSize();
        pref.x = Math.max(pref.x, 600);
        pref.y = Math.max(pref.y, 400);
        popup.setPreferredSize(pref);
        popup.setLocalTranslation(cam.getWidth() * 0.5f - menuScale * pref.x * 0.5f, 
                                  cam.getHeight() * 0.5f + menuScale * pref.y * 0.5f, 100);                       
    }

    @Override
    protected void cleanup(Application app) {
    }

    @Override
    protected void enable() {
        ((SimpleApplication)getApplication()).getGuiNode().attachChild(blocker);
        ((SimpleApplication)getApplication()).getGuiNode().attachChild(popup);
    }

    @Override
    protected void disable() {
        blocker.removeFromParent();
        popup.removeFromParent();
    }
    
    private class Close implements Command<Button> {
        public void execute( Button source ) {
            getStateManager().detach(ErrorState.this);
        }
    }
}
