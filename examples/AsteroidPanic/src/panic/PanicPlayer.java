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

import com.simsilica.lemur.core.VersionedHolder;
import com.simsilica.lemur.core.VersionedReference;


/**
 *  The state of the player: level, score, etc.
 *
 *  @author    Paul Speed
 */
public class PanicPlayer {

    private VersionedHolder<Integer> level = new VersionedHolder<>(1);
    private VersionedHolder<Integer> score = new VersionedHolder<>(0);
    private VersionedHolder<Integer> shipsRemaining = new VersionedHolder<>(0);

    private boolean invincible = true;
    private boolean dead = false;

    public PanicPlayer( int ships ) {
        shipsRemaining.setObject(ships);
    }

    public void setInvincible( boolean f ) {
        this.invincible = f;
    }

    public boolean isInvincible() {
        return invincible;
    }

    public void setDead( boolean f ) {
        this.dead = f;
    }

    public boolean isDead() {
        return dead;
    }

    public void addLevel( int add ) {
        level.setObject(level.getObject() + add);
    }

    public int getLevel() {
        return level.getObject();
    }

    public void addScore( int add ) {
        score.setObject(score.getObject() + add);
    }

    public int getScore() {
        return score.getObject();
    }

    public void addShipsRemaining( int add ) {
        shipsRemaining.setObject(shipsRemaining.getObject() + add);
    }

    public int getShipsRemaining() {
        return shipsRemaining.getObject();
    }

    public VersionedReference<Integer> getLevelRef() {
        return level.createReference();
    }

    public VersionedReference<Integer> getScoreRef() {
        return score.createReference();
    }

    public VersionedReference<Integer> getShipsRemainingRef() {
        return shipsRemaining.createReference();
    }
}


