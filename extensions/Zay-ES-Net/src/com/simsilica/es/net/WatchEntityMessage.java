/*
 * $Id$
 * 
 * Copyright (c) 2015, Simsilica, LLC
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.simsilica.es.net;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import com.simsilica.es.EntityId;
import java.util.Arrays;

/**
 *
 *
 *  @author    Paul Speed
 */
@Serializable
public class WatchEntityMessage extends AbstractMessage {

    private int requestId;
    private int watchId;
    private EntityId entityId;
    private Class[] componentTypes;

    public WatchEntityMessage() {
    }
    
    public WatchEntityMessage( int requestId, int watchId, EntityId entityId, Class... components ) {
        this.requestId = requestId;
        this.watchId = watchId;
        this.entityId = entityId;
        this.componentTypes = components;
    }
 
    public int getRequestId() {
        return requestId;
    }
    
    public int getWatchId() {
        return watchId;
    }
 
    public EntityId getEntityId() {
        return entityId;
    }
 
    public Class[] getComponentTypes() {
        return componentTypes;
    }
 
    @Override   
    public String toString() {
        return getClass().getSimpleName() + "[" + requestId + ", " + Arrays.asList(componentTypes) + "]"; 
    }
}

