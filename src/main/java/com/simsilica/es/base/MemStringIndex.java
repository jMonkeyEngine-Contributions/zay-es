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

package com.simsilica.es.base;

import com.simsilica.es.StringIndex;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 *  An in-memory version of the StringIndex interface.
 *
 *  @author    Paul Speed
 */
public class MemStringIndex implements StringIndex {

    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<String, Integer> index = new HashMap<String, Integer>();
    private Map<Integer, String> strings = new HashMap<Integer, String>();
    private int nextId = 0;   
    
    public MemStringIndex() {
    }

    @Override
    public int getStringId( String s, boolean add ) {
        
        // See if it already exists first
        lock.readLock().lock();
        try {
            Integer result = index.get(s);
            if( result == null && add ) {
                // Ok, so we need to convert to a write lock
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    // Check one more time in case another thread beat us to
                    // it.
                    result = index.get(s);
                    if( result == null ) {
                        // we still need to create it
                        result = nextId++;
                        index.put(s, result);
                        strings.put(result, s);
                    }
                } finally {
                    // Reverse the lock state... we don't 
                    // technically need the read lock anymore but
                    // it makes the logic easier
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
            }
            return result != null ? result : -1;
        } finally {
            lock.readLock().unlock();
        }   
        
    }
    
    @Override
    public String getString( int id ) {
 
        lock.readLock().lock();
        try {   
            return strings.get(id);
        } finally { 
            lock.readLock().unlock();
        }
    }    
}

