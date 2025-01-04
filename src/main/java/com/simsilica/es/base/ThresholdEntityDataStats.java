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

import org.slf4j.*;

import com.simsilica.es.EntityCriteria;
import com.simsilica.es.Query;

/**
 *  Logs a warning whenever an operation exceeds a treshold.
 *
 *  @author    Paul Speed
 */
public class ThresholdEntityDataStats implements EntityDataStats {
    static Logger log = LoggerFactory.getLogger(ThresholdEntityDataStats.class);

    private final long thresholdNanos;
    private final boolean includeStackTrace;

    /**
     *  Creates a ThresholdEntityDataStats object that will log a warning if calls take
     *  long than 13 ms and will include the stack trace in that log message.
     */
    public ThresholdEntityDataStats() {
        this(13000000L, true);
    }

    /**
     *  Creates a ThresholdEntityDataStats object that will log a warning if a call
     *  exceeds the specified threshold.  If includStackTrace is true then the call
     *  stack will also be included in the log call.
     */
    public ThresholdEntityDataStats( long thresholdNanos, boolean includeStackTrace ) {
        this.thresholdNanos = thresholdNanos;
        this.includeStackTrace = includeStackTrace;
    }

    @Override
    public void findEntities( long nanos, EntityCriteria criteria, Query query, int results ) {
        if( nanos > thresholdNanos ) {
            String msg = String.format("findEntities() threshold exceeded: %.03f ms, %d results, %s, %s",
                                    nanos/1000000.0,
                                    results,
                                    criteria,
                                    query
                                  );
            if( includeStackTrace ) {
                log.warn(msg, new Throwable("stack-trace"));
            } else {
                log.warn(msg);
            }
        }
    }

    @Override
    public void findEntity( long nanos, EntityCriteria criteria, Query query, boolean found ) {
        if( nanos > thresholdNanos ) {
            String msg = String.format("findEntity() threshold exceeded: %.03f ms, found: %s, %s, %s",
                                    nanos/1000000.0,
                                    found,
                                    criteria,
                                    query
                                  );
            if( includeStackTrace ) {
                log.warn(msg, new Throwable("stack-trace"));
            } else {
                log.warn(msg);
            }
        }
    }
}
