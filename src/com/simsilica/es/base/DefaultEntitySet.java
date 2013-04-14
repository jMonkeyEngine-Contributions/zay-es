/*
 * $Id$
 *
 * Copyright (c) 2011-2013 jMonkeyEngine
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

package com.simsilica.es.base;

import com.simsilica.es.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.log4j.Logger;



/**
 *  A set of entities that possess certain components with
 *  automatic updates as the entity components change.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class DefaultEntitySet extends AbstractSet<Entity> 
                              implements EntitySet
{
    static Logger log = Logger.getLogger(EntitySet.class);

    // Concurrent hash map because the change set accumulation
    // checks the map for entity ID existence.
    private Map<EntityId,Entity> entities = new ConcurrentHashMap<EntityId,Entity>();
 
    private ConcurrentLinkedQueue<EntityChange> changes 
                    = new ConcurrentLinkedQueue<EntityChange>();
    
    private DefaultEntityData ed;
    private ComponentFilter mainFilter; // for now anyway
    private ComponentFilter[] filters;    
    private Class[] types;
    private boolean filtersChanged = false;      
 
    protected Transaction transaction = new Transaction();
    private Set<Entity> addedEntities = new HashSet<Entity>();
    private Set<Entity> changedEntities = new HashSet<Entity>();
    private Set<Entity> removedEntities = new HashSet<Entity>();

    private boolean released = false;    

    public boolean debugOn = false;

    // We could treat types as filters here too and just convert
    // them to raw classes on input.
    
    public DefaultEntitySet( DefaultEntityData ed, ComponentFilter filter, Class[] types )
    {
        this.ed = ed;
        this.types = types;
        setMainFilter(filter);
    }

    protected Class[] getTypes()
    {
        return types;
    }

    public String debugId()
    {
        return "EntitySet@" + System.identityHashCode(this);
    }
    
    protected void setMainFilter( ComponentFilter filter )
    {
        this.mainFilter = filter;
 
        if( filter != null )
            {       
            filters = new ComponentFilter[ types.length ];
            for( int i = 0; i < types.length; i++ )
                {
                if( filter.getComponentType() == types[i] )
                    filters[i] = filter;
                }
            }
        else
            {
            filters = null;
            }                
    }
    
    protected ComponentFilter getMainFilter()
    {
        return mainFilter;
    }
    
    /**
     *  Called to have the entity set load its initial set of 
     *  data.  This is called during creation (but not construction)
     *  and when the filter is reset.  
     */
    protected void loadEntities( boolean reload )
    {
        Set<EntityId> idSet = ed.findEntities( mainFilter, types );
        if( idSet.isEmpty() )
            return;
 
        // Note: we do a full component loop here just to
        // reuse the EntityComponent[] buffer.  We could have
        // just as easily called ed.getEntity() for each ID.
        
        // Now we have the info needed to build the entity set
        EntityComponent[] buffer = new EntityComponent[types.length]; 
        for( EntityId id : idSet )
            {
            // If we already have the entity then it is not a new
            // add and we'll ignore it.  This means that some entities
            // may have newer info than others but we will get their
            // event soon enough.
            // We include this for the reload after a filter change. 
            if( reload && containsId(id) )
                continue;
                
            for( int i = 0; i < buffer.length; i++ )
                buffer[i] = ed.getComponent( id, types[i] );
                
            // Now create the entity
            DefaultEntity e = new DefaultEntity( ed, id, buffer.clone(), types );
            if( add(e) && reload )
                addedEntities.add(e);
            }
    
        // I had a big long comment in AbstractEntityData worrying
        // about threading and stuff.  But the EntityChange events
        // are getting queued and as long as they aren't rejected
        // out of hand (which would be a bug) then we don't care if
        // they come in while we build the entity set.
        
    }     

    /**
     *  Removes entities from the set that no longer match the
     *  set's criteria.  This will update the removedEntities
     *  tracking.
     */
    protected void purgeEntities()
    {
        for( Iterator<Entity> it = iterator(); it.hasNext(); )
            {
            Entity e = it.next();
            if( !entityMatches(e) )
                {
                it.remove();
                removedEntities.add(e);
                }                 
            }
    }
 
    /**
     *  Swaps out the current main filter for a new one.  Returns
     *  true if the entity set was changed during this process.
     *  This is similar to applyChanges() and any current pending
     *  changes will be applied during the filter transition.
     */
    @Override
    public void resetFilter( ComponentFilter filter )
    {
        // General logic:
        // -remove the entities that don't match the filter
        // -find the entities that do and add them if they
        //  weren't already added.
        // -make sure that this is reflected in the change
        //  sets.
 
 
        // The problem with treating this like applyChanges() is
        // that it makes for two different code paths, for one.
        // For another, it makes the HostedEntityData's job harder
        // trying to sort out which events to send to the user.
        //
        // We can't delay until applyChanges() is called because
        // then we risk doing work collecting changes for entities
        // that don't apply anymore.  Though I guess in the remote
        // case it won't be sending them.
        //
        // What is the downside of setting a flag and waiting for
        // the next applyChanges()?  In single player, we will accept
        // changes for entities that we haven't purged.  Which isn't
        // too big of a deal.
        // In multiplayer this won't happen except anything that was
        // already sent anyway.  The server will have accurate filtering
        // after its next update... and will send us our removes, too
        // actually.  Perhaps that is not something that we want.
        //
        // I don't see a way of avoiding it, though.  Even if we purge
        // them here we would have to do special case code not to send
        // them on the server.  I suppose we could reset the filter
        // and apply all at once even on the server and just send
        // the changes right then... but then we potentially have a
        // threading problem that we didn't have before.  Right now
        // only one thread is applying changes and sending those
        // results out.  So we'd still delay and we'd have to special
        // case things in the send changes loop.
        //
        // We can always add a flag later, I guess... or just expose
        // the one we've added here and let the loop see if the filter
        // has changed before applyChanges() is called.
        //
        // I've kept the above for reference but the current implementation
        // sets a flag and lets applyChanges() fix things.
               
        // Switch out the main filter
        setMainFilter( filter );
 
        filtersChanged = true;
    }
 
    @Override
    public boolean containsId( EntityId id )
    {
        return entities.containsKey(id);
    }
 
    @Override
    public Entity getEntity( EntityId id )
    {
        return entities.get(id);
    }
 
    @Override
    public boolean equals( Object o )
    {
        return o == this;
    }
 
    @Override
    public int size()
    {
        return entities.size();
    }
    
    @Override
    public Iterator<Entity> iterator()
    {
        return new EntityIterator();
    }
 
    @Override
    public void clear()
    {
        entities.clear();
    }
    
    @Override
    public boolean add( Entity e )
    {
        // Note: this may come back to bite me later but we
        //       return that we don't add it but we actually do replace
        //       the old value.
        return entities.put( e.getId(), e ) == null;
    }

    protected Entity remove( EntityId id )
    {
        return entities.remove(id);
    }
 
    @Override
    public boolean remove( Object e )
    {
        if( !(e instanceof Entity) )
            return false;
        return entities.remove( ((Entity)e).getId() ) != null;
    }

    @Override
    public boolean contains( Object e )
    {
        if( !(e instanceof Entity) )
            return false;
        return entities.containsKey( ((Entity)e).getId() );
    }

    /**
     *  Returns the entities that were added during applyChanges.
     */
    @Override
    public Set<Entity> getAddedEntities()
    {
        return addedEntities;
    }     

    /**
     *  Returns the entities that were changed during applyChanges.
     */
    @Override
    public Set<Entity> getChangedEntities()
    {
        return changedEntities;
    }     

    /**
     *  Returns the entities that were removed during applyChanges.
     */
    @Override
    public Set<Entity> getRemovedEntities()
    {
        return removedEntities;
    }     

    @Override
    public void clearChangeSets()
    {
        addedEntities.clear();
        changedEntities.clear();
        removedEntities.clear();
    }

    @Override
    public boolean hasChanges()
    {
        return !addedEntities.isEmpty() || !changedEntities.isEmpty() || !removedEntities.isEmpty();                        
    }

    /**
     *  Applies any accumulated changes to this list's entities since
     *  the last time it was called and returns true if there were
     *  changes.
     */
    @Override
    public boolean applyChanges()
    {
        return applyChanges(null);
    }
 
    /**
     *  Applies any accumulated changes to this list's entities since
     *  the last time it was called and returns true if there were
     *  changes.  Changes that caused an update (not an add or a remove)
     *  will be added to the supplied updates set.
     */
    @Override
    public boolean applyChanges( Set<EntityChange> updates )
    {
        return applyChanges( updates, true );
    }
    
    protected boolean buildTransactionChanges( Set<EntityChange> updates )
    {
        if( changes.isEmpty() )
            return false;

        EntityChange change;
        while( (change = changes.poll()) != null )
            {
            transaction.addChange( change, updates );
            }
        return true;
    }

    protected void filterUpdates( Set<EntityChange> updates )
    {
        if( updates == null )
            return;
            
        // Fix the updates set by removing changes for removed
        // entities
        /*for( Iterator<EntityChange> it = updates.iterator(); it.hasNext(); )
            {
            EntityChange e = it.next();
            if( !entities.containsKey(e.getEntityId()) )
                it.remove(); 
            }*/
        // The above is wrong-headed because the changes we remove
        // might have been good changes for another entity set.
        
        // Note: there is another way to do that if we care.  Each
        // set would only _add_ the things that were changes to existing
        // entities that didn't cause a remove.  They would not post-filter
        // the global set!!
    }
 
    public boolean hasFilterChanged()
    {
        return filtersChanged;
    }
    
    protected boolean applyChanges( Set<EntityChange> updates, boolean clearChangeSets )
    {
        // Need to return something to the caller about the adds and removes
        // actually we could just add that information to internal buffers
        // that get reset each time this is called. That means for any given
        // frame you can see what changed since the last one.  We could
        // provide a method for clearing them if the app was memory starved
        // or add an argument to this method.
        //
        // this is useful for cleaning up things like the scene graph where
        // we keep non-component objects out there.  I'm not sure they could
        // be made components either and we'd probably still care about whether
        // they were added or not or what specific components had changed...
        // even if only for performance reasons.  We'll see.
        if( clearChangeSets )
            clearChangeSets();
 
        if( released )
            {
            // Then the changes are irrelevant
            changes.clear();
 
            // And everything is a remove           
            removedEntities.addAll(this);
            clear();
            return hasChanges();
            }
 
        if( buildTransactionChanges(updates) )
            {            
            // Resolve all of the changes into the change sets
            transaction.resolveChanges();
            }
            
        if( filtersChanged )
            {
            filtersChanged = false;
            
            // Remove any entities that no longer match
            purgeEntities();
 
            // Find the latest entities
            loadEntities( true );
            }
 
        filterUpdates( updates );
                                       
        return !addedEntities.isEmpty() || !changedEntities.isEmpty() || !removedEntities.isEmpty();                        
    }     
 
    /**
     *  Releases this entity set from processing further entity
     *  updates and destroys any internal data structures.
     */
    @Override
    public void release()
    {
        ed.releaseEntitySet(this);
        
        // Except we can't clear because release() might have been
        // called from a different thread than the one processing
        // the data.
        // clear();
        released = true;
    }

    protected boolean entityMatches( Entity e )
    {
        EntityComponent[] array = e.getComponents();
        for( int i = 0; i < types.length; i++ )
            {
            if( array[i] == null )
                return false;
                
            if( filters == null || filters[i] == null )
                continue;
                
            if( !filters[i].evaluate(array[i]) )
                return false;
            }
        return true; 
    }
 
    /**
     *  Returns true if the specific component matches the criteria
     *  for this entity set.
     */
    protected boolean isMatchingComponent( EntityComponent c )
    {
        for( int i = 0; i < types.length; i++ )
            {
            if( c.getClass() != types[i] )
                continue;                
            if( filters != null && filters[i] != null )
                {
                return filters[i].evaluate(c);
                }
            return true;                                
            }
            
        return false;
    }        
 
    @Override
    public final boolean hasType( Class type )
    {
        for( Class c : types )
            {
            if( c == type )
                return true;
            }
        return false;
    }
    
    private int typeIndex( Class type )
    {
        for( int i = 0; i < types.length; i++ )
            {
            if( types[i] == type )
                return i;
            }
        return -1;
    }
 
    protected boolean isRelevantChange( EntityChange change )
    {
        // Here we care about any changes that change the status
        // of an entity's inclusion in this set.
        // So:
        // -changes for components we don't watch can be ignored.
        // -changes for entities we have and types we have are
        //  always important.
        // -changes for entities we don't have but that match
        //  our filters we do care about.  
    
        // Is this even a type we watch?     
        if( !hasType(change.getComponentType()) )
            {
            if( log.isTraceEnabled() )
                log.trace( "   not our type." );
            return false;  // doesn't matter to us
            }

        // Is it about an entity that we already have?
        Entity e = entities.get(change.getEntityId());
        if( e != null )
            {
            // The component is a type we're interested in about an
            // entity we have... the pure definition of a relevant
            // change even if (especially if) that change will remove
            // it from this set
            // No further checking is needed.
            if( log.isTraceEnabled() )
                log.trace( "   We already have it, so we care." );            
            return true;
            }

        // So now we are to the case of an entity that we don't
        // have but a component type we are interested in.  The
        // component change might add this entity to the set.
        //
        // There are a few cases here:
        // 1) the entity has all of the components to be in this
        //    set and this is the change that will make it.
        // 2) the entity does not have all of the components to be
        //    in this set but this is one of the changes that would
        //    bring it closer.
        // 3) this change would definitely not be relevant to set
        //    inclusion.
        //
        // It may take several component changes before the entity
        // is ready to be included in this set.  We only truly
        // need to know about the last one but the apply loop is
        // already doing this check and it has the opportunity to
        // do it more efficiently by caching previous look-ups.
        //
        // so the best we can do is eliminate the ones that would
        // definitely not apply
        // ...and if we have no filters, we should assume that they
        // all might
        // (I think the old code wasn't so great about this but it
        //  only had one filter to worry about and my use-cases have
        //  so far been limited.  I believe it would have failed
        //  if there was an inLeaf and the position wasn't the last
        //  or next to last to change.  No.  It's just weird... it
        //  looks up the position every time so it includes each
        //  change for an entity matching the filter so far.
        //  That's not a bad way to go but could be expensive for
        //  many filters.)
        if( filters == null )
            {
            if( log.isTraceEnabled() )
                log.trace( "   No special filters, so we care." );            
            return true;  /// nothing more to check
            }
            
 
        /*        
            The problem with the below checks is that they don't
            take into account that we may have already seen components
            for the entity that would cause it to be added... and just
            haven't called applyChanges() yet.
            
            The following checks would then cause us to miss changes that
            would remove the entity from the set again.  We'd have to
            keep a lot more change-related book keeping (and keep it
            in sync with the change list) to avoid it.
             
        // If the component is being removed... we really don't
        // care about that.
        EntityComponent newValue = change.getComponent();
        if( newValue == null )
            {
            if( log.isTraceEnabled() )
                log.trace( "   It's a removal of a component for an entity we don't care about yet." );            
            return false;
            }

        // If it's not a matching component then it can't possibly
        // be relevant. 
        if( !isMatchingComponent(newValue) )
            {
            if( log.isTraceEnabled() )
                log.trace( "   It's a non-matching component for an entity we don't care about yet." );            
            return false;
            }
        */

        // Hmmm... though this does mean that we see lots of events
        // for entities that we don't care about and probably won't.
        if( log.isTraceEnabled() )
            log.trace( "It might be relevant." );
                        
        // We might care
        return true;
    } 
    
    protected void entityChange( EntityChange change )
    {
        if( log.isTraceEnabled() )
            log.trace( "entityChange(" + change + ")" );    
        if( !isRelevantChange( change ) )
            return;
            
        if( log.isTraceEnabled() )
            log.trace( "Adding change:" + change );
             
        // Accumulate the change for the next update pass
        changes.add(change); 
    }
    
    private class EntityIterator implements Iterator<Entity>
    {
        private Iterator<Map.Entry<EntityId,Entity>> delegate = entities.entrySet().iterator();
        
        public EntityIterator()
        {
        }
        
        @Override
        public boolean hasNext()
        {
            return delegate.hasNext();
        }
        
        @Override
        public Entity next()
        {
            return delegate.next().getValue();
        }
        
        @Override
        public void remove()
        {
            delegate.remove();
        }
    }
    
    /**
     *  Keeps accumulated data about a set of changes.  This buffers
     *  the changes and finalizes them all at once which should be
     *  a more efficient and simpler design than the deeply nested
     *  loop I used to have in applyChanges().
     * 
     *  Logic design is as follows:
        // Keep a map of the entities that we got components for that
        // we applied... so a DefaultEntity just kept there.
        //
        // Keep a map of the entities that we have changed for other
        // reasons (or just the set).
        //
        // Go through the adds and complete any that are incomplete.
        // Remove any that don't match the filter.
        //
        // Go through the otherwise changed entities and check them
        // against the filter.  If no longer matching then they
        // are removes.  Otherwise, they are really changes.
        //
        // In the adds, there is a slight race condition of sorts
        // since when we go to fully retrieve the entity then we
        // may see state that we haven't applied changes for yet.
        // I think that's ok.  We still give an accurate state upon
        // return from this method and the next changes will just
        // be redundant.
        //
        // The other benefit of the above is that in the cases
        // where all of the entity components have changed (like when
        // forwarding a new entity over the network) then we don't
        // redundantly pull its whole info and we no longer stress
        // the cache we put in to worry about that case.     
     */
    protected class Transaction
    {
        Map<EntityId,DefaultEntity> adds = new HashMap<EntityId,DefaultEntity>();
        Set<EntityId> mods = new HashSet<EntityId>();
        
        public void directRemove( EntityId id )
        {
            // These worry me.  I'm not sure when it is appropriate
            // to process them.  For now we will directly remove them
            // because it's the only way I can see to make the state
            // right.  But really that seems off to me
            Entity e = remove( id );
            if( e != null )
                removedEntities.add(e);                                     
        }
        
        public void directAdd( Entity e )
        {
            if( add( e ) )
                addedEntities.add(e);
        }
        
        public void addChange( EntityChange change, Set<EntityChange> updates )
        {
            EntityId id = change.getEntityId();
            EntityComponent comp = change.getComponent();
            DefaultEntity e = (DefaultEntity)entities.get( id );
 
            // If we don't have the entity then it's an add 
            // and we need to create one.
            if( e == null )
                {
                // See if we already added this one
                e = adds.get(id);
                
                if( e == null )
                    {
                    if( comp == null )
                        {
                        // We've never seen this entity before and we get
                        // an event about removing a component.  We can
                        // safely ignore it
                        return;
                        }
                        
                    // We add the components even if they don't match because
                    // otherwise we might have to retrieve them again.  We
                    // could get lots of changes for this entity and we will
                    // validate what we have in the completion loop.
                    
                    // Else we need to add it.
                
                    // Create an empty entity with the right number
                    // of components.                
                    e = new DefaultEntity( ed, id, new EntityComponent[types.length], types );
                    adds.put( id, e );
                    }               
                }
            else
                {                    
                // Then it's an entity we have already and we are about
                // to change it.
                mods.add( id );
                
                // We track the updates that caused a change... we'll
                // filter out the ones that were for removed entities
                // later.  This is actually somewhat better than we did
                // before since we only send changes for entities that
                // are still relevant.
                }
 
            // Apply the change
            int index;
            if( comp == null )
                index = typeIndex(change.getComponentType());
            else
                index = typeIndex(comp.getClass());

            // We keep track of the changes that might have been
            // relevant.  Technically this is too broadly scoped but
            // determining accurate affecting changes requires book-keeping
            // per entity.  
            // If we do no filtering at all that means that all Position
            // changes get delivered to all clients even if they don't
            // have the entity... no.  Because we would have pre-filtered
            // that case.  Still, we can double-check here by hitting it
            // against the filter before adding it to the updates set. 
            if( updates != null )
                {
                if( comp == null || filters == null || filters[index] == null || filters[index].evaluate(comp) )
                    updates.add(change);
                }
            
            e.getComponents()[index] = comp;                     
        }
 
        protected boolean completeEntity( DefaultEntity e )
        {
            // Try to make it complete if is isn't already.
            // We need to recheck the components against the
            // filters because we do no prefiltering on changes.
            // Technically, we should get changes from components
            // that don't match but it is better to be safe.
            // For example, there is a "bug" at the moment where
            // direct removes are happening before the transaction
            // processing which means we see changes for an entity
            // that has been forcefully removed and we think it is
            // an add.  But because when we had the entity we accept
            // all changes even if they didn't match (in case it
            // would remove the entity) then we would add it again
            // even though the components did not match.
            EntityComponent[] array = e.getComponents();
            for( int i = 0; i < types.length; i++ )
                {
                boolean rechecking = false;
                if( array[i] == null )
                    {
                    // Fill it in 
                    if( log.isDebugEnabled() )
                        log.debug( "Pulling component type:" + types[i] + " for id:" + e.getId() );                   
                    array[i] = ed.getComponent( e.getId(), types[i] );
                
                    // If we get nothing back then this entity can't be completed
                    if( array[i] == null )
                        return false;
                    }
                else
                    {
                    rechecking = true;
                    }
                
                // Now that we have a value, check the filters
                
                // If we have a filter and it doesn't match the filter then
                // this whole entity doesn't match
                if( filters == null || filters[i] == null )
                    continue;
 
                if( !filters[i].evaluate(array[i]) )
                    {
                    // Because we wasted a lot of time for something
                    // that should be filtered in most cases... usually
                    // it's a bug when it isn't.
                    //if( rechecking )
                    //    log.warn( "Non-matching component:" + array[i] + " for entity:" + e );
                    return false;
                    }                    
                }
            
            // Just a safety net... can maybe be removed    
            ((DefaultEntity)e).validate();
                           
            return true;                
        }
 
        public void resolveChanges()
        {
            // So now we can take what we've accumulated and figure
            // out what's what.

            // Process the adds.
            for( DefaultEntity e : adds.values() )  
                {
                if( completeEntity(e) )
                    {
                    // It was an added entity
                    if( add(e) )
                        addedEntities.add(e);
                    }
                else
                    {
                    // It couldn't be completed so it is not really an add...
                    // it's just a waste of our time. ;)
                    }
                }
                
            // Now... see which changes were removes and which ones
            // were real updates
            for( EntityId id : mods )
                {
                Entity e = entities.get(id);
                
                //((DefaultEntity)e).validate();
                               
                // e.isComplete() would run through the whole component
                // loop which we will do anyway to check the filters.
                // We'll combine these into one step
                if( entityMatches(e) )
                    {
                    changedEntities.add(e);
                    }
                else
                    {
                    if( remove(e) )                    
                        removedEntities.add(e);
                    }
                }
                
            // Clear the buffers for next time
            adds.clear();
            mods.clear();                                  
        }
    } 
}

