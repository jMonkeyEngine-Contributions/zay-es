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

import com.simsilica.es.EntityId;
import com.simsilica.es.Query;

/**
 *  Queries across multiple tables at the same time, linked by entity ID.
 *
 *  @author    Paul Speed
 */
public class JoinQuery implements Query {
    static Logger log = LoggerFactory.getLogger(JoinQuery.class);

    private final SqlEntityData parent;
    private final List<TableQuery> queries = new ArrayList<>();
    private String statement;
    private List<Object> parms;

    public JoinQuery( SqlEntityData parent, TableQuery... queries ) {
        this.parent = parent;
        this.queries.addAll(Arrays.asList(queries));
    }

    @SuppressWarnings("unchecked")
    protected void buildQuery() {
        if( statement != null ) {
            return;
        }

        this.parms = new ArrayList<>();
        StringBuilder tables = new StringBuilder();
        StringBuilder where = new StringBuilder();
        int index = 0;
        for( TableQuery query : queries ) {
            try {
                String name = "t" + index;
                if( tables.length() > 0 ) {
                    tables.append(", ");
                }
                tables.append(query.getTableName() + " " + name);

                if( index > 0 ) {
                    // Join this table with the previous one
                    if( where.length() > 0 ) {
                        where.append(" AND ");
                    }
                    where.append(name + ".entityId = t0.entityId");
                }

                if( query.getFilter() == null ) {
                    // Move along... nothing to add
                    continue;
                }

                StringBuilder sub = new StringBuilder();
                int added = query.getTable().appendFilter(name, query.getFilter(), sub, parms);
                if( added == 0 ) {
                    continue;
                }

                if( where.length() > 0 ) {
                    where.append(" AND ");
                }

                if( added > 1 ) {
                    where.append("(");
                    where.append(sub);
                    where.append(")");
                } else {
                    where.append(sub);
                }
            } finally {
                index++;
            }
        }

        StringBuilder sql = new StringBuilder("SELECT t0.entityId FROM ");
        sql.append(tables);
        if( where.length() > 0 ) {
            sql.append(" WHERE ");
            sql.append(where);
        }

        this.statement = sql.toString();

        log.info("Built SQL:" + statement + " parms:" + parms + " for:" + this);
    }

    protected PreparedStatement prepareStatement( SqlSession session ) throws SQLException {
        PreparedStatement st = session.prepareStatement(statement);
        int index = 1;
        for( Object o : parms ) {
            st.setObject(index++, o);
        }
        return st;
    }

    public Set<EntityId> execute() {
        buildQuery();

        try {
            SqlSession session = parent.getSession();
            PreparedStatement st = prepareStatement(session);

            Set<EntityId> results = new HashSet<EntityId>();
            ResultSet rs = st.executeQuery();
            try {
                while( rs.next() ) {
                    Long entityId = rs.getLong(1);
                    results.add(new EntityId(entityId));
                }
            } finally {
                rs.close();
            }

            return results;
        } catch( SQLException e ) {
            throw new RuntimeException("Error executing sql:" + statement, e);
        }
    }

    public EntityId findFirst() {
        buildQuery();

        try {
            SqlSession session = parent.getSession();
            PreparedStatement st = prepareStatement(session);
            ResultSet rs = st.executeQuery();
            try {
                while( rs.next() ) {
                    Long entityId = rs.getLong(1);
                    return new EntityId(entityId);
                }
            } finally {
                rs.close();
            }
        } catch( SQLException e ) {
            throw new RuntimeException("Error executing sql:" + statement, e);
        }

        return null;
    }

    public Query join( Query other ) {
        if( other.getClass() == TableQuery.class ) {
            TableQuery q = (TableQuery)other;
            if( q.getParent() == parent ) {
                // Just add it to our list
                queries.add(q);
                statement = null;
                return this;
            }
        }
        if( other.getClass() == JoinQuery.class ) {
            JoinQuery q = (JoinQuery)other;
            if( q.parent == parent ) {
                queries.addAll(q.queries);
                statement = null;
                return this;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + queries + "]";
    }
}
