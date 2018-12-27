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

package com.simsilica.es.server;

import com.jme3.network.HostedConnection;
import com.jme3.network.Server;
import com.jme3.network.service.AbstractHostedService;
import com.jme3.network.service.HostedServiceManager;
import com.simsilica.es.EntityData;
import com.simsilica.es.ObservableEntityData;
import com.simsilica.es.net.EntitySerializers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *  A HostedService that manages access to an EntityData instance
 *  for client connections.  When added to the service manager, this
 *  will handle the networking necessary to facilitate client-server
 *  EntityData access.  A reciprocal EntityDataClientService should be
 *  added to the Client's ClientServiceManager to complete the setup.
 *
 *  <p>It is up to the game server to periodically call sendUpdates()
 *  to flush any pending EntitySet changes to clients.</p> 
 *
 *  @author    Paul Speed
 */
public class EntityDataHostedService extends AbstractHostedService 
                                     implements EntityHostSettings {

    static Logger log = LoggerFactory.getLogger(EntityDataHostedService.class);

    private int channel;
    private final ObservableEntityData ed;
    private boolean autoHost = true;
    private int maxEntityBatchSize = 20;
    private int maxChangeBatchSize = 20;
    
    private SessionDataDelegator delegator;

    /**
     *  Creates a new EntityDataHostedService for the specified EntityData that will
     *  communicate over the specified channel.  Autohosting is set to true by default.
     */
    public EntityDataHostedService( int channel, ObservableEntityData ed ) {
        this(channel, ed, true);
    }
    
    /**
     *  Creates a new EntityDataHostedService for the specified EntityData that will
     *  communicate over the specified channel and will automatically host for new connections
     *  depending on the specified autoHost value.
     */
    public EntityDataHostedService( int channel, ObservableEntityData ed, boolean autoHost ) {
        this.channel = channel;
        this.ed = ed;
        this.autoHost = autoHost;
        
        // Make sure the relevant serializers are registered
        EntitySerializers.initialize();
    }

    public EntityData getEntityData() {
        return ed;
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
        for( HostedConnection conn : getServer().getConnections() ) {
            HostedEntityData hed = conn.getAttribute(HostedEntityData.ATTRIBUTE_NAME);
            if( hed == null ) {
                continue;
            }
            hed.sendUpdates();
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
     *  Returns the HostedEntityData for the specified connection.  This
     *  can be used to setup connection-specific entity data setup.
     */
    public HostedEntityData getHostedEntityData( HostedConnection hc ) {
        return hc.getAttribute(HostedEntityData.ATTRIBUTE_NAME); 
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

    @Override
    protected void onInitialize( HostedServiceManager services ) {
        
        // A general listener for forwarding the ES messages
        // to the client-specific handler
        this.delegator = new SessionDataDelegator(HostedEntityData.class, 
                                                  HostedEntityData.ATTRIBUTE_NAME,
                                                  true);
        getServer().addMessageListener(delegator, delegator.getMessageTypes());
    } 

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        for( HostedConnection conn : getServer().getConnections() ) {
            stopHostingOnConnection(conn);
        } 
    }

    @Override
    public void terminate( HostedServiceManager hsm ) {
        getServer().removeMessageListener(delegator, delegator.getMessageTypes());
    }

    @Override
    public void connectionAdded( Server server, HostedConnection hc ) {
        log.debug("Connection added:" + hc);
        if( autoHost ) {
            startHostingOnConnection(hc);
        }    
    }

    @Override
    public void connectionRemoved( Server server, HostedConnection hc ) {
        log.debug("Connection removed:" + hc);
        stopHostingOnConnection(hc);    
    }

}
