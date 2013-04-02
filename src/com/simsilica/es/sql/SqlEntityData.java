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

import com.simsilica.es.base.MapComponentHandler;
import com.simsilica.es.base.ComponentHandler;
import com.simsilica.es.EntityChange;
import com.simsilica.es.PersistentComponent;
import com.simsilica.es.base.AbstractEntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.StringIndex;
import com.simsilica.es.ComponentFilter;
import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.log4j.Logger;


/**
 *  EntityData implementation that uses SQL tables to
 *  store persistent information.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class SqlEntityData extends AbstractEntityData
{
    static Logger log = Logger.getLogger(SqlEntityData.class);
    
    private String dbPath;
    private ThreadLocal<SqlSession> cachedSession = new ThreadLocal<SqlSession>();
 
    private Map<Class, ComponentHandler> handlers = new ConcurrentHashMap<Class, ComponentHandler>();    
    private PersistentEntityIdGenerator idGenerator;
 
    private SqlStringIndex stringIndex;

    // Somehow we want to be able to query across multiple tables
    // if we can.  So if all of the components are persistent, this
    // should give us a leg up.  But maybe it doesn't matter in practice.
 
    public SqlEntityData( File dbPath, long writeDelay ) throws SQLException
    {
        this( dbPath.toURI().toString(), writeDelay );
    }
    
    public SqlEntityData( String dbPath, long writeDelay ) throws SQLException
    {
        this.dbPath = dbPath;

        try
            {
            // Hard code this stuff for now.
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
            }
        catch( ClassNotFoundException e )
            {
            throw new SQLException( "Driver not found for: org.hsqldb.jdbc.JDBCDriver", e ); 
            }
        
        SqlSession session = getSession();
        
        // In a stand-alone client we will want a very quick write delay
        // to avoid crash-related mayhem.
        execute( "SET FILES WRITE DELAY " + writeDelay + " MILLIS" );
        execute( "SET FILES DEFRAG 50" );
               
        idGenerator = PersistentEntityIdGenerator.create( this );
        
        stringIndex = new SqlStringIndex( this, 100 ); 
    }
 
    protected void execute( String statement ) throws SQLException 
    {
        SqlSession session = getSession();
        Statement st = session.getConnection().createStatement();
        try
            {
            st.execute(statement);
            }
        finally 
            {
            st.close();
            }
    }
    
    protected SqlSession getSession() throws SQLException
    {        
        SqlSession session = cachedSession.get();
        if( session != null )
            return session;
 
        // Soooo... apparently hsqldb doesn't like proper
        // encoded URIs.
        dbPath = dbPath.replaceAll( "%20", " " );
                    
        Connection conn = DriverManager.getConnection("jdbc:hsqldb:" + dbPath + "/entity_db",    
                                                      "SA", "");
 
        log.info( "Created connection.  Autocommit:" + conn.getAutoCommit() );
                                                                                     
        session = new SqlSession(conn);
        cachedSession.set(session);        
        return session;
    } 
 
    @Override
    public StringIndex getStrings()
    {
        return stringIndex;
    }
 
    @Override
    public void close()
    {
        super.close();
        try
            {
            // Shut the database down
            SqlSession session = getSession();
            execute("SHUTDOWN COMPACT");
            session.getConnection().close();    
            }
        catch( SQLException e )
            {
            throw new RuntimeException( "Database was not shutdown cleanly", e );
            }
    }
    
    @Override
    public EntityId createEntity()
    {
        return new EntityId(idGenerator.nextEntityId());
    }

    @Override
    public void removeEntity( EntityId entityId )
    {
        // Note: because we only add the ComponentHandlers when
        // we encounter the component types... it's possible that
        // the entity stays orphaned with a few components if we
        // have never accessed any of them.  SqlEntityData should
        // probably specifically be given types someday.  FIXME
    
        // Remove all of its components
        for( Class c : handlers.keySet() )
            removeComponent( entityId, c );
    }

    protected ComponentHandler getHandler( Class type )
    {
        ComponentHandler result = handlers.get(type);
        if( result == null )
            {
            // A little double checked locking to make sure we 
            // don't create a handler twice
            synchronized( this )
                {
                result = handlers.get(type);
                if( result == null )
                    {
                    if( PersistentComponent.class.isAssignableFrom(type) )
                        result = new SqlComponentHandler( this, type );
                    else
                        result = new MapComponentHandler();
                    handlers.put(type, result);
                    }
                }
            }
        return result;             
    }

    @Override
    public <T extends EntityComponent> T getComponent( EntityId entityId, Class<T> type )
    {
        ComponentHandler handler = getHandler(type);
        return (T)handler.getComponent( entityId );
    }
    
    @Override
    public void setComponent( EntityId entityId, EntityComponent component )
    {
        ComponentHandler handler = getHandler(component.getClass());
        handler.setComponent( entityId, component );
        
        // Can now update the entity sets that care
        entityChange( new EntityChange( entityId, component ) ); 
    }
    
    @Override
    public boolean removeComponent( EntityId entityId, Class type )  
    {
        ComponentHandler handler = getHandler(type);
        boolean result = handler.removeComponent(entityId);
        
        // Can now update the entity sets that care
        entityChange( new EntityChange( entityId, type ) );
        
        return result; 
    }

    @Override
    protected EntityId findSingleEntity( ComponentFilter filter )
    {
        return getHandler(filter.getComponentType()).findEntity(filter);
    }

    @Override
    protected Set<EntityId> getEntityIds( Class type )
    {
        return getHandler(type).getEntities();
    }

    @Override
    protected Set<EntityId> getEntityIds( Class type, ComponentFilter filter )
    {
        return getHandler(type).getEntities( filter );
    }

}
