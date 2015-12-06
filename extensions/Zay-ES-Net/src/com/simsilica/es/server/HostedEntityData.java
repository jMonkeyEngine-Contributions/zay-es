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

import com.jme3.network.HostedConnection;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityChange;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.WatchedEntity;
import com.simsilica.es.net.ComponentChangeMessage;
import com.simsilica.es.net.EntityDataMessage;
import com.simsilica.es.net.EntityDataMessage.ComponentData;
import com.simsilica.es.net.EntityIdsMessage;
import com.simsilica.es.net.FindEntitiesMessage;
import com.simsilica.es.net.FindEntityMessage;
import com.simsilica.es.net.GetComponentsMessage;
import com.simsilica.es.net.GetEntitySetMessage;
import com.simsilica.es.net.ReleaseEntitySetMessage;
import com.simsilica.es.net.ReleaseWatchedEntityMessage;
import com.simsilica.es.net.ResetEntitySetFilterMessage;
import com.simsilica.es.net.ResultComponentsMessage;
import com.simsilica.es.net.StringIdMessage;
import com.simsilica.es.net.WatchEntityMessage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *  Provides the per-connection access and book-keeping to
 *  the server's EntityData.  Instances of this class are created
 *  and managed by the EntityDataHostService and users should
 *  not be required to do anything specific with this class.
 *
 *  @author    Paul Speed
 */
public class HostedEntityData {

    public static final String ATTRIBUTE_NAME = "hostedEntityData";
 
    static Logger log = LoggerFactory.getLogger(HostedEntityData.class);  
 
    private final EntityDataHostService service;   
    private final HostedConnection conn;
    private final EntityData ed;
    
    private final AtomicBoolean closing = new AtomicBoolean(false);
    private final Map<Integer, EntitySet> activeSets = new ConcurrentHashMap<Integer, EntitySet>();
    private final Map<Integer, WatchedEntity> activeEntities = new ConcurrentHashMap<Integer, WatchedEntity>();
 
    /**
     *  Used to lock against sending updates in specific cases where
     *  the EntitySet updates would cause issues for other code.  So far
     *  there is only one use-case.
     *  This lock will block the whole sendUpdates() method and should
     *  only be issued for really short-lived operations like resetting
     *  a filter.
     */
    private final Lock updateLock = new ReentrantLock();
 
    /**
     *  Reused during update sending to capture the latest entity
     *  set updates. 
     */   
    private final Set<EntityChange> changeBuffer = new HashSet<EntityChange>();
    
    /**
     *  Reused during update sending to collect full entity changes before
     *  sending them on.
     */
    private final List<ComponentData> entityBuffer = new ArrayList<ComponentData>();

    /**
     *  Reused during update sending to collect batches of component changes
     *  before sending them on. 
     */
    private final List<EntityChange> changeList = new ArrayList<EntityChange>();
    
    public HostedEntityData( EntityDataHostService service, HostedConnection conn, EntityData ed ) {
        this.service = service;
        this.ed = ed;
        this.conn = conn;
        log.trace("Created HostedEntityData:" + this);    
    }
    
    public void close() {
        if( !closing.compareAndSet(false, true) ) {
            return;
        } 
   
        log.trace("Closing HostedEntityData:" + this);
        
        // Release all of the active sets
        for( EntitySet set : activeSets.values() ) {
            log.trace("Releasing: EntitySet@" + System.identityHashCode(set));        
            set.release();
        }
        activeSets.clear();
        
        // Release all of the active entities
        for( WatchedEntity e : activeEntities.values() ) {
            log.trace("Releasing: WatchedEntity@" + System.identityHashCode(e));        
            e.release();
        }
        activeEntities.clear();    
    }    
    
    public void getComponents( HostedConnection source, GetComponentsMessage msg ) {
        if( log.isTraceEnabled() ) {
            log.trace("getComponents:" + msg);
        }    
        Entity e = ed.getEntity(msg.getEntityId(), msg.getComponentTypes());
        if( log.isTraceEnabled() ) {
            log.trace("Sending back entity data:" + e);
        }        
        source.send(service.getChannel(), 
                    new ResultComponentsMessage(msg.getRequestId(), e));
    }
  
    public void findEntities( HostedConnection source, FindEntitiesMessage msg ) {
        if( log.isTraceEnabled() ) {
            log.trace("findEntities:" + msg);
        }    
        Set<EntityId> result = ed.findEntities(msg.getFilter(), msg.getComponentTypes());
        if( log.isTraceEnabled() ) {        
            log.trace("Sending back entity ID data:" + result);
        }        
        source.send(service.getChannel(),
                    new EntityIdsMessage(msg.getRequestId(), result));                    
    }

    public void findEntity( HostedConnection source, FindEntityMessage msg ) {        
        if( log.isTraceEnabled() ) {
            log.trace("findEntity:" + msg);
        }    
        EntityId result = ed.findEntity(msg.getFilter(), msg.getComponentTypes());
        if( log.isTraceEnabled() ) {
            log.trace("Sending back entity ID data:" + result);
        }        
        source.send(service.getChannel(),
                    new EntityIdsMessage(msg.getRequestId(), result));                    
    }
    
    public void watchEntity( HostedConnection source, WatchEntityMessage msg ) {
        
        if( log.isTraceEnabled() ) {
            log.trace("watchEntity:" + msg);
        } 
        int watchId = msg.getWatchId();
        WatchedEntity result = activeEntities.get(watchId);
        if( result != null ) {
            throw new RuntimeException("WatchedEntity already exists for watch ID:" + watchId);
        }        
        
        result = ed.watchEntity(msg.getEntityId(), msg.getComponentTypes());
        activeEntities.put(watchId, result);
        if( log.isTraceEnabled() ) {
            log.trace("Sending back entity data:" + result);
        }
        
        // We can reuse the result components message        
        source.send(service.getChannel(), 
                    new ResultComponentsMessage(msg.getRequestId(), result));
    }
    
    public void releaseEntity( HostedConnection source, ReleaseWatchedEntityMessage msg ) {
        if( log.isTraceEnabled() ) {
            log.trace("releaseEntity:" + msg);
        }
        int watchId = msg.getWatchId();
        WatchedEntity e = activeEntities.remove(watchId);
        e.release();
    }
   
    public void getEntitySet( HostedConnection source, GetEntitySetMessage msg ) {
        if( log.isTraceEnabled() ) {
            log.trace("getEntitySet:" + msg);
        }    
        // Just send the results back directly       
        // Need to send the entity set ID that the client
        // will recognize and the entity data which is
        // entity ID plus an array of components.
        // This may need to be broken into several messages to fit.
        // Since components are relatively small, it might be ok to just
        // pick an arbitrary maximum that should always fit.  Besides,
        // there is a nice balance when breaking them up with keeping
        // the message pipe moving.

        int setId = msg.getSetId();        
        EntitySet set = activeSets.get(setId);
        
        // We should be the first or there is an error.
        if( set != null ) {
            throw new RuntimeException("Set already exists for ID:" + setId);
        }
        
        if( log.isTraceEnabled() ) {
            log.trace("Creating set for ID:" + msg.getSetId());
        }
            
        set = ed.getEntities(msg.getFilter(), msg.getComponentTypes());
        
        int batchMax = service.getMaxEntityBatchSize();
        List<ComponentData> data = new ArrayList<ComponentData>();
        for( Entity e : set ) {
            data.add(new ComponentData(e));
            if( data.size() > batchMax ) {
                sendAndClear(setId, data);
            }
        }
        
        if( !data.isEmpty() ) {
            sendAndClear(setId, data);
        }
        
        // Put the EntitySet into the active sets after we have
        // iterated over its data.  This prevents one case where
        // an eager sendUpdates() could applyChanges() on top of
        // us while doing a full data flush to the client.  After
        // this, all updates will be sent to the client via the
        // sendUpdates() method and there won't be a thread conflict
        // (except with filter resets which will be dealt with 
        //  using a short-lived lock for that case.)
        activeSets.put(setId, set);
    }
 
    public void resetEntitySetFilter( HostedConnection source, ResetEntitySetFilterMessage msg ) {
        if( log.isTraceEnabled() )
            log.trace( "resetEntitySetFilter:" + msg );
        
        // Note: we could avoid the lock by queuing a command that applies
        //       the filter in sendUpdates() but we don't really avoid much
        //       threading overhead that way.
        updateLock.lock();
        try {
            EntitySet set = activeSets.get(msg.getSetId());
            set.resetFilter(msg.getFilter());
        } finally {
            updateLock.unlock();
        }        
    } 
 
    public void releaseEntitySet( HostedConnection source, ReleaseEntitySetMessage msg ) {
        if( log.isTraceEnabled() ) {
            log.trace("releaseEntitySet:" + msg);
        }
        
        // Releasing an entity set is (currently) a safe operation
        // to perform even if the set is in use at the time.  The client
        // already has to deal with the race condition of continuing to
        // get updates for a (from their perspective) released set anyway.        
        EntitySet set = activeSets.remove(msg.getSetId());
        set.release();            
    }
    
    public void getStringInfo( HostedConnection source, StringIdMessage msg ) {
        if( msg.getId() != null ) {
            source.send(new StringIdMessage(msg.getRequestId(), 
                                            ed.getStrings().getString(msg.getId())));   
        } else if( msg.getString() != null ) {
            source.send(new StringIdMessage(msg.getRequestId(), 
                                            ed.getStrings().getStringId(msg.getString(), false)));   
        } else {
            throw new RuntimeException("Bad StringIdMessage:" + msg);
        }
    }

    protected void sendAndClear( int setId, List<ComponentData> buffer ) {
        conn.send(service.getChannel(), new EntityDataMessage(setId, buffer));
        buffer.clear();
    }
 
    protected void sendAndClear( List<EntityChange> buffer ) {
        conn.send(service.getChannel(), new ComponentChangeMessage(buffer));
        buffer.clear(); 
    }
    
    /**
     *  Periodically called by the EntityDataHostService to send any relevant changes
     *  to the client.
     */
    public void sendUpdates() {
        if( closing.get() ) {
            return;
        }
        
        // Clear the last change buffer and last data buffer just in case
        changeBuffer.clear();
        entityBuffer.clear();
 
        // One lock per update is better than locking per entity set
        // even if it makes message handling methods wait a little longer.
        // They can afford to wait.
        updateLock.lock();
        try {
            // Go through all of the active sets 
            int entityMax = service.getMaxEntityBatchSize();
            for( Map.Entry<Integer,EntitySet> e : activeSets.entrySet() ) {
            
                EntitySet set = e.getValue();
                if( !set.applyChanges(changeBuffer) ) {
                    // No changes to this set since last time
                    continue;
                }
                            
                // In theory we could just send the raw component changes
                // and let the client sort out the adds, removes, etc. for
                // their entity sets.
                // However, in the case of adds, we potentially make the
                // client do a lot more network comms just to sort it out.
                // For example, if the client only sees one change that
                // causes the entity to get added then it will have to
                // retrieve all of the other components for that entity.
                // When we detect an add, it's in our best interest to go
                // ahead and send the whole thing.
                // Removes are a little different since the client will
                // instantly know to remove it just from the component
                // change.
                
                // So, send adds specifically
                for( Entity entity : set.getAddedEntities() ) {
                    entityBuffer.add( new ComponentData(entity) );                
                    if( entityBuffer.size() > entityMax ) {
                        sendAndClear(e.getKey(), entityBuffer);
                    }
                }
                
                if( !entityBuffer.isEmpty() ) {
                    sendAndClear(e.getKey(), entityBuffer);
                } 
            }
        } finally {
            updateLock.unlock();
        }
        
        // Collect changes for any active entities
        for( WatchedEntity e : activeEntities.values() ) {
            e.applyChanges(changeBuffer);
        }
        
        if( !changeBuffer.isEmpty() ) {
            // Send the component changes themselves...
            // Note: it's possible some of these are redundant with
            //       the adds above but there is no easy way to
            //       safely detect that.            
            int changeMax = service.getMaxChangeBatchSize(); 
            for( EntityChange c : changeBuffer ) {
                changeList.add(c);
                if( changeList.size() > changeMax ) {
                    sendAndClear(changeList);
                } 
            }
            
            if( !changeList.isEmpty() ) {             
                sendAndClear(changeList);
            }
        }
    }         
}


