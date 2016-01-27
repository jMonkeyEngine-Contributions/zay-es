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

package com.simsilica.es.server;

import com.jme3.network.ConnectionListener;
import com.jme3.network.HostedConnection;
import com.jme3.network.Server;
import com.simsilica.es.ObservableEntityData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *  Provides server-side hosting capability for an EntityData
 *  instance.  Once attached to a server, this will process messages
 *  and make sure client requests are granted and client updates
 *  are sent.
 *
 *  <p>It is up to the game server to periodically call sendUpdates()
 *  to flush any pending EntitySet changes to clients.</p>
 *
 *  <p>For JME 3.1 and above, use EntityDataHostedService and EntityDataClientService
 *  instead of these more crude classes.  They will be deprecated when JME 3.1 is
 *  full released.</p> 
 *
 *  @author    Paul Speed
 */
public class EntityDataHostService implements EntityHostSettings {

    static Logger log = LoggerFactory.getLogger(EntityDataHostService.class);
    
    private final Server server;
    private final int channel;
    private final ObservableEntityData ed;
    private boolean autoHost = true;
    private int maxEntityBatchSize = 20;
    private int maxChangeBatchSize = 20;
    
    private final ConnectionObserver connectionObserver;
 
    /**
     *  Creates a new EntityDataHostService that will watch for new
     *  connections on the specified Server and provide access to the
     *  specified EntityData.  Entity request and response messages will
     *  be sent on the specified channel.
     */   
    public EntityDataHostService( Server server, int channel, ObservableEntityData ed ) {
        this.server = server;
        this.channel = channel;
        this.ed = ed;
        this.connectionObserver = new ConnectionObserver();
        server.addConnectionListener(connectionObserver);
        
        // A general listener for forwarding the ES messages
        // to the client-specific handler
        SessionDataDelegator delegator = new SessionDataDelegator(HostedEntityData.class, 
                                                                  HostedEntityData.ATTRIBUTE_NAME,
                                                                  true);
        server.addMessageListener(delegator, delegator.getMessageTypes());
    }
    
    @Override
    public int getChannel() {
        return channel;
    }

    /**
     *  Must be called by the game server to send pending updates
     *  to the relevant clients.
     */
    public void sendUpdates() {
        for( HostedConnection conn : server.getConnections() ) {
            HostedEntityData hed = conn.getAttribute(HostedEntityData.ATTRIBUTE_NAME);
            if( hed == null ) {
                continue;
            }
            hed.sendUpdates();
        }
    }
    
    /**
     *  Causes this service to stop listening for new connections and
     *  all existing connections will have stopHostingOnConnection() called
     *  for them.     
     */
    public void stop() {
        server.removeConnectionListener(connectionObserver);
        for( HostedConnection conn : server.getConnections() ) {
            stopHostingOnConnection(conn);
        } 
    } 
 
    /**
     *  Sets up the specified connection for hosting remote 
     *  entity data commands.  By default this is performed automatically
     *  in addConnection() and is controlled by the setAutoHost() property.
     */
    public void startHostingOnConnection( HostedConnection hc ) {
        log.debug("startHostingOnConnection:" + hc);
        hc.setAttribute(HostedEntityData.ATTRIBUTE_NAME, new HostedEntityData(this, hc, ed));
    }

    /**
     *  Terminates the specified connection for hosting remote 
     *  entity data commands.  By default this is performed automatically
     *  in removeConnection().
     */
    public void stopHostingOnConnection( HostedConnection hc ) {
        HostedEntityData hed = hc.getAttribute(HostedEntityData.ATTRIBUTE_NAME);
        if( hed == null ) {
            return;
        }
        log.debug("stopHostingOnConnection:" + hc);
        hc.setAttribute(HostedEntityData.ATTRIBUTE_NAME, null);
        hed.close();
    }
 
    /**
     *  Sets the maximum number of entities that will be sent back
     *  in a single batched results message.  The optimal number largely
     *  depends on the relative size of an average entity as the app
     *  retrieves it.  Small components and small numbers of components
     *  per entity means larger batches are optimal.
     *  Defaults to 20.
     */
    public void setMaxEntityBatchSize( int i ) {
        this.maxEntityBatchSize = i;
    }
 
    @Override
    public int getMaxEntityBatchSize() {
        return maxEntityBatchSize;
    }

    /**
     *  Sets the maximum number of EntityChanges that will be sent back
     *  in a single batched results message.  The optimal number largely
     *  depends on the average size of components.
     *  Defaults to 20.
     */
    public void setMaxChangeBatchSize( int i ) {
        this.maxChangeBatchSize = i;
    }
 
    @Override
    public int getMaxChangeBatchSize() {
        return maxChangeBatchSize;
    }
 
    /**
     *  Set to true to have new connections automatically 'hosted'
     *  by this entity service.  In other words, any newly added
     *  connections will automatically have startHostingOnConnection() called.
     *  Set this to false if the application requires further client
     *  setup before letting them access entities.  In that case, the game
     *  server will have to call startHostingOnConnection() manually.
     *  Defaults to true.
     */
    public void setAutoHost( boolean b ) {
        this.autoHost = b;
    }
 
    public boolean getAutoHost() {
        return autoHost;
    }
 
    protected void addConnection( HostedConnection hc ) {
        log.debug("Connection added:" + hc);
        if( autoHost ) {
            startHostingOnConnection(hc);
        }    
    }
    
    protected void removeConnection( HostedConnection hc ) {
        log.debug("Connection removed:" + hc);
        stopHostingOnConnection(hc);    
    } 
    
    protected class ConnectionObserver implements ConnectionListener {

        @Override
        public void connectionAdded(Server server, HostedConnection hc) {
            addConnection(hc);
        }

        @Override
        public void connectionRemoved(Server server, HostedConnection hc) {
            removeConnection(hc);
        }
    }
}

