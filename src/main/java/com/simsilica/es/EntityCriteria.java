/*
 * $Id$
 *
 * Copyright (c) 2024, Simsilica, LLC
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

package com.simsilica.es;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.*;


/**
 *  A composite query criteria objects that describes a type of
 *  entity including components it must have and any optional filters
 *  associated with those components.
 *
 *  @author    Paul Speed
 */
public class EntityCriteria implements Cloneable {
    static Logger log = LoggerFactory.getLogger(EntityCriteria.class);

    private Map<Class<? extends EntityComponent>, ComponentFilter> criteria = new LinkedHashMap<>();

    public EntityCriteria() {
    }

    @Override
    public EntityCriteria clone() {
        try {
            return (EntityCriteria)super.clone();
        } catch( CloneNotSupportedException e ) {
            throw new RuntimeException("Failed to clone", e);
        }
    }

    /**
     *  Compatibility method for building a criteria from the old-style filter + types.
     *  This will replace any existing criteria configured in this instance.
     */
    @SuppressWarnings("unchecked")
    public EntityCriteria set( ComponentFilter filter, Class... types ) {
        criteria.clear();
        for( Class t : types ) {
            if( filter != null && filter.getComponentType() == t ) {
                criteria.put(t, filter);
            } else {
                criteria.put(t, null);
            }
        }
        return this;
    }

    public EntityCriteria set( Class<? extends EntityComponent>[] types, ComponentFilter[] filters ) {
        criteria.clear();
        for( int i = 0; i < types.length; i++ ) {
            Class<? extends EntityComponent> type = types[i];
            ComponentFilter filter = filters[i];
            if( filter != null && filter.getComponentType() != type ) {
                throw new IllegalArgumentException("Type:" + type + " does not match filter:" + filter + " at index:" + i);
            }
            criteria.put(type, filter);
        }
        return this;
    }

    public EntityCriteria add( ComponentFilter... filters ) {
        for( ComponentFilter f : filters ) {
            add(f.getComponentType(), f);
        }
        return this;
    }

    @SafeVarargs
    public final EntityCriteria add( Class<? extends EntityComponent>... types ) {
        for( Class type : types ) {
            add(type, null);
        }
        return this;
    }

    public <T extends EntityComponent> EntityCriteria setFilter( Class<T> type, ComponentFilter<T> filter ) {
        criteria.put(type, filter);
        return this;
    }

    public void clearFilters() {
        for( Map.Entry<Class<? extends EntityComponent>, ComponentFilter> e : criteria.entrySet() ) {
            e.setValue(null);
        }
    }

    public Collection<ComponentFilter> getFilters() {
        return Collections.unmodifiableCollection(criteria.values());
    }

    @SuppressWarnings("unchecked")
    protected EntityCriteria add( Class type, ComponentFilter filter ) {
        if( criteria.containsKey(type) ) {
            throw new IllegalArgumentException("Type already defined:" + type);
        }
        criteria.put(type, filter);
        return this;
    }

    /**
     *  Returns a frozen array of the types in this criteria in the same order
     *  that the frozen filters array will be returned. 1:1 mapping.
     */
    @SuppressWarnings("unchecked")
    public Class<? extends EntityComponent>[] toTypeArray() {
        return criteria.keySet().toArray(new Class[0]);
    }

    /**
     *  Returns a frozen array of the filters in this criteria in the same
     *  order that the frozen types are returned.  1:1 mapping.  If a particular
     *  type does not have a filter then the corresponding entry is null in this array.
     */
    public ComponentFilter[] toFilterArray() {
        return criteria.values().toArray(new ComponentFilter[0]);
    }

    /**
     *  Returns true if the specific component matches the criteria.
     */
    public boolean isMatchingComponent( EntityComponent c ) {
        if( !hasType(c.getClass()) ) {
            return false;
        }
        ComponentFilter filter = criteria.get(c.getClass());
        return filter == null || filter.evaluate(c);
    }

    /**
     *  Returns true if this criteria handles the specified type.
     */
    public final boolean hasType( Class<? extends EntityComponent> type ) {
        return criteria.containsKey(type);
    }

    @Override
    public int hashCode() {
        return criteria.hashCode();
    }

    @Override
    public boolean equals( Object o ) {
        if( o == this ) {
            return true;
        }
        if( o == null || o.getClass() != getClass() ) {
            return false;
        }
        EntityCriteria other = (EntityCriteria)o;
        return other.criteria.equals(criteria);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + criteria;
    }
}

