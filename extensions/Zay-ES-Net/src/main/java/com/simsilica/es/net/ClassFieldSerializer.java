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

import com.jme3.network.serializing.Serializer;
import com.jme3.network.serializing.SerializerRegistration;
import com.jme3.network.serializing.serializers.StringSerializer;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;


/**
 *  Serializes java.lang.reflect.Field objects. 
 *
 *  Needs to be moved to SpiderMonkey.
 *
 *  @author    Paul Speed
 */
public class ClassFieldSerializer extends Serializer {

    private StringSerializer delegate = new StringSerializer();

    public Field readObject( ByteBuffer data, Class c ) throws IOException {
        // Use serializer's support for class reading/writing
        SerializerRegistration reg = readClass(data);
        if( reg == null || reg.getType() == Void.class ) { 
            return null;
        }
 
        Class type = reg.getType();
        String name = delegate.readObject( data, String.class );
        
        try {
            Field result = type.getDeclaredField(name);
            return result;
        } catch( NoSuchFieldException e ) {
            throw new IOException( "Error resolving field:" + name + " on:" + type, e );
        }
    }
    
    public void writeObject( ByteBuffer buffer, Object object ) throws IOException {
        if( object == null ) {            
            buffer.putShort((short)-1);
            return;
        }
        Field field = (Field)object;
                    
        writeClass( buffer, field.getDeclaringClass() );
        delegate.writeObject( buffer, field.getName() );
    }
}


