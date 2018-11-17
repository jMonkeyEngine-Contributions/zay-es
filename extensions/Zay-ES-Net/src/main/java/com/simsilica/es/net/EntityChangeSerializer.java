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

package com.simsilica.es.net;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.jme3.network.serializing.Serializer;

import com.simsilica.es.EntityChange;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityId;


/**
 *  Serializes EntityChange objects which don't have a no-arg
 *  constructor and so can't partake of the normal FieldSerializer
 *
 *  @author    Paul Speed
 */
public class EntityChangeSerializer extends Serializer {

    private Serializer idSerializer;
    private Serializer classSerializer;

    @Override
    public void initialize( Class type ) {
        idSerializer = Serializer.getSerializer(EntityId.class, false);
        classSerializer = Serializer.getSerializer(Class.class, true);
    }

    @SuppressWarnings("unchecked") 
    public <T> T readObject( ByteBuffer data, Class<T> c ) throws IOException {
        EntityId id = idSerializer.readObject(data, EntityId.class);
        Class type = classSerializer.readObject(data, Class.class);
        Object component = Serializer.readClassAndObject(data);
        
        return c.cast(new EntityChange(id, type, (EntityComponent)component));
    }
    
    public void writeObject( ByteBuffer buffer, Object object ) throws IOException {
        EntityChange change = (EntityChange)object;

        idSerializer.writeObject(buffer, change.getEntityId());
        classSerializer.writeObject(buffer, change.getComponentType());
        
        // Have to use dynamic lookup for the value
        Serializer.writeClassAndObject(buffer, TransientUtils.clean(change.getComponent()));
    }
}


