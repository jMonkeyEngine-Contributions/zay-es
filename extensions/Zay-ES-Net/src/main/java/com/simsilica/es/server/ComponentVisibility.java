/*
 * $Id$
 * 
 * Copyright (c) 2018, Simsilica, LLC
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

package com.simsilica.es.server;

import java.util.*;

import com.simsilica.es.*;

/**
 *  Controls the client visibility of certain component instances.
 *  For certain types of components, it may be limited what the client
 *  can actually see at any given time based on location, privileges,
 *  etc..  These ComponentVisibility strategy objects can control 
 *  this by intercepting the normal component-retrieving calls and
 *  returning null instead.  Additionally, it can be called occasionaly
 *  to generate the component removal events that the client will need
 *  to see when a component is no longer visible.   
 *
 *  @author    Paul Speed
 */
public interface ComponentVisibility {

    /**
     *  Returns the type of component of which this ComponentVisibility
     *  strategy controls the visibility.
     */
    public Class<? extends EntityComponent> getComponentType();

    /**
     *  Called to initialize this ComponentVisibilty strategy once attached
     *  to an EntityDataWrapper.  The supplied EntityData is the original EntityData
     *  object that the EntityDataWrapper wraps.
     */
    public void initialize( EntityData delegate );

    /**
     *  Returns the visibility filtered value of the component for the specified
     *  entity or null if the component either isn't set or is invisible.
     */
    public <T extends EntityComponent> T getComponent( EntityId entityId, Class<T> type );

    /**
     *  Returns all of the entity IDs that match the specified filter and are
     *  visible.  If filter is null then all visible entity IDs for this ComponentVisibility's
     *  component type are returned.  
     */
    public Set<EntityId> getEntityIds( ComponentFilter filter );
 
    /** 
     *  Recalculates the current visible set and returns the differences as
     *  EntityChanges.
     */   
    public boolean collectChanges( Queue<EntityChange> changes );
    
}
