/*
 * $Id$
 *
 * Copyright (c) 2013 jMonkeyEngine
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

import com.jme3.network.Message;
import com.jme3.network.MessageConnection;
import com.jme3.network.MessageListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *  A MessageListener implementation that will forward messages to methods
 *  of a delegate object.  These methods can be automapped or specifically
 *  specified.  Subclasses provide specific implementations for how to
 *  find the actual delegate object.
 *
 *  @author    Paul Speed
 */
public abstract class AbstractMessageDelegator<S extends MessageConnection> 
                                implements MessageListener<S> {
                                
    static Logger log = LoggerFactory.getLogger(AbstractMessageDelegator.class);                                
                                
    private Class delegateType;
    private Map<Class, Method> methods = new HashMap<Class, Method>();
    private Class[] messageTypes;
    
    protected AbstractMessageDelegator( Class delegateType, boolean automap ) {
        this.delegateType = delegateType;
        if( automap ) {
            automap();
        }
    }
 
    public Class[] getMessageTypes() {
        if( messageTypes == null ) {
            messageTypes = methods.keySet().toArray(new Class[methods.size()]);
        }
        return messageTypes;
    }
 
    protected boolean isValidMethod( Method m, Class messageType ) {
 
        if( log.isTraceEnabled() ) {
            log.trace("isValidMethod(" + m + ", " + messageType + ")");
        }
               
        // Parameters must be S and message type or just message type
        Class<?>[] parms = m.getParameterTypes();
        if( parms.length != 2 && parms.length != 1 ) {
            log.trace("Parameter count is not 1 or 2");
            return false;
        }            
        int connectionIndex = parms.length > 1 ? 0 : -1;
        int messageIndex = parms.length > 1 ? 1 : 0;

        if( connectionIndex > 0 && !MessageConnection.class.isAssignableFrom(parms[connectionIndex]) ) {
            log.trace("First paramter is not a MessageConnection or subclass.");
            return false;
        }
 
        if( messageType == null && !Message.class.isAssignableFrom(parms[messageIndex]) ) {
            log.trace("Second paramter is not a Message or subclass.");
            return false;
        }
        if( messageType != null && !parms[messageIndex].isAssignableFrom(messageType) ) {
            log.trace("Second paramter is not a " + messageType );
            return false;
        }
        return true;            
    }
 
    protected Class getMessageType( Method m ) {
        Class<?>[] parms = m.getParameterTypes();
        return parms[parms.length-1];    
    }
    
    protected Method findDelegate( String name, Class messageType ) {
        // We do an exhaustive search because it's easier to 
        // check for a variety of parameter types and it's all
        // that Class would be doing in getMethod() anyway.
        for( Method m : delegateType.getDeclaredMethods() ) {
                    
            if( !m.getName().equals(name) ) {
                continue;
            }                
 
            if( isValidMethod(m, messageType) ) {
                return m;
            }
        }
                       
        return null;        
    }
    
    protected boolean allowName( String name ) {
        return true;
    }
    
    public final AbstractMessageDelegator<S> automap() {        
        map((Set<String>)null);
        if( methods.isEmpty() ) {
            throw new RuntimeException("No message handling methods found for class:" + delegateType);
        }
        return this;
    }
 
    public AbstractMessageDelegator<S> map( String... methodNames ) {
        Set<String> names = new HashSet<String>( Arrays.asList(methodNames) );
        map(names);
        return this;
    }
    
    protected void map( Set<String> constraints ) {
        
        if( log.isTraceEnabled() ) {
            log.trace("map(" + constraints);
        } 
        for( Method m : delegateType.getDeclaredMethods() ) {        
            if( log.isTraceEnabled() ) {
                log.trace("Checking method:" + m);
            }

            if( constraints == null && !allowName(m.getName()) ) {
                log.trace("Name is not allowed.");
                continue;
            }
            if( constraints != null && !constraints.contains(m.getName()) ) {
                log.trace("Name is not in constraints set.");
                continue;
            }

            if( isValidMethod(m, null) ) {
                if( log.isTraceEnabled() ) {            
                    log.trace("Adding method mapping:" + getMessageType(m) + " = " + m);
                }
                // Make sure we can access the method even if it's not public or
                // is in a non-public inner class.
                m.setAccessible(true);  
                methods.put(getMessageType(m), m);
            }            
        }
        
        messageTypes = null;        
    }
    
    public AbstractMessageDelegator<S> map( Class messageType, String methodName ) {
        // Lookup the method 
        Method m = findDelegate( methodName, messageType );
        if( m == null ) { 
            throw new RuntimeException( "Method:" + methodName 
                                        + " not found matching signature (MessageConnection, " 
                                        + messageType.getName() + ")" );
        }                                        
 
        if( log.isTraceEnabled() ) {            
            log.trace("Adding method mapping:" + messageType + " = " + m);
        }  
        methods.put( messageType, m );
        messageTypes = null;        
        return this;   
    }
    
    protected Method getMethod( Class c ) {
        Method m = methods.get(c);
        return m;
    }

    protected abstract Object getSourceDelegate( S source );

    @Override
    public void messageReceived( S source, Message msg ) {
        if( msg == null ) {
            return;
        }
 
        Object delegate = getSourceDelegate(source);
        if( delegate == null ) {
            // Means ignore this message/source
            return;
        } 
            
        Method m = getMethod(msg.getClass());
        if( m == null ) {
            throw new RuntimeException("Delegate method not found for message class:" 
                                        + msg.getClass());
        }
 
        try {
            if( m.getParameterTypes().length > 1 ) {           
                m.invoke( delegate, source, msg );
            } else {
                m.invoke( delegate, msg );
            }
        } catch( IllegalAccessException e ) {
            throw new RuntimeException("Error executing:" + m, e);
        } catch( InvocationTargetException e ) {
            throw new RuntimeException("Error executing:" + m, e.getCause());
        }
    }
}


