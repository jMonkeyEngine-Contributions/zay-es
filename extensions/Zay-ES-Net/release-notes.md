Zay-ES-Net v1.4.1 (unreleased)
------------------


Zay-ES-Net v1.4.0 (latest)
------------------
* Fixed HostedEntityData.getStringInfo() to send back a null if the ID doesn't exist
    for the string rather than sending back -1.  A -1 would have been cached but a
    null won't be.
* Added EntityDataHostedServer.getHostedEntityData() for setting up connection
    specific entity stuff from other services.
* Added support for client-specific ComponentVisibility filters that can be used to
    limit the clients' views of certain component values, e.g.: based on permissions,
    local object visibility, etc..
* Added lots of additional trace logging in support of the above.
* Fixed HostedEntityData.getEntitySet() to force the next sendUpdates() to make
    at least one pass through the entity sets.  Else some components won't be
    properly tracked.


Zay-ES-Net v1.3.1
------------------
* Fixed a bug introduced in PR #18 where the filter check in RemoteEntityData
    completeEntity() was inverted.


Zay-ES-Net v1.3.0
------------------
* RemoteEntityData.completeEntity() modified to check the components against
    the filter to fix the case where two entity sets share the same components
    but not the same filter.  See PR #18
* Fix NPE in RemoteStringIndex when asking for a non-existant string. See Issue: #14
* Set sourceCompatibility to 1.7 and turned on detailed 'unchecked' warnings
* Fixed a ton of 'unchecked' related issues.  Many of the generic method signatures
    have changed.
* Some javadoc formatting fixes.


Zay-ES-Net v1.2.1
------------------
* Upgraded the project to be JDK 1.7 and source 1.7 based.
* Added support for a TransientComponent tagging interface
    that can be used to avoid sending certain component types over the
    wire. (They go over as null.)  Note: the classes themselves still
    need to be registered with the Serializer so that the types can be
    sent.  This is a compromise over a more invasive and ultimately less
    flexible transient design. (For example: this still lets you grab
    entities with a transient component for selection purposes, it just won't
    have a value.)
* Added support for the new WatchedEntity feature of EntityData.  This can
    be a more efficient way of tracking one entity versus constantly polling
    using getComponent().
* Added EntityDataHostedService and EntityDataClientService implementations
    to take advantage of JME 3.1's new service model.
* Server-side entity data services modified to require ObservableEntityData
    implementations.
* Completely reworked how entity updates are sent to clients.  The
    HostedEntityData.sendUpdates() method is entirely new and based on a new
    approach.  In short: client interest is tracked in a mark-and-sweep data
    structure and only relevant changes are sent after the data structures
    have been updated.  This avoids sending out of order information as well
    as redundant or hidden information.
* RemoteEntityData's internal RemoteEntitySet now avoids resolving incomplete
    entities.  In the latest code, if the entity is a member of the set then
    it would already have received all of the necessary data.  A round trip
    to the server to grab data that definitely won't be there is a wasteful
    op, now avoided.


Zay-ES-Net v1.1.1
------------------
* Implemented RemoteEntityData's string index.
* Added trace log for cache hits in RemoteEntityData.


Zay-ES-Net r1207
-----------------
* Modified RemoteEntityData.RemoteEntitySet.release() to check
    to see if the connection is still connected before sending
    the release message.
* Fixed a bug where really eager sendUpdate() calls were causing
    thread contention with entity set creation.  This would be
    especially true for large sets and short update cycles.
* Fixed a similar future-bug for resetting the filter, too,
    though that one would be much harder to time just right, ie:
    even more bizarre if seen in the wild.
