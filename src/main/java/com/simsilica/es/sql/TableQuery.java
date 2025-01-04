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

package com.simsilica.es.sql;

import java.sql.*;
import java.util.*;

import org.slf4j.*;

import com.google.common.base.MoreObjects;

import com.simsilica.es.ComponentFilter;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityId;
import com.simsilica.es.Query;

/**
 *  Queries a single table but can be joined to a JoinQuery.  Otherwise
 *  behaves similarly to DefaultQuery.
 *
 *  @author    Paul Speed
 */
public class TableQuery<T extends EntityComponent> implements Query {
    static Logger log = LoggerFactory.getLogger(TableQuery.class);

    private final SqlEntityData parent;
    private final Class<T> type;
    private final ComponentTable<T> table;
    private final ComponentFilter<T> filter;

    public TableQuery( SqlEntityData parent, Class<T> type, ComponentTable<T> table, ComponentFilter<T> filter ) {
        this.parent = parent;
        this.type = type;
        this.table = table;
        this.filter = filter;
    }

    protected SqlEntityData getParent() {
        return parent;
    }

    protected String getTableName() {
        return table.getTableName();
    }

    protected ComponentFilter<T> getFilter() {
        return filter;
    }

    protected ComponentTable<T> getTable() {
        return table;
    }

    public Set<EntityId> execute() {
        try {
            return table.getEntityIds(parent.getSession(), filter);
        } catch( SQLException e ) {
            throw new RuntimeException("Error retrieving component entities for type:" + type + ", filter:" + filter);
        }
    }

    public EntityId findFirst() {
        try {
            return table.getEntityId(parent.getSession(), filter);
        } catch( SQLException e ) {
            throw new RuntimeException("Error finding component entity for type:" + type + ", filter:" + filter);
        }
    }

    public Query join( Query other ) {
        if( other.getClass() == TableQuery.class ) {
            TableQuery q = (TableQuery)other;
            if( parent == q.parent ) {
                return new JoinQuery(parent, this, q);
            }
        }
        if( other.getClass() == JoinQuery.class ) {
            return other.join(this);
        }
        return null;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getSimpleName())
            .omitNullValues()
            .add("type", type)
            .add("filter", filter)
            .toString();
    }
}
