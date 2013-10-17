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

import com.simsilica.es.base.EntityIdGenerator;
import java.sql.*;

/**
 *  Hands out new IDs.
 *
 *  @author    Paul Speed
 */
public class PersistentEntityIdGenerator implements EntityIdGenerator {

    private SqlEntityData parent;
    private String tableName = "ENTITY_ID";
    private long entityId;

    protected PersistentEntityIdGenerator( SqlEntityData parent ) throws SQLException {
        this.parent = parent;
        
        // See if the table exists
        SqlSession session = parent.getSession();
        DatabaseMetaData md = session.getConnection().getMetaData();
        ResultSet rs = md.getColumns(null, "PUBLIC", tableName, null);
        try {
            if( rs.next() ) {
                loadId(session);
                return;
            }
        } finally {
            rs.close();
        }

        // Really should have a separate class for this
        StringBuilder sb = new StringBuilder( "CREATE" );
        // If we don't create a cached table then all changes are lost if
        // we don't shut down cleanly.  Not good for entity Id.
        // Actually, that's true even for cached... if we don't wait long
        // enough.  MEMORY is worse though since all changes are lost without
        // a proper shutdown.  CACHED at least (by default) writes every 500 ms.
        // It can be changed... looking into it.
        // Well, it cannot be set per table... so we should add
        // safeguards if we can.
        sb.append(" CACHED TABLE");
        sb.append(" " + tableName + "\n");
        sb.append("(\n");
        sb.append("  id TINYINT,\n");
        sb.append("  entityId BIGINT");
        sb.append("\n)");
        
        Statement st = session.getConnection().createStatement();    
        st.executeUpdate(sb.toString());
        
        // And insert the initial zero record
        String sql = "INSERT INTO " + tableName + "(id,entityId) VALUES (0,0)"; 
        int i = st.executeUpdate(sql);
        if( i != 1 ) {
            throw new SQLException("Error initializing sequence table:" + sb);
        }
        st.close();    
    }

    public static PersistentEntityIdGenerator create( SqlEntityData parent ) throws SQLException {
        return new PersistentEntityIdGenerator(parent); 
    } 
    
    protected void loadId( SqlSession session ) throws SQLException {
    
        Statement st = session.getConnection().createStatement();
        try {
            ResultSet rs = st.executeQuery("SELECT entityId from " + tableName + " where id=0");
            if( rs.next() ) {
                entityId = rs.getLong(1);
            }
        } finally {
            st.close();
        }    
    }
 
    public synchronized long nextEntityId() {
    
        long result = entityId++;
        try {
            SqlSession session = parent.getSession();        
            Statement st = session.getConnection().createStatement();
            try {
                // Write the next value
                String sql = "UPDATE " + tableName + " SET entityId=" + entityId + " WHERE id=0";
                int update = st.executeUpdate(sql);
                if( update != 1 ) {
                    throw new SQLException("EntityID sequence not updated.");
                }
                return result;
            } finally {
                st.close();
            }
        } catch( SQLException e ) {
            throw new RuntimeException("Error persisting entity ID", e);
        }                
    }
    
}

