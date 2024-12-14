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
import com.jme3.network.serializing.serializers.FieldSerializer;
import com.simsilica.es.CreatedBy;
import com.simsilica.es.EntityChange;
import com.simsilica.es.EntityCriteria;
import com.simsilica.es.EntityId;
import com.simsilica.es.Name;
import com.simsilica.es.filter.AndFilter;
import com.simsilica.es.filter.FieldFilter;
import com.simsilica.es.filter.OrFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 *  @author    Paul Speed
 */
public class EntitySerializers {

    static Logger log = LoggerFactory.getLogger(EntitySerializers.class);

    private static final Class[] classes = {
        ComponentChangeMessage.class,
        EntityDataMessage.class,
        EntityDataMessage.ComponentData.class,
        EntityIdsMessage.class,
        EntitySetErrorMessage.class,
        FindEntitiesMessage.class,
        FindEntityMessage.class,
        GetComponentsMessage.class,
        GetEntitySetMessage.class,
        ReleaseEntitySetMessage.class,
        ReleaseWatchedEntityMessage.class,
        ResetEntitySetFilterMessage.class,
        ResultComponentsMessage.class,
        StringIdMessage.class,
        WatchEntityMessage.class
    };

    private static final Class[] forced = {
        // Some standard Zay-ES classes
        EntityId.class,
        CreatedBy.class,
        Name.class,
        FieldFilter.class,
        OrFilter.class,
        AndFilter.class,
        EntityCriteria.class
    };

    public static void initialize() {
        Serializer.registerClass( Class.class, new ClassSerializer() );
        Serializer.registerClass( java.lang.reflect.Field.class, new ClassFieldSerializer() );

        Serializer.registerClasses(classes);

        // Register some classes manually since Spider Monkey currently
        // requires them all to have @Serializable but we already know
        // which serializer we want to use.  Eventually I will fix SM
        // but for now I'll do this here.
        // This keeps Zay-ES proper from requiring the jme3-networking
        // dependency.
        Serializer fieldSerializer = new FieldSerializer();
        boolean error = false;
        for( Class c : forced ) {
            try {
                Serializer.registerClass(c, fieldSerializer);
            } catch( Exception e ) {
                log.error("Error registering class:" + c, e);
                error = true;
            }
        }
        if( error ) {
            throw new RuntimeException("Some classes failed to register");
        }

        // Another standard one for Zay-ES that requires a custom
        // serializer
        Serializer.registerClass(EntityChange.class, new EntityChangeSerializer());
    }
}


