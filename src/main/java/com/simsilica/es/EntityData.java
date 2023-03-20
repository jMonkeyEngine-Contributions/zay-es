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

/**
 *  The main entry point for retrieving entities
 *  and components.
 *
 *  @author    Paul Speed
 */
public interface EntityData {

    public EntityId createEntity();
    public void removeEntity( EntityId entityId );
    
    public <T extends EntityComponent> void setComponent( EntityId entityId, T component );
    public void setComponents( EntityId entityId, EntityComponent... components );
    public <T extends EntityComponent> boolean removeComponent( EntityId entityId, Class<T> type );
    public <T extends EntityComponent> void removeComponents( EntityId entityId, Class<T>... types );

    public <T extends EntityComponent> T getComponent( EntityId entityId, Class<T> type );
    
    public Entity getEntity( EntityId entityId, Class... types );
    public EntityId findEntity( ComponentFilter filter, Class... types );
    public Set<EntityId> findEntities( ComponentFilter filter, Class... types );
    
    public EntitySet getEntities( Class... types );
    public EntitySet getEntities( ComponentFilter filter, Class... types );

    public WatchedEntity watchEntity( EntityId entityId, Class... types );

    public StringIndex getStrings();
    
    public void close();  
}
