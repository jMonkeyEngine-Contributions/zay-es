/*
 * $Id$
 *
 * Copyright (c) 2011-2013 jMonkeyEngine
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

package com.simsilica.es.base;

import com.google.common.base.MoreObjects;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.ComponentFilter;
import java.util.*;
import java.util.concurrent.*;


/**
 *  Map-based component handler for in-memory components.
 *
 *  @author    Paul Speed
 */
public class MapComponentHandler<T extends EntityComponent>
                    implements ComponentHandler<T> {

    private final Class<? extends EntityComponent> type;
    private final Map<EntityId,T> components = new ConcurrentHashMap<EntityId,T>();

    /**
     *  Provided only for backwards compatibility with any custom subclasses.
     */
    @Deprecated
    public MapComponentHandler() {
        this(null);
    }

    public MapComponentHandler( Class<? extends EntityComponent> type ) {
        this.type = type;
    }

    @Override
    public void setComponent( EntityId entityId, T component ) {
        components.put(entityId, component);
    }

    @Override
    public boolean removeComponent( EntityId entityId ) {
        return components.remove(entityId) != null;
    }

    @Override
    public T getComponent( EntityId entityId ) {
        return components.get(entityId);
    }

    @Override
    public Set<EntityId> getEntities() {
        return components.keySet();
    }

    @Override
    public Set<EntityId> getEntities( ComponentFilter filter ) {

        if( filter == null ) {
            return components.keySet();
        }

        Set<EntityId> results = new HashSet<EntityId>();
        for( Map.Entry<EntityId,T> e : components.entrySet() ) {
            if( filter.evaluate((EntityComponent)e.getValue()) ) {
                results.add(e.getKey());
            }
        }
        return results;
    }

    @Override
    public EntityId findEntity( ComponentFilter filter ) {
        for( Map.Entry<EntityId,T> e : components.entrySet() ) {
            if( filter == null || filter.evaluate((EntityComponent)e.getValue()) ) {
                return e.getKey();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getSimpleName())
            .add("type", type)
            .add("size", components.size())
            .toString();
    }
}
