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

package com.simsilica.es;


/**
 *  Uniquely identifies strings by int id in a persistent
 *  way that can be stored in components.
 *
 *  <p>It is often the case where it is desirable to map some commonly
 *  used set of strings to integer IDs for transfer over the network or 
 *  for database storage.  These are cases where the code wants the flexibility
 *  of strings, has a limited number of values, and wants to keep storage or
 *  transfer space compact.  For example, object type names, shape names, 
 *  etc..  The number is finite but not necessarily known ahead of time.  In
 *  these cases, static constants can be clumsy and don't play nice with debug
 *  logs.  Enums are limited to a hard-coded set of values.  Neither of these
 *  are quite as flexible as strings for some use-cases.</p>  
 *
 *  <p>Because the number of strings is finite, sending full strings over the wire 
 *  and/or storing them in a database is wasteful. String index lets you convert 
 *  them to/from unique integer IDs that are safely stored in the ES... and safely
 *  transferred over the network in a Zay-ES-net setup.</p>
 *
 *  <pre>
 *  int myId = stringIndex.getStringId("My String", true);
 *  ...
 *  String s = stringIndex.getString(myId);
 *  s.equals("My String") == true
 *  </pre>
 *
 *  <p>And in the getString() case, it could have been called on a remote client
 *  and still find the string.</p>
 *
 *  @author    Paul Speed
 */
public interface StringIndex {
    /**
     *  Returns an existing integer ID mapped to the specified string or if 'add' is
     *  true then it will create one if it doesn't already exist.
     *  Returns -1 if add=false and the string has not been previously mapped.
     */
    public int getStringId(String s, boolean add);
    
    /**
     *  Returns the string value for a previously registered string.  
     */
    public String getString(int id);    
}
