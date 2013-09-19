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
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.lemur.event.BaseAppState;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *  Watches entities with Position and ModelType components
 *  and creates/destroys Spatials as needed as well as moving
 *  them to the appropriate locations.
 *  Spatials are created with a ModelFactory callback object
 *  that can be game specific.  
 *
 *  @author    Paul Speed
 */
public class ModelState extends BaseAppState {

    static Logger log = LoggerFactory.getLogger( ModelState.class );

    private EntityData ed;
    private EntitySet entities;
    private Map<EntityId, Spatial> models = new HashMap<EntityId, Spatial>();
    private Node modelRoot;
    private ModelFactory factory;

    public ModelState( ModelFactory factory ) {
        this.factory = factory;
    }

    public Node getModelRoot() {
        return modelRoot;
    }

    public Spatial getSpatial( EntityId entity ) {
        return models.get(entity);
    }

    protected Spatial createSpatial( Entity e ) {
        return factory.createModel(e);
    }

    protected void addModels( Set<Entity> set ) {

        for( Entity e : set ) {
            // See if we already have one
            Spatial s = models.get(e.getId());
            if( s != null ) {
                log.error("Model already exists for added entity:" + e);
                continue;
            }

            s = createSpatial(e);
            models.put(e.getId(), s);
            updateModelSpatial(e, s);
            modelRoot.attachChild(s);
        }
    }

    protected void removeModels( Set<Entity> set ) {

        for( Entity e : set ) {
            Spatial s = models.remove(e.getId());
            if( s == null ) {
                log.error("Model not found for removed entity:" + e);
                continue;
            }
            s.removeFromParent();
        }
    }

    protected void updateModelSpatial( Entity e, Spatial s ) {
        Position p = e.get(Position.class);
        InterpolationControl ic = s.getControl(InterpolationControl.class);
        if( ic != null ) {
            ic.setTarget(p.getLocation());
        } else {        
            s.setLocalTranslation(p.getLocation());
        }
        s.setLocalRotation(p.getFacing());
    }

    protected void updateModels( Set<Entity> set ) {

        for( Entity e : set ) {
            Spatial s = models.get(e.getId());
            if( s == null ) {
                log.error("Model not found for updated entity:" + e);
                continue;
            }

            updateModelSpatial(e, s);
        }
    }

    @Override
    protected void initialize( Application app ) {

        factory.setState(this);

        // Grab the set of entities we are interested in
        ed = getState(EntityDataState.class).getEntityData();
        entities = ed.getEntities(Position.class, ModelType.class);

        // Create a root for all of the models we create
        modelRoot = new Node("Model Root");
    }

    @Override
    protected void cleanup( Application app ) {

        // Release the entity set we grabbed previously
        entities.release();
        entities = null;
    }

    @Override
    protected void enable() {
        ((Main)getApplication()).getRootNode().attachChild(modelRoot);

        entities.applyChanges();
        addModels(entities);
    }

    @Override
    public void update( float tpf ) {
        if( entities.applyChanges() ) {
            removeModels(entities.getRemovedEntities());
            addModels(entities.getAddedEntities());
            updateModels(entities.getChangedEntities());
        }
    }

    @Override
    protected void disable() {
        modelRoot.removeFromParent();
        removeModels(entities);
    }

}
