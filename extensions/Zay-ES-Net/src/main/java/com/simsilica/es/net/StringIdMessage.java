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


/**
 *  Used for requesting StringIndex lookups or responding
 *  to them.  Whatever is filled out in this message is used
 *  to lookup the other value and reply.  We can get away with
 *  this because string ID lookups only go client-&gt;server.
 *
 *  @author    Paul Speed
 */
@Serializable
public class StringIdMessage extends AbstractMessage {

    private int requestId;

    // Have to make 'id' an Object field because making 
    // it an Integer field confuses the Serializer and it
    // won't send nulls... but NPE instead.
    private Object id; 
    private String value;
    
    public StringIdMessage() {
    }
    
    public StringIdMessage( int requestId, int id ) {
        this.requestId = requestId;
        this.id = id;
    }
    
    public StringIdMessage( int requestId, String value ) {
        this.requestId = requestId;
        this.value = value;
    }

    public int getRequestId() {
        return requestId;
    }
    
    public String getString() {
        return value;
    }
    
    public Integer getId() {
        return (Integer)id;
    }
 
    @Override   
    public String toString() {
        return "StringIdMessage[id=" + id + ", string=" + value + "]";
    }
}
