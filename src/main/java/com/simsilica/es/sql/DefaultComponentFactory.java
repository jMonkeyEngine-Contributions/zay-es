/*
 * $Id$
 *
 * Copyright (c) 2011-2023 jMonkeyEngine
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

package com.simsilica.es.sql;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * @author Ali-RS
 */
public class DefaultComponentFactory<T> implements SqlComponentFactory<T> {

    private final FieldType[] fields;
    private final Constructor<T> ctor;

    public DefaultComponentFactory(Class<T> type) {
        List<FieldType> types = FieldTypes.getFieldTypes(type);
        this.fields = types.toArray(new FieldType[types.size()]);

        // Look up a no-arg constructor so that we can make sure it
        // is accessible similar to fields
        try {
            ctor = type.getDeclaredConstructor();

            // Make sure it is accessible
            ctor.setAccessible(true);
        } catch( NoSuchMethodException e ) {
            throw new IllegalArgumentException("Type does not have a no-arg constructor:" + type, e);
        }
    }

    @Override
    public FieldType[] getFieldTypes() {
        return fields;
    }

    @Override
    public T createComponent(ResultSet rs) throws SQLException {
        try {
            int index = 1;
            T target = ctor.newInstance();
            for (FieldType t : fields) {
                index = t.load(target, rs, index);
            }

            return target;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Error in table mapping", e);
        }
    }
}
