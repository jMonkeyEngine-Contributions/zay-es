Zay-ES Core v1.6.0 (unrelease)
--------------------
* Modified SqlEntityData to allowing specifying the max string size
    for the StringIndex (default is still 50 characters)
* Modified StringTable to automatically upgrade the table if the
    specified max size is larger than the current column size.
* Added EntityData.removeComponents() and implementations to various
    default implementations.  Allows removing more than one component
    at a time similar to the way setComponents() can set more than one
    at a time.
* Added SqlComponentFactory and refactored the SqlEntityData classes to
    use it.  This allows overriding the default result set -> component
    creation to customize it for different object types.     
    

Zay-ES Core v1.5.0 (latest)
--------------------
* Added some trace logging to DefaultEntityData.removeEntity()
    and DefaultEntityData.removeComponent()
* Added the PersistentEntityData interface to allow EntityData implementations
    to implement persistence-specific methods.
* Modified SqlEntityData to implement the PersistentEntityData interface.
* Added PersistentEntityData.markPersistentType() for indicating that
    a component should be persistent even if it doesn't implement the
    PersistentComponent interface.  Sometimes a library component's persistability
    is not black and white between one codebase and another and requiring
    interface implementation was too restrictive.
* Modified the sql ComponentTable class to work with non-public type
    constructors.
* Fixed sql ComponentTable to handle components with no fields.     
* Added a proper toString() method to DefaultWatchedEntity.
* Fixed redundant EntityChange events from being sent if EntityData.removeEntity()
    is used.


Zay-ES Core v1.4.0 
-------------------
* Upgraded the build to use gradle 7.4.2
* Migrated publishing to maven central instead of jcenter


Zay-ES Core v1.3.2
-------------------
* Updated StringIndex javadoc to provide more information.


Zay-ES Core v1.3.1
-------------------
* Fixed MemStringIndex.getStringId(string, false) to return -1 instead of throwing
    an NPE if the string has no ID.
* Modified DefaultEntityData.addEntityComponentListener() to throw an IllegalArgumentException
    for null listeners.


Zay-ES Core v1.3.0
-------------------
* MemStringIndex fixed to consider the boolean 'add' parameter.
* Set sourceCompatibility to 1.7 and turned on detailed 'unchecked' warnings
* Fixed a ton of 'unchecked' related issues.  Many of the generic method signatures
    have changed.
* Some javadoc formatting fixes.


Zay-ES Core v1.2.1
-------------------
* Upgraded the project to be JDK 1.7 and source 1.7 based.
* Added the ability to get a WatchedEntity which acts like an entity
    except can be watched for changes in a way similar to a whole
    EntitySet.  Useful for single-entity focused systems like player
    displays/controls.
* Deprecated the EntitySet.applyChanges(Set) method as it's not really
    possible to accurately return the changes that caused the set
    to be in its new state... especially not without a proper ordering.
    The network code that relies on this method has been reworked to
    do it more correctly.
* DefaultEntityData.getEntities() now delegates loading of the entities
    to the set instead of hand-rolling it. (The DefaultEntitySet was
    already capable of loading its own data.)
* DefaultEntitySet's Transaction.completeEntity() method was moved to
    the outer class so that subclasses can override how incomplete entities
    are resolved by the set.


Zay-ES Core v1.1.1
-------------------
* Fixed the DefaultEntitySet logger to log to the appropriate category.
* Added some defensive entityId parameter checks to DefaultEntityData
    getComponent and setComponent.
* Added an in-memory StringIndex implementation and include it by default
    in DefaultEntityData if it hasn't been extended.


Zay-ES Core r1202
------------------
* Reformatted source code to be closer to normal Java coding conventions.
* Added some Javadoc here and there.
* Removed unused methods update() and clear() from DefaultEntity


Zay-ES Core r1200
-------------------
* Removed the direct DefaultEntityData reference from DefaultEntity
* Removed the direct reference to DefaultEntityData from DefaultEntitySet.
* Added a protected isReleased() method to DefaultEntitySet so subclasses
    can check released state.
* Added a protected method for accessing the change queue.
* Added a method to DefaultEntitySet.Transaction for directly injecting
    added entities.  (Useful for when entities are loaded asynchronously.)
* Added an internal optimization to DefaultEntitySet that avoids attempting
    to re-retrieve a component that was removed.
* Added a protected registerComponentHandler() method to DefaultEntityData
    so that subclasses can customimze component handler implementations.
* Removed the SpiderMonkey dependency from core Zay-ES
* Converted direct log4j references to slf4j.

