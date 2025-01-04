/*
 * $Id$
 *
 * Copyright (c) 2024 jMonkeyEngine
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

package com.simsilica.es.net;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *  Indicates EntitySet interaction errors that can be sent back to the client.
 *
 *  @author    Paul Speed
 */
@Serializable
public class EntitySetErrorMessage extends AbstractMessage {

    private static final Pattern LINE = Pattern.compile("(?m)^(.).*$");

    private int setId;
    private String error;;

    public EntitySetErrorMessage() {
    }

    public EntitySetErrorMessage( int setId, Throwable error ) {
        this.setId = setId;
        this.error = throwableToString("SERVER-ERROR:", error);
    }

    public static String throwableToString( String prefix, Throwable error ) {
        StringWriter sOut = new StringWriter();
        try( PrintWriter out = new PrintWriter(sOut) ) {
            error.printStackTrace(out);
        }
        String result = sOut.toString();
        Matcher m = LINE.matcher(result);
        return m.replaceAll(prefix + "$0");
    }

    public int getSetId() {
        return setId;
    }

    public String getError() {
        return error;
    }

    @Override
    public String toString() {
        return "EntitySetErrorMessage[" + setId + ", " + error + "]";
    }
}

