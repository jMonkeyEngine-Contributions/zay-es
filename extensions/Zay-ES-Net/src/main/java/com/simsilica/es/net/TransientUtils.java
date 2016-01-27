/*
 * $Id$
 * 
 * Copyright (c) 2015, Simsilica, LLC
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

package com.simsilica.es.net;

import com.simsilica.es.EntityComponent;
import com.simsilica.es.TransientComponent;


/**
 *  Some utils for cleaning transient component references
 *  or arrays of references.
 *
 *  @author    Paul Speed
 */
public class TransientUtils {

    private static boolean supportTransients = true;
    
    /**
     *  Turns suport for transient components on/off.  There is a 
     *  small performance penalty to check arrays for transient components.
     *  Setting this option to false will cause the checks to be skipped.
     *  Defaults to true so checks will be performed by default.
     */
    public static void setSupportTransientComponents( boolean f ) {
        supportTransients = f;
    }
    
    public static boolean getSupportTransientComponents() {
        return supportTransients;
    }
    
    /**
     *  If there are no transient components in the specified array
     *  then it is returned directly, else a clone is returned with null
     *  elements where any transient components previously existed.
     */
    public static <T extends EntityComponent> T[] safeClean( T[] array ) {
        if( array == null ) {
            return null;
        }
        if( !supportTransients ) {
            return array;
        }
        T[] result = null;
        for( int i = 0; i < array.length; i++ ) {
            if( array[i] instanceof TransientComponent ) {
                if( result == null ) {
                    // Clone the array to make a result
                    result = array.clone();
                }
                result[i] = null;
            }
        }
        return result == null ? array : result;
    }
    
    /**
     *  If there are no transient components in the specified array
     *  they are nulled out if supportTransients is true.
     */
    public static <T extends EntityComponent> T[] clean( T[] array ) {
        if( array == null ) {
            return null;
        }
        if( !supportTransients ) {
            return array;
        }
        for( int i = 0; i < array.length; i++ ) {
            if( array[i] instanceof TransientComponent ) {
                array[i] = null;
            }
        }
        return array;
    }

    public static <T extends EntityComponent> T clean( T component ) {
        if( component instanceof TransientComponent && supportTransients ) {
            return null;
        }
        return component;
    } 
}
