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

import java.util.*;
import java.util.concurrent.*;

import org.apache.log4j.Logger;



/**
 *  A set of entities that possess certain components with
 *  automatic updates as the entity components change.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public interface EntitySet extends Set<Entity>
{
    /**
     *  Swaps out the current main filter for a new one.  The
     *  changes will show up the next time applyChanges() is called.
     */
    public void resetFilter( ComponentFilter filter );
 
    public boolean containsId( EntityId id );
 
    public Entity getEntity( EntityId id );
    
    /**
     *  Returns the entities that were added during applyChanges().
     */
    public Set<Entity> getAddedEntities();

    /**
     *  Returns the entities that were changed during applyChanges().
     */
    public Set<Entity> getChangedEntities();

    /**
     *  Returns the entities that were removed during applyChanges().
     */
    public Set<Entity> getRemovedEntities();

    /**
     *  Clears all pending change sets accumulated during the last
     *  applyChanges().  The change sets are automatically cleared
     *  at the beginning of the next applyChanges() but sometimes
     *  it can be useful to free them early (if the change set is large, 
     *  etc.).
     */
    public void clearChangeSets();

    /**
     *  Returns true if there were entity changes during the last
     *  applyChanges().
     */
    public boolean hasChanges();

    /**
     *  Applies any accumulated changes to this list's entities since
     *  the last time it was called and returns true if there were
     *  changes.
     */
    public boolean applyChanges();
 
    /**
     *  Applies any accumulated changes to this list's entities since
     *  the last time it was called and returns true if there were
     *  changes.  Changes that caused an update (not an add or a remove)
     *  will be added to the supplied updates set.
     */
    public boolean applyChanges( Set<EntityChange> updates );

    /**
     *  Releases this entity set from processing further entity
     *  updates.  The entities contained in the set will remain
     *  until garbage collected normally or until clear() is
     *  called.
     */
    public void release();
 
    public boolean hasType( Class type );
}

