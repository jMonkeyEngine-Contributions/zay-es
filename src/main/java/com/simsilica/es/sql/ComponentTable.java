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

package com.simsilica.es.sql;

import java.sql.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

import com.simsilica.es.ComponentFilter;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.filter.OrFilter;
import com.simsilica.es.filter.AndFilter;
import com.simsilica.es.filter.FieldFilter;


/**
 *
 *  @author    Paul Speed
 */
public class ComponentTable<T extends EntityComponent> {

    static Logger log = LoggerFactory.getLogger(ComponentTable.class);

    private final boolean cached = true;
    private final SqlComponentFactory<T>  componentFactory;
    private final FieldType[] fields;
    private final String tableName;
    private String[] dbFieldNames;

    private final String insertSql;
    private final String updateSql;

    protected ComponentTable( Class<T> type, SqlComponentFactory<T> factory ) {
        this.componentFactory = factory;
        this.fields = factory.getFieldTypes();
        this.tableName = type.getSimpleName().toUpperCase();

        List<String> names = new ArrayList<String>();
        for( FieldType t : fields ) {
            t.addFields( "", names );
        }
        dbFieldNames = new String[names.size()];
        dbFieldNames = names.toArray(dbFieldNames);

        insertSql = createInsertSql();
        updateSql = createUpdateSql();
    }

    public static <T extends EntityComponent> ComponentTable<T> create( SqlSession session,
                                                                        Class<T> type ) throws SQLException {
        ComponentTable<T> result = new ComponentTable<>(type, new DefaultComponentFactory<>(type));
        result.initialize(session);

        return result;
    }

    protected String getTableName() {
        return tableName;
    }

    protected String createUpdateSql() {
        StringBuilder sql = new StringBuilder("UPDATE " + tableName);
        sql.append(" SET (");

        Joiner.on(", ").appendTo(sql, dbFieldNames);
        sql.append(")");

        sql.append(" = ");

        sql.append("(");
        for( int i = 0; i < dbFieldNames.length; i++ ) {
            sql.append((i > 0 ? ", " : "") + "?");
        }
        sql.append(")");

        sql.append(" WHERE entityId = ?");
        return sql.toString();
    }

    protected String createInsertSql() {

        StringBuilder sql = new StringBuilder("INSERT INTO " + tableName);
        sql.append(" (");

        Joiner.on(", ").appendTo(sql, dbFieldNames);
        if( dbFieldNames.length > 0 ) {
            // It's possible for a component to have no fields in which case
            // we don't want the comma.
            sql.append(", ");
        }
        sql.append("entityId");
        sql.append(")");
        sql.append(" VALUES ");
        sql.append("(");
        for( int i = 0; i < dbFieldNames.length; i++ ) {
            sql.append((i > 0 ? ", " : "") + "?");
        }
        if( dbFieldNames.length > 0 ) {
            sql.append(", ");
        }
        sql.append("?");
        sql.append(")");

        return sql.toString();
    }

    protected void initialize( SqlSession session ) throws SQLException {

        // See if the table exists
        DatabaseMetaData md = session.getConnection().getMetaData();

        log.info("Checking for table:" + tableName);
        ResultSet rs = md.getColumns(null, "PUBLIC", tableName, null);
        Map<String,Integer> dbFields = new HashMap<String,Integer>();
        try {
            while( rs.next() ) {
                if( log.isTraceEnabled() ) {
                    log.trace(rs.getString("TABLE_NAME") + " :" + rs.getString("COLUMN_NAME"));
                }
                dbFields.put(rs.getString("COLUMN_NAME"), rs.getInt("DATA_TYPE"));
            }

            // 2022-08-16 Leave this in so we know we found a table at all
            //dbFields.remove("ENTITYID");
        } finally {
            rs.close();
        }

        if( log.isTraceEnabled() ) {
            log.trace("TABLE:" + tableName);
            for( Map.Entry<String, Integer> e : dbFields.entrySet() ) {
                log.trace("    " + e);
            }
        }

        // Check for index existence... we don't have different index types
        // yet so this is sufficient.
        rs = md.getIndexInfo(null, "PUBLIC", tableName, false, false);
        Set<String> indexedFields = new HashSet<>();
        try {
            //ResultSetMetaData rsmd = rs.getMetaData();
            //int numberOfColumns = rsmd.getColumnCount();
            while( rs.next() ) {
                //for( int i = 1; i <= numberOfColumns; i++ ) {
                //    log.info("    " + rsmd.getColumnName(i) + "=" + rs.getObject(i));
                //}
                indexedFields.add(rs.getString("COLUMN_NAME"));
            }
        } finally {
            rs.close();
        }

        if( log.isTraceEnabled() ) {
            log.trace("INDEXED FIELDS:");
            for( String s : indexedFields ) {
                log.trace("    " + s);
            }
        }

        Map<String,FieldType> defs = new LinkedHashMap<String,FieldType>();
        for( FieldType t : fields ) {
            t.addFieldDefinitions("", defs);
        }

        if( !dbFields.isEmpty() ) {
            // 2022-08-16 The object fields won't have an entityID so we
            // remove it before the structure check.  In today's light, if we wanted
            // to be 100% correct then we would still make sure we had it but this
            // is fine.
            dbFields.remove("ENTITYID");
            checkStructure(session, tableName, defs, dbFields, indexedFields);
            return;
        }

        // Really should have a separate class for this
        StringBuilder sb = new StringBuilder("CREATE");
        if( cached ) {
            sb.append(" CACHED");
        }
        sb.append(" TABLE");

        sb.append(" " + tableName + "\n");
        sb.append("(\n");
        sb.append("  entityId BIGINT PRIMARY KEY");
        for( Map.Entry<String,FieldType> e : defs.entrySet() ) {
            sb.append(",\n  " + e.getKey() + " " + e.getValue().getDbType());
        }
        sb.append("\n)");

        log.info("Create statement:\n" + sb);

        Statement st = session.getConnection().createStatement();
        int i = st.executeUpdate(sb.toString());
        st.close();

        log.info("Result:" + i);

        for( Map.Entry<String,FieldType> entry : defs.entrySet() ) {
            if( entry.getValue().isIndexed() ) {
                createIndex(session, tableName, entry.getKey());
            }
        }
    }

    protected void createIndex( SqlSession session, String tableName, String column ) throws SQLException {
        log.info("creating index for:" + tableName + " column:" + column);
        // CREATE INDEX spawn_pos_bin_id_index ON "PUBLIC"."SPAWNPOSITION" (BINID);
        StringBuilder sb = new StringBuilder("CREATE");
        sb.append(" INDEX");
        sb.append(" " + tableName + "_" + column + "_IDX");
        sb.append(" ON " + tableName);
        sb.append(" (" + column + ")");
        log.info("Create index statement:" + sb);

        Statement st = session.getConnection().createStatement();
        int i = st.executeUpdate(sb.toString());
        st.close();

        log.info("Result:" + i);
    }

    protected boolean addField( SqlSession session, String tableName, FieldType ft ) throws SQLException {
        log.info("add field for:" + tableName + " field:" + ft);
        StringBuilder sb = new StringBuilder( "ALTER TABLE" );
        sb.append( " " + tableName + "\n" );
        sb.append( " ADD COLUMN " + ft.getFieldName() + " " + ft.getDbType() );

        log.info("Add field statement:" + sb);

        Statement st = session.getConnection().createStatement();
        int i = st.executeUpdate(sb.toString());
        st.close();

        log.info("Result:" + i);

        // The poblem left is how to provide values for the new fields other than
        // defaults or null.

        return true;
    }

    protected void checkStructure( SqlSession session,
                                   String tableName,
                                   Map<String,FieldType> defs,
                                   Map<String,Integer> dbFields,
                                   Set<String> indexedFields ) throws SQLException {

        log.info("Table fields:" + dbFields);
        log.info("Object fields:" + defs);

        Set<String> newFields = new HashSet<String>();
        Set<String> removedFields = new HashSet<String>();
        Set<String> newIndex = new HashSet<>();

        // If the DB has a field but the type def doesn't then it's a 'removed field'
        for( String s : dbFields.keySet() ) {
            if( !defs.containsKey(s) ) {
                removedFields.add(s);
            }
        }

        // If the type def has a field but the table doesn't then it's an 'added field'
        for( String s : defs.keySet() ) {
            if( !dbFields.containsKey(s) ) {
                newFields.add(s);
            }
        }

        // Could also check for altered fields
        for( Map.Entry<String,Integer> e : dbFields.entrySet() ) {
            FieldType ft = defs.get(e.getKey());
            if( ft == null ) {
                continue;
            }

            // Compare the DB's type to the FieldType's type
        }

        // If the field type def says the field should be indexed but it's
        // not... then it's a 'new index'.
        // If a particular database has an index then we won't remove it.  It's
        // possible that some DB admin added it on purpose.
        for( Map.Entry<String, FieldType> entry : defs.entrySet() ) {
            if( !entry.getValue().isIndexed() ) {
                continue;
            }
            if( indexedFields.contains(entry.getKey()) ) {
                continue;
            }
            newIndex.add(entry.getKey());
        }

        log.info("New fields:" + newFields);
        log.info("Removed fields:" + removedFields);
        log.info("New index fields:" + newIndex);

        if( !newFields.isEmpty() ) {
            // See if we can add any of the new fields... even if we can't
            // fill in the new values for them.
            for( String s : new ArrayList<>(newFields) ) {
                FieldType ft = defs.get(s);
                if( addField(session, tableName, ft) ) {
                    newFields.remove(s);
                }
            }
        }

        if( !newIndex.isEmpty() ) {
            // This is something we can fix
            for( String index : newIndex ) {
                createIndex(session, tableName, index);
            }
        }

        // See if it has the required fields
        if( !newFields.isEmpty() ) {
            throw new RuntimeException("Added fields.  Schema mismatch, table fields:" + dbFields
                                        + " object fields:" + defs.keySet());
        }
        if( !removedFields.isEmpty() ) {
            // In theory, this should be ok but we'll issue a warning.  Extra
            // fields just take up extra space.
            log.warn("Removed fields.  Schema mismatch, table fields:" + dbFields
                                        + " object fields:" + defs.keySet());
        }
    }

    protected FieldType getFieldType( String field ) {
        for( FieldType t : fields ) {
            if( t.getFieldName().equals(field) ) {
                return t;
            }
        }
        return null;
    }

    public void setComponent( SqlSession session, EntityId entityId,
                              T component ) throws SQLException {

        int index;
        int result;
        PreparedStatement st;

        // If we don't have any fields then there would never be anything to update
        // and the update syntax is bad.  So we'll only try to update if the
        // object has actual fields (ie: not a marker component)
        if( fields.length > 0 ) {
            // Try to update the existing component first
            st = session.prepareStatement(updateSql);
            index = 1;
            for( FieldType t : fields ) {
                index = t.store(component, st, index);
            }

            st.setObject(index++, entityId.getId());
            result = st.executeUpdate();
            if( result > 0 ) {
                return;
            }
        }

        // If that didn't succeed then insert
        st = session.prepareStatement(insertSql);

        index = 1;
        for( FieldType t : fields ) {
            index = t.store(component, st, index);
        }
        st.setObject(index++, entityId.getId());

        result = st.executeUpdate();
    }

    public boolean removeComponent( SqlSession session, EntityId entityId ) throws SQLException {

        String sql = "DELETE FROM " + tableName + " WHERE entityId=" + entityId.getId();
        PreparedStatement st = session.prepareStatement(sql.toString());
        int result = st.executeUpdate();
        /// a result of 1 means it worked but we can silently fail if it
        // didn't exist.
        return result > 0;
    }

    public T getComponent( SqlSession session, EntityId entityId ) throws SQLException {

        StringBuilder sql = new StringBuilder("SELECT ");
        if( dbFieldNames.length > 0 ) {
            Joiner.on(", ").appendTo(sql, dbFieldNames);
        } else {
            // 2022-08-16 We need 'some' field just to make the SQL work in
            // the case of no other fields.  Since we will ignore it anyway
            // then it doesn't really matter but this seems better than '*'
            sql.append("entityId");
        }
        sql.append(" FROM " + tableName);
        sql.append(" WHERE entityId=?");

        PreparedStatement st = session.prepareStatement(sql.toString());
        st.setObject(1, entityId.getId());
        ResultSet rs = st.executeQuery();
        try {
            if( rs.next() ) {
                return componentFactory.createComponent(rs);
            }
            return null;
        } finally {
            rs.close();
        }
    }

    public Set<EntityId> getEntityIds( SqlSession session ) throws SQLException {

        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(" entityId");
        sql.append(" FROM " + tableName);

        Set<EntityId> results = new HashSet<EntityId>();

        PreparedStatement st = session.prepareStatement(sql.toString());
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
    }

    protected int appendFilter( String prefix, FieldFilter f, StringBuilder where, List<Object> parms ) {

        FieldType ft = getFieldType(f.getFieldName());

        if( where.length() > 0 ) {
            where.append(" AND ");
        }

        Object dbValue = ft.toDbValue(f.getValue());
        if( dbValue == null ) {
            where.append(prefix + "." + f.getFieldName() + " IS NULL");
        } else {
            where.append(prefix + "." + f.getFieldName() + " = ?");
            parms.add(dbValue);
        }
        return 1;
    }

    protected int appendFilter( String prefix, OrFilter f, StringBuilder where, List<Object> parms ) {

        if( where.length() > 0 )
            where.append(" AND ");

        int count = 0;

        StringBuilder sub = new StringBuilder();
        for( ComponentFilter op : f.getOperands() ) {

            if( count > 0 ) {
                where.append(" OR ");
            }

            int nested = appendFilter(prefix, op, sub, parms);
            if( nested > 1 ) {
                where.append("(" + sub + ")");
            } else {
                where.append(sub);
            }

            sub.setLength(0);
            count += nested;
        }
        return count;
    }

    protected int appendFilter( String prefix, AndFilter f, StringBuilder where, List<Object> parms ) {

        if( where.length() > 0 ) {
            where.append(" AND ");
        }

        int count = 0;

        StringBuilder sub = new StringBuilder();
        for( ComponentFilter op : f.getOperands() ) {

            if( count > 0 ) {
                where.append( " AND " );
            }

            int nested = appendFilter(prefix, op, sub, parms);
            if( nested > 1 ) {
                where.append("(" + sub + ")");
            } else {
                where.append(sub);
            }

            sub.setLength(0);
            count += nested;
        }
        return count;
    }

    protected int appendFilter( String prefix, ComponentFilter f, StringBuilder where, List<Object> parms ) {
        if( f instanceof FieldFilter ) {
            return appendFilter(prefix, (FieldFilter)f, where, parms);
        } else if( f instanceof OrFilter ) {
            return appendFilter(prefix, (OrFilter)f, where, parms);
        } else if( f instanceof AndFilter ) {
            return appendFilter(prefix, (AndFilter)f, where, parms);
        } else {
            throw new IllegalArgumentException("Cannot handle filter:" + f);
        }
    }

    protected String buildStatement( ComponentFilter filter, List<Object> parms ) {
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(" t1.entityId");
        sql.append(" FROM " + tableName + " t1");

        StringBuilder where = new StringBuilder();
        if( filter != null ) {
            appendFilter("t1", filter, where, parms);
        }

        if( where.length() > 0 ) {
            sql.append(" WHERE " + where);
        }

        return sql.toString();
    }

    public Set<EntityId> getEntityIds( SqlSession session,
                                       ComponentFilter filter ) throws SQLException {

        List<Object> parms = new ArrayList<>();
        String statement = buildStatement(filter, parms);

        try {
            PreparedStatement st = session.prepareStatement(statement);
            int index = 1;
            for( Object o : parms ) {
                st.setObject(index++, o);
            }

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

    /**
     *  Retrieves the first matching entity.
     */
    public EntityId getEntityId( SqlSession session,
                                 ComponentFilter filter ) throws SQLException {

        List<Object> parms = new ArrayList<>();
        String statement = buildStatement(filter, parms);

        PreparedStatement st = session.prepareStatement(statement);
        int index = 1;
        for( Object o : parms ) {
            st.setObject(index++, o);
        }

        ResultSet rs = st.executeQuery();
        try {
            while( rs.next() ) {
                Long entityId = rs.getLong(1);
                return new EntityId(entityId);
            }
        } finally {
            rs.close();
        }

        return null;
    }

    public Iterator<Map.Entry<EntityId,T>> components( SqlSession session ) throws SQLException {

        // Just grab them all for now
        List<Map.Entry<EntityId,T>> results = new ArrayList<Map.Entry<EntityId,T>>();

        StringBuilder sql = new StringBuilder("SELECT ");
        if( dbFieldNames.length > 0 ) {
            Joiner.on(", ").appendTo(sql, dbFieldNames);
            sql.append(", entityId");
        } else {
            // 2022-08-16 For components with no fields, just the entityID is all we need
            sql.append("entityId");
        }
        sql.append(" FROM " + tableName);

        PreparedStatement st = session.prepareStatement(sql.toString());
        ResultSet rs = st.executeQuery();
        try {
            while( rs.next() ) {
                T target = componentFactory.createComponent(rs);

                Long entityId = rs.getLong("entityId");

                results.add(new ComponentReference<T>(new EntityId(entityId), target));
            }
        } finally {
            rs.close();
        }

        return results.iterator();
    }

    private class ComponentReference<T> implements Map.Entry<EntityId,T> {

        private EntityId entityId;
        private T component;

        public ComponentReference( EntityId entityId, T component ) {
            this.entityId = entityId;
            this.component = component;
        }

        @Override
        public EntityId getKey() {
            return entityId;
        }

        @Override
        public T getValue() {
            return component;
        }

        @Override
        public T setValue( T value ) {
            throw new UnsupportedOperationException("Cannot set the component on a reference.");
        }
    }
}
