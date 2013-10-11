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

package trap;

import com.jme3.app.Application;
import com.jme3.network.Client;
import com.jme3.network.Network;
import com.simsilica.lemur.event.BaseAppState;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trap.game.MonkeyTrapConstants;


/**
 *
 *  @author    Paul Speed
 */
public class ConnectionState extends BaseAppState {

    static Logger log = LoggerFactory.getLogger(ConnectionState.class);
 
    private Client client;
    private String user; 
    
    public ConnectionState( String host, int port, String user ) throws IOException {
        // Create the connection right now to validate the 
        // info.  We'll actually start it later.
        this.client = Network.connectToServer(MonkeyTrapConstants.GAME_NAME,
                                              MonkeyTrapConstants.PROTOCOL_VERSION,
                                              host, port, port);
        this.user = user;                                         
    }

    @Override
    protected void initialize( Application app ) {
        // Setup the client listeners
 
        // Startup the connection
        log.info("Starting client connection");
        client.start();    
    }

    @Override
    protected void cleanup( Application app ) {
        // Close the network connection
        log.info("Closing client connection");
        client.close();
    }

    @Override
    protected void enable() {
    }

    @Override
    protected void disable() {
    }
}
