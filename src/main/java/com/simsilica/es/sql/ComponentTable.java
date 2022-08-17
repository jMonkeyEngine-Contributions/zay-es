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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
    
    private boolean cached = true;
    private Class<T> type;
    private Constructor<T> ctor;
    private FieldType[] fields;
    private String tableName;
    private String[] dbFieldNames;
    
    private String insertSql;
    private String updateSql;

    protected ComponentTable( Constructor<T> ctor, Class<T> type, FieldType[] fields ) {
        this.type = type;
        this.ctor = ctor;
        this.fields = fields;
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
        List<FieldType> types = FieldTypes.getFieldTypes(type);
        FieldType[] array = new FieldType[types.size()];
        array = types.toArray(array);

        // Look up a no-arg constructor so that we can make sure it
        // is accessible similar to fields
        Constructor<T> ctor;
        try {
            ctor = type.getDeclaredConstructor();
            
            // Make sure it is accessible
            ctor.setAccessible(true);
        } catch( NoSuchMethodException e ) {
            throw new IllegalArgumentException("Type does not have a no-arg constructor:" + type, e);
        }
        
        ComponentTable<T> result = new ComponentTable<>(ctor, type, array);
        result.initialize(session);
        
        return result;
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
        sql.append(", entityId");
        sql.append(")");              
        sql.append(" VALUES ");       
        sql.append("(");
        for( int i = 0; i < dbFieldNames.length; i++ ) {
            sql.append((i > 0 ? ", " : "") + "?");
        }
        sql.append(", ?");
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
            checkStructure(defs, dbFields);
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
    }

    protected void checkStructure( Map<String,FieldType> defs, 
                                   Map<String,Integer> dbFields ) throws SQLException {
                                   
        log.info("Table fields:" + dbFields);
        log.info("Object fields:" + defs);

        Set<String> newFields = new HashSet<String>();
        Set<String> removedFields = new HashSet<String>();
        
        for( String s : dbFields.keySet() ) {
            if( !defs.containsKey(s) ) {
                removedFields.add(s);
            }
        }

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
            
        log.info("New fields:" + newFields);
        log.info("Removed fields:" + removedFields);            
 
        if( newFields.isEmpty() && removedFields.isEmpty() )
            return;
 
 /*       
        // Otherwise... let's see if we can alter the table to match
        for( String s : newFields )
            {
            FieldType ft = defs.get(s);
            
            StringBuilder sb = new StringBuilder( "ALTER TABLE" );
            sb.append( " " + tableName + "\n" );
            sb.append( " ADD COLUMN " + s + " " + ft.getDbType() );
            }
        
        This could work... the problem is in what to specify for
        the default value for new fields in an existing table.  It might be 
        solvable or it might be something we include some groovy
        scripts for upgrading versions or something. 
*/
        
        // See if it has the required fields
        if( !newFields.isEmpty() || !removedFields.isEmpty() ) {
            throw new RuntimeException("Schema mismatch, table fields:" + dbFields 
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
                              
        // Try to update the existing component first
        PreparedStatement st = session.prepareStatement(updateSql); 
        int index = 1;
        for( FieldType t : fields ) {
            index = t.store(component, st, index);
        }
         
        st.setObject(index++, entityId.getId());                
        int result = st.executeUpdate();         
        if( result > 0 ) {
            return;
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
                int index = 1;
                T target = ctor.newInstance(); 
                for( FieldType t : fields ) {
                    index = t.load(target, rs, index);
                }
                    
                return target;               
            }
            return null;
        } catch( InvocationTargetException | InstantiationException | IllegalAccessException e ) {
            throw new RuntimeException("Error in table mapping", e);
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
 
    protected int appendFilter( FieldFilter f, StringBuilder where, List<Object> parms ) {
    
        FieldType ft = getFieldType(f.getFieldName());
            
        if( where.length() > 0 ) {
            where.append(" AND ");
        }
    
        Object dbValue = ft.toDbValue(f.getValue());
        if( dbValue == null ) {
            where.append(f.getFieldName() + " IS NULL"); 
        } else {                       
            where.append(f.getFieldName() + " = ?");
            parms.add(dbValue);
        }
        return 1;           
    }
    
    protected int appendFilter( OrFilter f, StringBuilder where, List<Object> parms ) {
    
        if( where.length() > 0 )
            where.append(" AND ");
 
        int count = 0;
            
        StringBuilder sub = new StringBuilder();
        for( ComponentFilter op : f.getOperands() ) {
        
            if( count > 0 ) {
                where.append(" OR ");
            }
                
            int nested = appendFilter(op, sub, parms);
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

    protected int appendFilter( AndFilter f, StringBuilder where, List<Object> parms ) {
    
        if( where.length() > 0 ) {
            where.append(" AND ");
        }
 
        int count = 0;
            
        StringBuilder sub = new StringBuilder();
        for( ComponentFilter op : f.getOperands() ) {
        
            if( count > 0 ) {
                where.append( " AND " );
            }
                
            int nested = appendFilter(op, sub, parms);
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
    
    protected int appendFilter( ComponentFilter f, StringBuilder where, List<Object> parms ) {
        if( f instanceof FieldFilter ) {
            return appendFilter((FieldFilter)f, where, parms);
        } else if( f instanceof OrFilter ) {
            return appendFilter((OrFilter)f, where, parms);
        } else if( f instanceof AndFilter ) {
            return appendFilter((AndFilter)f, where, parms);
        } else {
            throw new IllegalArgumentException("Cannot handle filter:" + f);
        }
    }
 
    public Set<EntityId> getEntityIds( SqlSession session, 
                                       ComponentFilter filter ) throws SQLException {
                                       
        StringBuilder sql = new StringBuilder("SELECT ");       
        sql.append(" entityId");       
        sql.append(" FROM " + tableName);
        
        List<Object> parms = new ArrayList<Object>();
        
        StringBuilder where = new StringBuilder();
        appendFilter(filter, where, parms);
        
        if( where.length() > 0 ) {
            sql.append(" WHERE " + where);
        }
 
        try {
            PreparedStatement st = session.prepareStatement(sql.toString());
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
            throw new RuntimeException("Error executing sql:" + sql, e);
        }           
    }    

    /**
     *  Retrieves the first matching entity.
     */
    public EntityId getEntityId( SqlSession session, 
                                 ComponentFilter filter ) throws SQLException {
                                 
        StringBuilder sql = new StringBuilder("SELECT ");       
        sql.append(" entityId");       
        sql.append(" FROM " + tableName);
        
        List<Object> parms = new ArrayList<Object>();
        
        StringBuilder where = new StringBuilder();
        appendFilter(filter, where, parms);

        if( where.length() > 0 ) {
            sql.append(" WHERE " + where);
        }
 
        PreparedStatement st = session.prepareStatement(sql.toString());
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
                int index = 1;
                T target = ctor.newInstance();
                for( FieldType t : fields ) {
                    index = t.load(target, rs, index);
                }
                    
                Long entityId = rs.getLong(index);
                
                results.add(new ComponentReference<T>(new EntityId(entityId), target)); 
            }
        } catch( InvocationTargetException | InstantiationException | IllegalAccessException e ) {
            throw new RuntimeException("Error in table mapping", e);
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
