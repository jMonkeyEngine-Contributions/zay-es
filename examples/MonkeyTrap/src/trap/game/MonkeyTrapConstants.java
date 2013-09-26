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

package trap.game;

import trap.game.ai.AiType;


/**
 *
 *  @author    Paul Speed
 */
public class MonkeyTrapConstants {
 
    public static final String E_MONKEY = "Monkey";
    public static final String E_OGRE = "Ogre";
    public static final String E_BLING = "Bling";
    public static final String E_HEALTH = "Health";
    public static final String E_BARREL = "Barrel";
    public static final String E_BANANA = "Banana";
    public static final String E_CHEST = "Chest";
    
    public static final double MONKEY_MOVE_SPEED = 4.0; // m/sec
    public static final double OGRE_MOVE_SPEED = 3.0; // m/sec
    public static final double MONKEY_TURN_SPEED = 2.5; // rotations/sec, 90 degrees in 100 ms
    public static final double OGRE_TURN_SPEED = 1.25; // rotations/sec, 90 degrees in 200 ms
 
    public static final int MONKEY_HITPOINTS = 25;
    public static final int OGRE_HITPOINTS = 15;
    public static final int CHEST_HITPOINTS = 5;
    public static final int BARREL_HITPOINTS = 1;
        
    public static final Speed SPEED_MONKEY = new Speed(MONKEY_MOVE_SPEED, MONKEY_TURN_SPEED);
    public static final Speed SPEED_OGRE = new Speed(OGRE_MOVE_SPEED, OGRE_TURN_SPEED);
    
    public static final ModelType TYPE_MONKEY = new ModelType("Monkey");
    public static final ModelType TYPE_OGRE = new ModelType("Ogre");
    public static final ModelType TYPE_BARRELS = new ModelType("Barrel");
    public static final ModelType TYPE_CHEST = new ModelType("Chest");
    public static final ModelType TYPE_BANANA = new ModelType("Banana");
    public static final ModelType TYPE_BLING = new ModelType("Bling");
    
    public static final AiType AI_DRUNK = new AiType("Drunk");
    public static final AiType AI_SURVEY = new AiType("Survey");
}
