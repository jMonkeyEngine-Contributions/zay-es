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

package com.simsilica.util;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 *  Keeps track of different types of reporters for generating
 *  status reports on various things.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class ReportSystem
{
    public static final String REPORT_CACHE = "cache";
    
    private static Map<String,List<Reporter>> reporters = new ConcurrentHashMap<String,List<Reporter>>();
    
    private static List<Reporter> getList(String reportType)
    {
        List<Reporter> list = reporters.get(reportType);
        if( list == null )
            {
            list = new CopyOnWriteArrayList<Reporter>();
            reporters.put(reportType, list);
            }
        return list;
    }
    
    public static void registerCacheReporter( Reporter r )
    {
        registerReporter( REPORT_CACHE, r );
    }
       
    public static void registerReporter( String type, Reporter r )
    {
        getList(type).add(r);
    }   
 
    public static void printReport( String type, PrintWriter out )
    {
        for( Reporter r : getList(type) )
            r.printReport( type, out ); 
    }
    
    public static String getReport( String type )
    {
        StringWriter sOut = new StringWriter();
        PrintWriter out = new PrintWriter(sOut);
        printReport( type, out );
        out.close();
        return sOut.toString();                
    }
}
