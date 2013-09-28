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

package trap.game;

import com.simsilica.es.EntityData;
import java.util.*;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trap.game.ai.AiService;


/**
 *  Manages the various systems that perform core game logic.
 *  These are the same in single or multiplayer.
 *
 *  @author    Paul Speed
 */
public class GameSystems {

    private Logger log = LoggerFactory.getLogger(GameSystems.class);

    private boolean started;
    private ScheduledExecutorService executor;
    private ServiceRunnable serviceRunner;
    private TimeProvider gameTime;
    private List<Service> services = new ArrayList<Service>();
    
    public GameSystems() {        
        // Setup the Monkey Trap services
        services.add(new EntityDataService());
        services.add(new DecayService());
        services.add(new MazeService(48, 48));
        services.add(new SpawnService(15));
        services.add(new AiService());        
        services.add(new MovementService());
        services.add(new CombatInitiativeService());
        services.add(new HealthService());
        services.add(new LootService());

        gameTime = new GameTimeProvider();     
        serviceRunner = new ServiceRunnable();
    }
 
    public EntityData getEntityData() {
        return getService(EntityDataService.class).getEntityData(); 
    }
    
    public <T extends Service> T addService( T s ) {
        if( started ) {
            throw new IllegalStateException( "Game systesm are already started." );
        }
        services.add(s);
        return s;
    }
 
    public <T extends Service> T getService( Class<T> type ) {
        for( Service s : services ) {
            if( type.isInstance(s) ) {
                return (T)s;
            }
        }
        return null;
    }
    
    public void start() {
        if( started ) {
            return;
        }
        for( Service s : services ) {
            s.initialize(this);
        }
	    executor = Executors.newScheduledThreadPool(1);         
        executor.scheduleAtFixedRate(serviceRunner, 0, 100, TimeUnit.MILLISECONDS);
        started = true;
    }
    
    public void stop() {
        if( !started ) {
            return;
        }
        executor.shutdown();
        
        // Terminate them backwards
        for( int i = services.size() - 1; i >= 0; i-- ) {
            Service s = services.get(i);
            s.terminate(this);
        }
        started = false;
    }
 
    public long getGameTime() {
        return gameTime.getTime(); 
    }
 
    public TimeProvider getGameTimeProvider() {
        return gameTime;
    }
 
    protected void runServices( long time ) {
        for( Service s : services ) {
            s.update(time);
        }
    }
 
    private class GameTimeProvider implements TimeProvider {    
        private long start = System.nanoTime();
    
        public long getTime() {
            return System.nanoTime() - start;           
        }  
    }
    
    private class ServiceRunnable implements Runnable {
        public void run() {
            try {
                runServices(getGameTime());
            } catch( RuntimeException e ) {
                log.error( "Error executing services", e );
            }
        }
    }    
}
