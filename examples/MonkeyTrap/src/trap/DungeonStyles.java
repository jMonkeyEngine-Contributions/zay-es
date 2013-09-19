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

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.component.TbtQuadBackgroundComponent;
import com.simsilica.lemur.core.GuiComponent;
import com.simsilica.lemur.style.Attributes;
import com.simsilica.lemur.style.Styles;


/**
 *  Sets up some default styles used by the Monkey Trap
 *  screens.
 *
 *  @author    Paul Speed
 */
public class DungeonStyles {

    public static final String TITLE_ID = "title";
    public static final String MESSAGE_ID = "message";
    public static final String MENU_ID = "menu";
    public static final String SUBMENU_ID = "submenu";
    public static final String MENU_TITLE_ID = "menu.title";
    public static final String EDIT_LABEL_ID = "edit.label";
    public static final String EDIT_FIELD_ID = "edit.field";

    public static void initializeStyles( Styles styles ) {

        // Set the message font size to be the same (by default) for any
        // any style.
        Attributes title = styles.getSelector(TITLE_ID, null);
        title.set("fontSize", 48f);
        title.set("textHAlignment", HAlignment.CENTER);

        Attributes message = styles.getSelector(MESSAGE_ID, null);
        message.set("fontSize", 32f);

        // Then the dungeon-specific styles

        // Common background for some panels and buttons
        GuiComponent stoneBevel = TbtQuadBackgroundComponent.create( "Interface/stone-512.jpg",
                                                                     1,
                                                                     10, 10, 502, 502,
                                                                     1f, false );
        

        Attributes menu = styles.getSelector(MENU_ID, "dungeon");
        menu.set("background", stoneBevel.clone());

        Attributes menuTitle = styles.getSelector(MENU_TITLE_ID, "dungeon");
        menuTitle.set("insets", new Insets3f(1, 1, 10, 1));
        menuTitle.set("fontSize", 48f); // would inherit from regular title but I feel
                                        // like being specific

        GuiComponent woodBevel = TbtQuadBackgroundComponent.create( "Interface/wood-bevel-512x256.jpg",
                                                                    1,
                                                                    10, 10, 502, 246,
                                                                    1f, false );
        GuiComponent stoneDown = TbtQuadBackgroundComponent.create( "Interface/stone-inner-bevel-128.png",
                                                                    1,
                                                                    5, 5, 123, 123,
                                                                    1f, false );

        Attributes button = styles.getSelector(Button.ELEMENT_ID, "dungeon");
        button.set("background", woodBevel.clone());
        button.set("insets", new Insets3f(1, 35, 10, 35));
        button.set("textHAlignment", HAlignment.CENTER);
        button.set("fontSize", 32f);
        
        
        Attributes subpanel = styles.getSelector(SUBMENU_ID, "dungeon");
        subpanel.set("insets", new Insets3f(1, 135, 10, 35)); 
        subpanel.set("background", stoneBevel.clone());
        
        Attributes editLabel = styles.getSelector(EDIT_LABEL_ID, "dungeon");
        editLabel.set("fontSize", 24f);
        editLabel.set("insets", new Insets3f(5, 5, 5, 5)); 
        editLabel.set("textHAlignment", HAlignment.RIGHT);
        //editLabel.set("color", new ColorRGBA(0, 0, 0.2f, 1)); 
        editLabel.set("color", ColorRGBA.White); 
        editLabel.set("shadowOffset", new Vector3f(1, -1, 0.1f)); 
        //editLabel.set("shadowColor", new ColorRGBA(0.8f, 0.9f, 0.9f, 1));
        editLabel.set("shadowColor", ColorRGBA.Black);  
        
        Attributes editField = styles.getSelector(TextField.ELEMENT_ID, "dungeon");
        editField.set("fontSize", 24f);
        editField.set("background", stoneDown.clone());
        
    }
}
