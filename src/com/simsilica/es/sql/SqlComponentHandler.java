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

import com.simsilica.es.base.ComponentHandler;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.ComponentFilter;
import java.sql.*;
import java.util.*;


/**
 *  Sql-based component handler for in-memory components.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class SqlComponentHandler<T extends EntityComponent> implements ComponentHandler<T>
{
    private SqlEntityData parent;
    private Class<T> type;
    private ComponentTable<T> table;
 
    public SqlComponentHandler( SqlEntityData parent, Class<T> type ) 
    {
        this.parent = parent;
        this.type = type;
        try
            {
            this.table = ComponentTable.create( parent.getSession(), type );
            }
        catch( SQLException e )
            {
            throw new RuntimeException( "Error creating table for component type:" + type, e );
            }
    }
    
    protected SqlSession getSession() throws SQLException
    {
        return parent.getSession();
    }
    
    @Override
    public void setComponent( EntityId entityId, T component )
    {
        try
            {
            table.setComponent( getSession(), entityId, component );
            }
        catch( SQLException e )
            {
            throw new RuntimeException( "Error setting component:" + component + " on entity:" + entityId, e ); 
            }
    }
    
    @Override
    public boolean removeComponent( EntityId entityId )
    {
        try
            {
            return table.removeComponent( getSession(), entityId );
            }
        catch( SQLException e )
            {
            throw new RuntimeException( "Error removing component type:" + type + " from entity:" + entityId ); 
            }
    }
    
    @Override
    public T getComponent( EntityId entityId )
    {
        try
            {       
            return (T)table.getComponent( getSession(), entityId );
            }
        catch( SQLException e )
            {
            throw new RuntimeException( "Error retrieving component type:" + type + " for entity:" + entityId, e );
            }       
    }
    
    @Override
    public Set<EntityId> getEntities()
    {
        try
            {
            return table.getEntityIds( getSession() );
            }
        catch( SQLException e )
            {
            throw new RuntimeException( "Error retrieving component entities for type:" + type );
            }
    }
     
    @Override
    public Set<EntityId> getEntities( ComponentFilter filter )
    {        
        if( filter == null )
            return getEntities();
        try
            {
            return table.getEntityIds( getSession(), filter );
            }
        catch( SQLException e )
            {
            throw new RuntimeException( "Error retrieving component entities for type:" + type, e );
            }
    }
                
    @Override
    public EntityId findEntity( ComponentFilter filter )
    {
        //throw new UnsupportedOperationException( "SQL-base direct entity look-up not yet implemented." );
        if( filter == null )
            return null;
        try
            {
            return table.getEntityId( getSession(), filter );
            }
        catch( SQLException e )
            {
            throw new RuntimeException( "Error retrieving entity for filter:" + filter, e );
            }
    } 
}
