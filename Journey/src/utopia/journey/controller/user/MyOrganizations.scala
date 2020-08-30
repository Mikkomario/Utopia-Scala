package utopia.journey.controller.user

import utopia.annex.controller.QueueSystem
import utopia.annex.model.request.GetRequest
import utopia.annex.model.schrodinger.CachedFindSchrodinger
import utopia.flow.async.AsyncExtensions._
import utopia.flow.container.ObjectsFileContainer
import utopia.flow.container.SaveTiming.Delayed
import utopia.flow.util.FileExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.journey.util.JourneyContext._
import utopia.metropolis.model.combined.user.MyOrganization

/**
  * An access point used for accessing the current user's organizations
  * @author Mikko Hilpinen
  * @since 12.7.2020, v1
  * @param queueSystem An authorized queue system used when requesting organization data
  * @param isSameUser Whether the current active user is the same as previously logged user (default = true)
  */
class MyOrganizations(queueSystem: QueueSystem, isSameUser: Boolean = true)
{
	// ATTRIBUTES	----------------------------
	
	private val container = new ObjectsFileContainer(containersDirectory/"organizations.json", MyOrganization,
		Delayed(3.minutes))
	// If user was changed, clears all cached data
	if (!isSameUser)
		container.clear()
	
	private var _schrodinger = makeNewSchrodinger()
	
	// TODO: Implement a unique queue when post features are being added
	
	
	// COMPUTED	-------------------------------
	
	/**
	  * a schrödinger containing a list of the current user's organizations
	  */
	def schrodinger = _schrodinger.view
	
	
	// OTHER	-------------------------------
	
	/**
	  * Reads organization data again
	  * @return A schrödinger with current and future data
	  */
	def update() =
	{
		// If the previous request hasn't finished yet, doesn't do anything
		// Otherwise retrieves organization data again and updates cache based on the results
		if (_schrodinger.isResolved)
			_schrodinger = makeNewSchrodinger()
		
		schrodinger
	}
	
	private def makeNewSchrodinger() =
	{
		val newSchrodinger = new CachedFindSchrodinger(container.current)
		// Retrieves up-to-date organization data
		newSchrodinger.completeWith(queueSystem.push(GetRequest("users/me/organizations"))) {
			_.vector(MyOrganization).parsed } { log(_) }
		// Caches the read data
		newSchrodinger.serverResultFuture.foreachSuccess { container.current = _ }
		
		newSchrodinger
	}
}
