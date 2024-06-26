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

package com.simsilica.es.filter;

import java.util.*;

import com.simsilica.es.ComponentFilter;
import com.simsilica.es.EntityComponent;

/**
 *  An AND filter that requires all component filters to be of
 *  the same type as the outer filter.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class AndFilter<T extends EntityComponent> implements ComponentFilter<T> {

    private Class<T> type;
    private ComponentFilter<? super T>[] operands;

    public AndFilter() {
    }

    @SafeVarargs
    public AndFilter( Class<T> type, ComponentFilter<? super T>... operands ) {
        this.type = type;
        this.operands = operands;
    }

    @SafeVarargs
    public static <T extends EntityComponent> AndFilter<T> create( Class<T> type,
                                                                   ComponentFilter<? super T>... operands ) {
        return new AndFilter<T>(type, operands);
    }

    public ComponentFilter<? super T>[] getOperands() {
        return operands;
    }

    @Override
    public Class<T> getComponentType() {
        return type;
    }

    @Override
    public boolean evaluate( EntityComponent c ) {
        if( !type.isInstance(c) ) {
            return false;
        }
        if( operands == null ) {
            return true;
        }

        for( ComponentFilter f : operands ) {
            if( !f.evaluate(c) ) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "AndFilter[" + Arrays.asList(operands) + "]";
    }
}

