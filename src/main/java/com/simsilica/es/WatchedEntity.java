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

package com.simsilica.es;

import java.util.Set;


/**
 *  A specialized entity that can be updated like an EntitySet,
 *  thus effectively providing an EntitySet of one.  This is useful
 *  for monitoring a single entity as one would monitor a set since
 *  there is no way to filter an EntitySet by a single ID.
 *
 *  <p>Unlike the entities in an EntitySet, this Entity can actually
 *  have null values for some components depending on their current
 *  state.  The WatchedEntity is watching a specific ID with a subset
 *  of its components but is not using the components for any kind of
 *  "membership" inclusion (like EntitySet does).</p> 
 *
 *  @author    Paul Speed
 */
public interface WatchedEntity extends Entity {
    
    /**
     *  Returns true if this entity has changes
     *  ready to be applied.
     */
    public boolean hasChanges();
    
    /**
     *  Applies any accumulated changes to this entity since
     *  the last time applyChanges() was called and returns true
     *  if changes were applied.  
     */
    public boolean applyChanges();
    
    /**
     *  Applies any accumulated changes to this entity since
     *  the last time applyChanges() was called and returns true
     *  if changes were applied.  Changes that caused an update 
     *  will be added to the supplied updates set.
     */
    public boolean applyChanges( Set<EntityChange> updates );        

    /**
     *  Releases this entity from processing further entity
     *  updates.  After this call hasChanges() will always return
     *  false and applyChanges() will do nothing.
     */
    public void release();
}
