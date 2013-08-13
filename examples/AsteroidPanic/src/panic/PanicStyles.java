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

import com.jme3.math.ColorRGBA;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.component.TbtQuadBackgroundComponent;
import com.simsilica.lemur.style.Attributes;
import com.simsilica.lemur.style.Styles;


/**
 *  Sets up some default styles used by the Asteroid Panic
 *  screens.
 *
 *  @author    Paul Speed
 */
public class PanicStyles {

    public static final String TITLE_ID = "title";
    public static final String MESSAGE_ID = "message";
    public static final String MENU_ID = "menu";
    public static final String MENU_TITLE_ID = "menu.title";

    public static void initializeStyles( Styles styles ) {

        // Set the message font size to be the same (by default) for any
        // any style.
        Attributes title = styles.getSelector(TITLE_ID, null);
        title.set("fontSize", 48f);
        title.set("textHAlignment", HAlignment.CENTER);

        Attributes message = styles.getSelector(MESSAGE_ID, null);
        message.set("fontSize", 32f);

        // Then the "retro"-specific styles

        // Common background for some panels and buttons
        TbtQuadBackgroundComponent border
            = TbtQuadBackgroundComponent.create("/com/simsilica/lemur/icons/border.png",
                                                1, 2, 2, 3, 3, 0, false);

        Attributes menu = styles.getSelector(MENU_ID, "retro");
        border.setColor(ColorRGBA.Blue);
        menu.set("background", border.clone());

        Attributes menuTitle = styles.getSelector(MENU_TITLE_ID, "retro");
        menuTitle.set("background", border.clone());
        menuTitle.set("insets", new Insets3f(1, 1, 10, 1));
        menuTitle.set("fontSize", 48f); // would inherit from regular title but I feel
                                        // like being specific

        Attributes button = styles.getSelector(Button.ELEMENT_ID, "retro");
        border.setColor(ColorRGBA.Cyan);
        button.set("background", border.clone());
        button.set("insets", new Insets3f(1, 5, 10, 5));
        button.set("textHAlignment", HAlignment.CENTER);
        button.set("fontSize", 32f);
    }
}
