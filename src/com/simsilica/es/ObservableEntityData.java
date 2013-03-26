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
 *  Represents a specialization of EntityData the can report
 *  changes about the entities it manages.  This is useful
 *  for server-side dispatch of relevant changes to other
 *  remote clients.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public interface ObservableEntityData extends EntityData
{
    public void addEntityComponentListener( EntityComponentListener l );
    public void removeEntityComponentListener( EntityComponentListener l );
    
    // This is technically anti-ES to do this.  The thing is that
    // for something like ropes and springs that connect multiple
    // entities, it's cheaper than trying to coordinate entity sets
    // for every entity or be able to change interest on the fly.
    // maybe when real physics networking is implemented there will
    // be an easy way to treat physical links like the other buffered
    // position data so that the client will only see data for their
    // zone.  In fact, it's pretty good thought that might be true.     
    public ChangeQueue getChangeQueue( Class... componentType );
    public void releaseChangeQueue( ChangeQueue queue );   
}
