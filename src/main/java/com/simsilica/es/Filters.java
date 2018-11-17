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

import com.simsilica.es.filter.*;


/**
 *  Static utility methods pertaining to ComponentFilter instances. 
 *
 *  @author    Paul Speed
 */
public class Filters {

    /**
     *  Creates a ComponentFilter that returns true if the specified
     *  field of the specified component type matches the specified value.
     *  Returns false for all other cases: different type, field doesn't exist, or value
     *  is different.
     *  The value comparison is done with .equals().  Two null values are considered equal.
     */
    public static <T extends EntityComponent> ComponentFilter<T> fieldEquals( Class<T> type, String field, Object value ) {
        return FieldFilter.create( type, field, value );    
    }

    /**
     *  Creates a ComponentFilter that returns true if any of the 
     *  supplied filters are true.  The OR filter will early out and stop
     *  at the first true filter.  Child filters are evaluated in the order
     *  provided.
     */
    @SuppressWarnings("unchecked")  // because Java doesn't like generic varargs
    public static <T extends EntityComponent> ComponentFilter<T> or( Class<T> type, ComponentFilter<? super T>... operands ) {
        return OrFilter.create( type, operands );
    }

    /**
     *  Creates a ComponentFilter that returns true if all of the 
     *  supplied filters are true.  The AND filter will early out and stop
     *  at the first false filter.  Child filters are evaluated in the order
     *  provided.
     */
    @SuppressWarnings("unchecked")  // because Java doesn't like generic varargs
    public static <T extends EntityComponent> ComponentFilter<T> and( Class<T> type, ComponentFilter<? super T>... operands ) {
        return AndFilter.create( type, operands );    
    }    
}
