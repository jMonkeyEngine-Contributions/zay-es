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

package trap.server;

import com.jme3.network.ConnectionListener;
import com.simsilica.es.server.EntityDataHostService;
import com.jme3.network.HostedConnection;
import com.jme3.network.Network;
import com.jme3.network.Server;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import trap.game.GameSystems;
import trap.game.MonkeyTrapConstants;
import trap.game.Service;
import trap.net.TrapSerializers;
import com.simsilica.es.net.EntitySerializers;
import com.simsilica.es.server.SessionDataDelegator;


/**
 *  The game server stand-alone class.  Can be run from the 
 *  command line or instantiated and started separately.
 *
 *  @author    Paul Speed
 */
public class GameServer
{
    private GameSystems systems;
    private int port;
    private Server host;    
    private EntityDataHostService edHost;
    
    private ConnectionObserver connectionObserver = new ConnectionObserver();

    static {
        EntitySerializers.initialize();
        TrapSerializers.initialize();    
    }

    public GameServer( GameSystems systems, int port ) {
        this.systems = systems;
        this.port = port;
    }

    public void start() throws IOException {
        // Create the network hosting connection
        host = Network.createServer(MonkeyTrapConstants.GAME_NAME, 
                                    MonkeyTrapConstants.PROTOCOL_VERSION, 
                                    port, port);
                                    
        System.out.println("Adding channel 0 on port:" + (port+1));                                    
        host.addChannel(port+1);
 
        // Add our own stub service to send updates at the end of
        // the game update cycle
        systems.addService(new Service() {
                public void update( long gameTime ) {
                    edHost.sendUpdates();
                }

                public void initialize( GameSystems systems ) {
                    // Setup the network listeners
                    edHost = new EntityDataHostService(host, 0, systems.getEntityData());                 
                }

                public void terminate( GameSystems systems ) {
                    // Remove the listeners for the es hosting
                    edHost.stop();        
                }
            });
        
        // Start the game systems
        systems.start();
 
        // Will delegate certain messages to the GameMessageHandler for
        // a particular connection.
        SessionDataDelegator delegator = new SessionDataDelegator(GameMessageHandler.class, 
                                                                  GameMessageHandler.ATTRIBUTE,
                                                                  true);
        host.addMessageListener(delegator, delegator.getMessageTypes());
 
        // Add our own connection listener that will add GameMessageHandlers
        // to connections.
        host.addConnectionListener(connectionObserver);
        
        // Start accepting connections
        host.start();
    }
    
    public void stop() {
        // Stop accepting network connections and kick any
        // existing clients.
        host.close();
 
        // Shut down the game systems
        systems.stop();
    }

    protected void addConnection( HostedConnection conn ) {
        System.out.println( "New connection from:" + conn );
        GameMessageHandler handler = new GameMessageHandler(systems, conn);
        conn.setAttribute(GameMessageHandler.ATTRIBUTE, handler);
    }
    
    protected void removeConnection( HostedConnection conn ) {
        System.out.println( "Connection closed:" + conn );        
        GameMessageHandler handler = conn.getAttribute(GameMessageHandler.ATTRIBUTE);
        if( handler != null ) {
            handler.close();
        }
    }

    public static void main( String... args ) throws Exception {
        
        int port = MonkeyTrapConstants.DEFAULT_PORT;
        for( int i = 0; i < args.length; i++ ) {
            if( "-p".equals(args[i]) && i + 1 < args.length ) {
                i++;
                port = Integer.parseInt(args[i]);
            } else {
                throw new IllegalArgumentException("Unknown option:" + args[i]);
            }
        }
        
        GameServer server = new GameServer(new GameSystems(), port);
        System.out.println("Starting server on port:" + port);
        server.start();
        
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while( (line = console.readLine()) != null ) {
            if( line.trim().length() == 0 ) {
                continue;
            }            
            if( "exit".equals(line) ) {
                break;
            } if( "connections".equals(line) ) {
                for( HostedConnection c : server.host.getConnections() ) {
                    System.out.println( c );
                }
            } else {
                System.out.println("Unknown command:" + line);
            }
        }
        
        System.out.println("Stopping server.");
        server.stop();    
    }
    
    private class ConnectionObserver implements ConnectionListener {

        public void connectionAdded(Server server, HostedConnection hc) {
            addConnection(hc);
        }

        public void connectionRemoved(Server server, HostedConnection hc) {
            removeConnection(hc);
        }
    }
}
