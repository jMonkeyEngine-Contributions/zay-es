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

import com.simsilica.es.StringType;
import com.simsilica.es.EntityId;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;


/**
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class FieldTypes
{
    private static final Map<String,String> dbTypes = new HashMap<String,String>();
    static
    {
        dbTypes.put( "int", "INTEGER" );
        dbTypes.put( "long", "BIGINT" );
        dbTypes.put( "short", "SMALLINT" );
        dbTypes.put( "byte", "TINYINT" );
        dbTypes.put( "float", "FLOAT" ); // though these are actually the same size in hsql
        dbTypes.put( "double", "DOUBLE" );
    }

    public static List<FieldType> getFieldTypes( Class type )
    {
        return getFieldTypes( null, type );
    }        
        
    protected static List<FieldType> getFieldTypes( String prefix, Class type )
    {
        List<FieldType> results = new ArrayList<FieldType>();
        Field[] fields = type.getDeclaredFields();
 
        for( Field f : fields )
            {
            // No static fields
            if( Modifier.isStatic( f.getModifiers() ) )
                continue;
            
            // No transient fields
            if( Modifier.isTransient( f.getModifiers() ) )
                continue;
 
            
            // Make sure we can access its value even when the field
            // is private.    
            f.setAccessible(true);
            
            Class ft = f.getType();            
            if( ft.isPrimitive() )
                {
                results.add( new PrimitiveField(prefix, f) );
                continue;
                } 
 
            if( EntityId.class.isAssignableFrom(ft) )
                {
                results.add( new EntityIdField(prefix, f) );
                continue;
                }
 
            if( String.class.equals(ft) )
                {
                results.add( new StringField(prefix, f) );
                continue;
                }
 
            if( Enum.class.isAssignableFrom(ft) )
                {
                // This is not as straight forward to handle as we'd like.
                // If we use strings then we have to calculate some max size
                // and if we use ordinals then even reordering the enum will
                // screw up the database mapping.                
                throw new UnsupportedOperationException( "Enum types are not supported." );
                }
            
            results.add( new ObjectField( prefix, f ) );            
            }
         
        return results;
    }

    protected static class EntityIdField implements FieldType
    {
        private String name;
        private String dbFieldName;
        private Field field;
        
        public EntityIdField( Field field )
        {
            this( null, field );
        }
        
        public EntityIdField( String prefix, Field field )
        {
            this.field = field;
            this.name = field.getName();
            if( prefix == null )
                dbFieldName = name;
            else
                dbFieldName = prefix + name;
        }
        
        @Override
        public String getFieldName()
        {
            return name;
        }
 
        @Override
        public Class getType()
        {
            return field.getType();
        }
        
        @Override
        public String getDbType()
        {
            //String s = field.getType().getSimpleName();
            String result = dbTypes.get("long");
            return result; 
        }
        
        @Override
        public void addFieldDefinitions( String prefix, Map<String,FieldType> defs )
        {
            defs.put( prefix + dbFieldName.toUpperCase(), this );
        }
        
        @Override
        public void addFields( String prefix, List<String> fields )
        {
            fields.add( prefix + dbFieldName );
        }
    
        @Override
        public Object toDbValue( Object o )
        {
            if( o == null )
                return null;
            return ((EntityId)o).getId();
        }
    
        @Override
        public int store( Object object, PreparedStatement ps, int index ) throws SQLException
        {
            try
                {
                EntityId entityId = (EntityId)field.get(object);
                if( entityId != null )
                    ps.setObject( index++, entityId.getId() );
                else 
                    ps.setObject( index++, null );
                return index;
                }
            catch( IllegalAccessException e )
                {
                throw new RuntimeException( "Error in field mapping", e );
                }
        }
 
        @Override
        public int load( Object target, ResultSet rs, int index ) throws SQLException
        {
            try
                {
                Number value = (Number)rs.getObject(index++);
                
                if( value != null )
                    field.set( target, new EntityId(value.longValue()) );
                else
                    field.set( target, null );
                return index;
                }
            catch( IllegalAccessException e )
                {
                throw new RuntimeException( "Error in field mapping", e );
                }
        }
        
        @Override
        public String toString()
        {
            if( dbFieldName != name )
                return name + "/" + dbFieldName + ":" + getType();
            return getFieldName() + ":" + getType();
        }
    }

    protected static class StringField implements FieldType
    {
        private String name;
        private String dbFieldName;
        private Field field;
        private int maxLength;
        
        public StringField( String prefix, Field field )
        {
            this.field = field;
            this.name = field.getName();
            if( prefix == null )
                dbFieldName = name;
            else
                dbFieldName = prefix + name;
                
            // See if there is an annotation that denotes size
            StringType meta = field.getAnnotation( StringType.class );
            if( meta != null )
                maxLength = meta.maxLength();
            else
                maxLength = 512;
        }
        
        @Override
        public String getFieldName()
        {
            return name;
        }
 
        @Override
        public Class getType()
        {
            return field.getType();
        }
        
        @Override
        public String getDbType()
        {
            return "VARCHAR(" + maxLength + ")"; 
        }
        
        @Override
        public void addFieldDefinitions( String prefix, Map<String,FieldType> defs )
        {
            defs.put( prefix + dbFieldName.toUpperCase(), this );
        }
        
        @Override
        public void addFields( String prefix, List<String> fields )
        {
            fields.add( prefix + dbFieldName );
        }
    
        @Override
        public Object toDbValue( Object o )
        {
            return o;
        }
    
        @Override
        public int store( Object object, PreparedStatement ps, int index ) throws SQLException
        {
            try
                {
                ps.setObject( index++, field.get(object) );
                return index;
                }
            catch( IllegalAccessException e )
                {
                throw new RuntimeException( "Error in field mapping", e );
                }
        }
 
        @Override
        public int load( Object target, ResultSet rs, int index ) throws SQLException
        {
            try
                {
                field.set( target, rs.getObject(index++) );
                return index;
                }
            catch( IllegalAccessException e )
                {
                throw new RuntimeException( "Error in field mapping", e );
                }
        }
        
        @Override
        public String toString()
        {
            if( dbFieldName != name )
                return name + "/" + dbFieldName + ":" + getType();
            return getFieldName() + ":" + getType();
        }
    }
    
    protected static class ObjectField implements FieldType
    {
        private String name;
        private Field field;
        private FieldType[] fields;
        
        public ObjectField( String prefix, Field field )
        {
            this.field = field;
            this.name = field.getName(); 
            List<FieldType> list = getFieldTypes( prefix, field.getType() );
            fields = new FieldType[list.size()];
            fields = list.toArray(fields); 
        }
        
        @Override
        public String getFieldName()
        {
            return name;
        }
 
        @Override
        public Class getType()
        {
            return field.getType();
        }
 
        @Override
        public String getDbType()
        {
            return "Undefined";
        }
        
        @Override
        public void addFieldDefinitions( String prefix, Map<String,FieldType> defs )
        {
            prefix = prefix + name + "_";
                
            for( FieldType t : this.fields )
                t.addFieldDefinitions( prefix.toUpperCase(), defs );
        }
       
        @Override
        public void addFields( String prefix, List<String> fields )
        {
            prefix = prefix + name + "_";
                
            for( FieldType t : this.fields )
                t.addFields( prefix, fields );             
        }
    
        @Override
        public Object toDbValue( Object o )
        {
            return o;
        }
        
        @Override
        public int store( Object object, PreparedStatement ps, int index ) throws SQLException
        {                
            try
                {
                Object subValue = field.get(object);
                
                for( FieldType t : fields )
                    {
                    index = t.store( subValue, ps, index );
                    }
                return index;
                }
            catch( IllegalAccessException e )
                {
                throw new RuntimeException( "Error in field mapping", e );
                }
        }
    
        @Override
        public int load( Object target, ResultSet rs, int index ) throws SQLException
        {
            try
                {
                Object subValue = field.getType().newInstance();
                
                for( FieldType t : fields )
                    {
                    index = t.load( subValue, rs, index );
                    }
                
                field.set( target, subValue );
                return index;
                }
            catch( InstantiationException e )
                {
                throw new RuntimeException( "Error in field mapping", e );
                }
            catch( IllegalAccessException e )
                {
                throw new RuntimeException( "Error in field mapping", e );
                }
        }
        
        @Override
        public String toString()
        {
            return getFieldName() + ":" + getType() + "{" + Arrays.asList(fields) + "}";
        }
    }
    
    protected static class PrimitiveField implements FieldType
    {
        private String name;
        private String dbFieldName;
        private Field field;
        
        public PrimitiveField( Field field )
        {
            this( null, field );
        }
        
        public PrimitiveField( String prefix, Field field )
        {
            this.field = field;
            this.name = field.getName();
            if( prefix == null )
                dbFieldName = name;
            else
                dbFieldName = prefix + name;
        }
        
        @Override
        public String getFieldName()
        {
            return name;
        }
 
        @Override
        public Class getType()
        {
            return field.getType();
        }
        
        @Override
        public String getDbType()
        {
            String s = field.getType().getSimpleName();
            String result = dbTypes.get(s);
            if( result != null )
                return result;
            return s; 
        }
        
        @Override
        public void addFieldDefinitions( String prefix, Map<String,FieldType> defs )
        {
            defs.put( prefix + dbFieldName.toUpperCase(), this );
        }
        
        @Override
        public void addFields( String prefix, List<String> fields )
        {
            fields.add( prefix + dbFieldName );
        }
    
        @Override
        public Object toDbValue( Object o )
        {
            return o;
        }
        
        @Override
        public int store( Object object, PreparedStatement ps, int index ) throws SQLException
        {
            try
                {
                ps.setObject( index++, field.get(object) );
                return index;
                }
            catch( IllegalAccessException e )
                {
                throw new RuntimeException( "Error in field mapping", e );
                }
        }
 
        protected Object cast( Number n, Class c )
        {
            if( c == Float.TYPE )
                return n.floatValue();
            if( c == Byte.TYPE )
                return n.byteValue();
            if( c == Short.TYPE )
                return n.shortValue();
            if( c == Integer.TYPE )
                return n.intValue();
            return n;                
        }
    
        @Override
        public int load( Object target, ResultSet rs, int index ) throws SQLException
        {
            try
                {
                Object value = rs.getObject(index++);
                
                if( value instanceof Number )
                    value = cast( (Number)value, getType() );
                
                field.set( target, value );
                return index;
                }
            catch( IllegalAccessException e )
                {
                throw new RuntimeException( "Error in field mapping", e );
                }
        }
        
        @Override
        public String toString()
        {
            if( dbFieldName != name )
                return name + "/" + dbFieldName + ":" + getType();
            return getFieldName() + ":" + getType();
        }
    }             
}


