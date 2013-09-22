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

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityComponent;


/**
 *  Represents a position and orientation of an entity
 *  starting at a specific point in time.  
 *
 *  @author    Paul Speed
 */
public class Position implements EntityComponent {
    private Vector3f location;
    private Quaternion facing;
    private long startTime;
    private long endTime;

    public Position( Vector3f location, long startTime, long endTime ) {
        this(location, new Quaternion(), startTime, endTime);
    }

    public Position( Vector3f location, Direction facing, long startTime, long endTime ) {
        this(location, facing.getFacing(), startTime, endTime);
    }
    
    public Position( Vector3f location, Quaternion facing, long startTime, long endTime ) {
        this.location = location;
        this.facing = facing;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Position newDirection( Direction dir, long startTime, long endTime ) {
        return new Position(location, dir, startTime, endTime);
    }

    public Position newLocation( Vector3f location, long startTime, long endTime ) {
        return new Position(location, facing, startTime, endTime);
    }

    public long getTime() {
        return endTime;
    }

    public long getChangeTime() {
        return startTime;
    }

    public Vector3f getLocation() {
        return location;
    }

    public Quaternion getFacing() {
        return facing;
    }

    @Override
    public String toString() {
        return "Position[" + location + ", " + facing + ", at:" + endTime + "]";
    }
}


