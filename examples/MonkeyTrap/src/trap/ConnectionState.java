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
import com.jme3.network.ClientStateListener;
import com.jme3.network.ClientStateListener.DisconnectInfo;
import com.jme3.network.Network;
import com.simsilica.es.ComponentFilter;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.Filters;
import com.simsilica.lemur.event.BaseAppState;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trap.game.ModelType;
import trap.game.MonkeyTrapConstants;
import trap.game.Position;
import trap.net.TrapSerializers;
import com.simsilica.es.net.EntitySerializers;
import trap.net.RemoteGameClient;


/**
 *
 *  @author    Paul Speed
 */
public class ConnectionState extends BaseAppState {

    static Logger log = LoggerFactory.getLogger(ConnectionState.class);
 
    private Client client;
    private String user; 
    private RemoteGameClient gameClient;
    
    private EntityData remoteEd;
    private EntitySet test;
 
    private GamePlayState gameState;
    
    private ClientObserver clientObserver = new ClientObserver();
    
    static {
        EntitySerializers.initialize();
        TrapSerializers.initialize();
    }
    
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
        client.addClientStateListener(clientObserver);
 
        // Startup the connection
        log.info("Starting client connection");
        client.start();
    }
    
    protected void connected() {       
        log.info("Connected");
        
        gameClient = new RemoteGameClient(user, client, 0); 
        
        remoteEd = gameClient.getEntityData(); //new RemoteEntityData(client, 0);
        test = remoteEd.getEntities(Position.class, ModelType.class);
        
        System.out.println( "Trying to get entity 0..." );
        Entity e = remoteEd.getEntity(new EntityId(0), Position.class, ModelType.class);
        System.out.println( "Got:" + e );
        
        ComponentFilter ogres = Filters.fieldEquals(ModelType.class, "type", "Ogre");
        Set<EntityId> result1 = remoteEd.findEntities(ogres, Position.class, ModelType.class);
        System.out.println( "Ogres:" + result1 );
                
        EntityId result2 = remoteEd.findEntity(ogres, Position.class, ModelType.class);
        System.out.println( "First ogre:" + result2 );
        
        test2 = remoteEd.getEntities(ogres, Position.class, ModelType.class);        
    }

EntitySet test2;
int counter = 0;

    protected synchronized void disconnected( DisconnectInfo di ) {       
        log.info("Disconnected:" + di);
        if( di != null ) {
            if( getState(ErrorState.class) != null ) {
                return;
            }
            ErrorState error = new ErrorState(di.reason, 
                                              di.error != null ? di.error.getMessage() : "Unknown error", 
                                              "dungeon");
            getApplication().getStateManager().attach(error);
            getApplication().getStateManager().detach(this);        
        }
    }

    @Override
    protected void cleanup( Application app ) {
        if( test != null ) {
            test.release();
        }
    
        // Close the network connection
        log.info("Closing client connection");
        if( client.isConnected() ) {
            client.close();
        }

        if( getState(MainMenuState.class) != null ) {
            getState(MainMenuState.class).setEnabled(true);
        }
    }

    @Override
    public void update( float tpf ) {    
        if( test != null && test.applyChanges() ) {
            System.out.println( "Added:" + test.getAddedEntities() );
            System.out.println( "Removed:" + test.getRemovedEntities() );
            System.out.println( "Changed:" + test.getChangedEntities() );
        }
        
        if( test2 != null && test2.applyChanges() ) {
            System.out.println( "test2 Added:" + test2.getAddedEntities() );
            System.out.println( "test2 Removed:" + test2.getRemovedEntities() );
            System.out.println( "test2 Changed:" + test2.getChangedEntities() );
        }
        
        if( test2 != null ) {
            counter++;
            if( counter == 60 ) {
                System.out.println("Changing filter...");
                ComponentFilter chests = Filters.fieldEquals(ModelType.class, "type", "Chest");
                test2.resetFilter(chests);   
            }
        }
        
        if( gameClient != null && gameClient.getMaze() != null && gameClient.getPlayer() != null ) {
            // We're ready to go
            gameState = new GamePlayState( gameClient );
            getStateManager().attach(gameState);
            
            // Stop calling our update... not sure how we will get cleaned
            // up exactly. 
            setEnabled(false);
        }
    }

    @Override
    protected void enable() {
    }

    @Override
    protected void disable() {
    }
    
    private class ClientObserver implements ClientStateListener {

        public void clientConnected( Client client ) {
            connected();
        }

        public void clientDisconnected( Client client, DisconnectInfo di ) {
            disconnected(di);
        }                
    }
}
