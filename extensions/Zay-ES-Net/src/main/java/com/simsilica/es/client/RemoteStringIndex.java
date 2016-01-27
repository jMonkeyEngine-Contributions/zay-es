/*
 * $Id$
 * 
 * Copyright (c) 2015, Simsilica, LLC
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

package com.simsilica.es.client;

import com.simsilica.es.StringIndex;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *  A client implementation of the StringIndex interface.
 *
 *  @author    Paul Speed
 */
public class RemoteStringIndex implements StringIndex {
    // I've implemented this as a separate class from RemoteEntityData
    // to make the caching configuration easier... but really it's just
    // delegating directly back to that class for cache misses.
    private RemoteEntityData remote; 
    
    private Map<String, Integer> idIndex = new ConcurrentHashMap<String, Integer>();
    private Map<Integer, String> stringIndex = new ConcurrentHashMap<Integer, String>();
    private ReadWriteLock indexLock = new ReentrantReadWriteLock();
    
    protected RemoteStringIndex( RemoteEntityData remote ) {
        this.remote = remote;
    }
     
    @Override
    public int getStringId( String s, boolean add ) {
        if( add == true ) {
            throw new UnsupportedOperationException("Clients cannot create new string mappings.");
        }
        indexLock.readLock().lock();
        try {
            Integer result = idIndex.get(s);
            // Note: misses are not cached... we'd have no way to detect
            //       if they were filled in later.
            if( result != null ) {
                // We're done.
                return result;
            }
        } finally {
            indexLock.readLock().unlock();
        }
        
        // Otherwise...    
        // Need to look it up... which means we need to
        // grab the write lock
        // Must release the read lock first
        indexLock.writeLock().lock();
        try {
            // Check the result again because between unlock
            // and lock something else might have requested the same
            // string.
            Integer result = idIndex.get(s);
            if( result != null ) {
                return result;
            }
            
            // Else ask remote
            result = remote.getStringId(s);
            if( result != null ) {
                idIndex.put(s, result);
                stringIndex.put(result, s);
            }
            return result;                   
        } finally {
            indexLock.writeLock().unlock();
        }               
    }
    
    @Override
    public String getString( int id ) {
    
        indexLock.readLock().lock();
        try {
            String result = stringIndex.get(id);
            // Note: misses are not cached... we'd have no way to detect
            //       if they were filled in later.
            if( result != null ) {
                // We're done.
                return result;
            }
        } finally {
            indexLock.readLock().unlock();
        }
        
        // Otherwise...    
        // Need to look it up... which means we need to
        // grab the write lock
        // Must release the read lock first
        indexLock.writeLock().lock();
        try {
            // Check the result again because between unlock
            // and lock something else might have requested the same
            // string.
            String result = stringIndex.get(id);
            if( result != null ) {
                return result;
            }
            
            // Else ask remote
            result = remote.getString(id);
            if( result != null ) {
                idIndex.put(result, id);
                stringIndex.put(id, result);
            }
            return result;                   
        } finally {
            indexLock.writeLock().unlock();
        }                   
    }
        
}


