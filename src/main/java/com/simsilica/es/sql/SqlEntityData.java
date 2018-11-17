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

import java.io.File;
import java.sql.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simsilica.es.EntityComponent;
import com.simsilica.es.PersistentComponent;
import com.simsilica.es.base.ComponentHandler;
import com.simsilica.es.base.DefaultEntityData;


/**
 *  EntityData implementation that uses SQL tables to
 *  store persistent information.
 *
 *  @author    Paul Speed
 */
public class SqlEntityData extends DefaultEntityData {

    static Logger log = LoggerFactory.getLogger(SqlEntityData.class);
    
    private String dbPath;
    private ThreadLocal<SqlSession> cachedSession = new ThreadLocal<SqlSession>();
 
    public SqlEntityData( File dbPath, long writeDelay ) throws SQLException {
        this(dbPath.toURI().toString(), writeDelay);
    }
    
    public SqlEntityData( String dbPath, long writeDelay ) throws SQLException {
    
        super(null);
        
        this.dbPath = dbPath;

        try {
            // Hard code this stuff for now.
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch( ClassNotFoundException e ) {
            throw new SQLException("Driver not found for: org.hsqldb.jdbc.JDBCDriver", e); 
        }
        
        // In a stand-alone client we will want a very quick write delay
        // to avoid crash-related mayhem.
        execute("SET FILES WRITE DELAY " + writeDelay + " MILLIS");
        execute("SET FILES DEFRAG 50");
               
        setIdGenerator(PersistentEntityIdGenerator.create( this )); 
        setStringIndex(new SqlStringIndex( this, 100 )); 
    }
 
    protected void execute( String statement ) throws SQLException {
        SqlSession session = getSession();
        Statement st = session.getConnection().createStatement();
        try {
            st.execute(statement);
        } finally {
            st.close();
        }
    }
    
    protected SqlSession getSession() throws SQLException {
        SqlSession session = cachedSession.get();
        if( session != null ) {
            return session;
        }
 
        // Soooo... apparently hsqldb doesn't like proper
        // encoded URIs.
        dbPath = dbPath.replaceAll("%20", " ");
                    
        Connection conn = DriverManager.getConnection("jdbc:hsqldb:" + dbPath + "/entity_db",    
                                                      "SA", "");
 
        log.info("Created connection.  Autocommit:" + conn.getAutoCommit());
                                                                                     
        session = new SqlSession(conn);
        cachedSession.set(session);        
        return session;
    } 

    @Override
    protected <T extends EntityComponent> ComponentHandler<T> lookupDefaultHandler( Class<T> type ) {
        if( PersistentComponent.class.isAssignableFrom(type) ) {
            return new SqlComponentHandler<T>(this, type);
        }
        return super.lookupDefaultHandler(type);
    }
 
    @Override
    public void close() {   
        super.close();
        try {
            // Shut the database down
            SqlSession session = getSession();
            execute("SHUTDOWN COMPACT");
            session.getConnection().close();    
        } catch( SQLException e ) {
            throw new RuntimeException("Database was not shutdown cleanly", e);
        }
    }
    
}
