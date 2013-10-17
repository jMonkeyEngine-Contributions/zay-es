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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.sql.*;

import com.simsilica.es.StringIndex;
import com.simsilica.util.ReportSystem;
import com.simsilica.util.Reporter;

/**
 *  Sql-based StringIndex implementation with basic LRU cache.
 *
 *  @author    Paul Speed
 */
public class SqlStringIndex implements StringIndex {

    private SqlEntityData parent;
    private StringTable stringTable;
    private Cache<Integer,String> idToString;      
    private Cache<String,Integer> stringToId;      
 
    public SqlStringIndex( SqlEntityData parent, int cacheSize ) {
        this.parent = parent;
    
        this.idToString = CacheBuilder.newBuilder().maximumSize(cacheSize).build();
        this.stringToId = CacheBuilder.newBuilder().maximumSize(cacheSize).build();

        ReportSystem.registerCacheReporter(new CacheReporter());
        
        try {
            this.stringTable = StringTable.create(parent.getSession()); 
        } catch( SQLException e ) {
            throw new RuntimeException("Error creating string table", e);
        }
    }
    
    protected SqlSession getSession() throws SQLException {
        return parent.getSession();
    }

    // A safe lookup with no adds... easier than try/catching all
    // over the place
    protected int lookupId( String s ) {
        try {
            return stringTable.getStringId(getSession(), s, false);
        } catch( SQLException e ) {
            throw new RuntimeException("Error getting string ID for:" + s, e);
        }
    }

    @Override
    public int getStringId( String s, boolean add ) {
    
        Integer result = stringToId.getIfPresent(s);
        if( result != null ) {
            return result;
        }

        // Try a naked lookup
        int i = lookupId(s);
        if( i < 0 && add ) { 
            synchronized( this ) {
                // Check the cache again... the string may have been added
                // while we were waiting for the synch and if so then it will
                // still be cached (presuming we aren't tearing through ID lookups
                // like madmen.)           
                result = stringToId.getIfPresent(s);
                if( result != null ) {
                    return result;
                }
                
                try {
                    i = stringTable.getStringId(getSession(), s, add);
                    if( i < 0 ) {
                        return -1;
                    }
    
                    // For the easy double-checked locking to work, we
                    // need to cache inside the synch block.  We could have
                    // just done another DB look-up above and then avoided this
                    // but this might be a little faster in heavy-contention.                                       
                    stringToId.put(s, i);
                    idToString.put(i, s);
                    return i;           
                } catch( SQLException e ) {
                    throw new RuntimeException("Error getting string ID for:" + s, e);
                }
            }
        }
        
        // If we weren't adding above then it might still be negative
        if( i < 0 ) {
            return -1;
        }
                                        
        stringToId.put(s, i);
        idToString.put(i, s);
 
        return i;           
    }
    
    @Override
    public String getString( int id ) {
    
        String result = idToString.getIfPresent(id);
        if( result != null ) {
            return result;
        }
            
        try {
            result = stringTable.getString(getSession(), id);
            if( result != null ) {
                idToString.put(id, result);
                stringToId.put(result, id);
            }
            return result;
        } catch( SQLException e ) {
            throw new RuntimeException("Error getting string for ID:" + id, e);
        }
    }
        
    private class CacheReporter implements Reporter {
    
        @Override
        public void printReport( String type, java.io.PrintWriter out ) {
            out.println("SqlStringIndex->id to string:" + idToString.size() 
                            + " stats:" + idToString.stats());
            out.println("SqlStringIndex->string to id:" + stringToId.size() 
                            + " stats:" + stringToId.stats());
        }
    }            
}
