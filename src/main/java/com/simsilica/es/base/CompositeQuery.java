/*
 * $Id$
 *
 * Copyright (c) 2024, Simsilica, LLC
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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import com.simsilica.es.EntityId;
import com.simsilica.es.Query;

/**
 *  Wraps a list of queries and executes them sequentially, intersecting
 *  the result of each with the previous.
 *
 *  @author    Paul Speed
 */
public class CompositeQuery implements Query {
    static Logger log = LoggerFactory.getLogger(CompositeQuery.class);

    private List<Query> children;

    public CompositeQuery( List<Query> children ) {
        this.children = children;
    }

    @Override
    public Set<EntityId> execute() {
        if( children.isEmpty() ) {
            return Collections.emptySet();
        }

        Set<EntityId> results = null;
        for( Query q : children ) {
            Set<EntityId> sub = q.execute();
            if( sub.isEmpty() ) {
                return Collections.emptySet();
            }
            if( results == null ) {
                results = new HashSet<>(sub);
            } else {
                results.retainAll(sub);
            }
        }

        return results;
    }

    @Override
    public EntityId findFirst() {
        if( children.isEmpty() ) {
            return null;
        }
        if( children.size() == 1 ) {
            // The one case where we can potentially optimized is where there
            // is only one child.
            return children.get(0).findFirst();
        }
        // Otherwise, we cannot do better than executing and returning the first one.
        Set<EntityId> all = execute();
        if( all.isEmpty() ) {
            return null;
        }
        return all.iterator().next();
    }

    public Query join( Query other ) {
        // Merging is possible but potentially non-trivial so I'll wait for
        // a use-case.  For example, should we do a child list to child list
        // (m x n) merge in case there are overlapping child Queries that can
        // be merged?
        return null;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getSimpleName())
            .add("children", children)
            .toString();
    }
}


