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

import com.google.common.base.Objects;
import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import com.simsilica.lemur.event.BaseAppState;


/**
 *
 *  @author    Paul Speed
 */
public class MusicState extends BaseAppState
{
    private String song;
    private AudioNode music;
    private String playing;
    private float delay;
    private float volume = 1.0f;

    public MusicState() {
    }

    public void setSong( String song ) {
        setSong(song, 0);
    }
    
    public void setSong( String song, float delay ) {        
        if( Objects.equal(this.song, song) )
            return;
        this.delay = delay;
        this.song = song;
        resetSong();
    }

    public void setVolume( float volume ) {
        if( this.volume == volume ) {
            return;
        }
        this.volume = volume;
        if( music != null && music.getStatus() != AudioSource.Status.Stopped ) {
            music.setVolume(volume);
        } 
    }

    protected void resetSong() {
        if( delay > 0 ) {
            return;
        }
        
        if( song == null && music != null ) {
            music.stop();
            music = null;
            return;
        } else if( song == null ) {
            return;
        }
 
        // Need to see if we are playing the right song               
        if( music == null || music.getStatus() == AudioSource.Status.Stopped 
            || !Objects.equal(song, playing) ) {
            
            AssetManager assets = getApplication().getAssetManager();
            music = new AudioNode(assets, song, true);            
            music.setReverbEnabled(false);            
            music.setPositional(false);
            music.setVolume(volume);
            music.play();
            playing = song;
        }        
    }

    @Override
    protected void initialize(Application app) {
    }

    @Override
    protected void cleanup(Application app) {
    }

    @Override
    protected void enable() {
    }

    @Override
    protected void disable() {
    }
    
    @Override
    public void update( float tpf ) {
        delay -= tpf;
        resetSong();
    }
}
