package utopia.journey.controller

import utopia.annex.controller.{QueueSystem, RequestQueue}
import utopia.annex.model.request.GetRequest
import utopia.flow.container.OptionObjectFileContainer
import utopia.flow.util.FileExtensions._
import utopia.journey.util.JourneyContext._
import utopia.metropolis.model.combined.user.UserWithLinks

/**
  * Used for accessing the current user's data
  * @author Mikko Hilpinen
  * @since 12.7.2020, v1
  */
class Me(queueSystem: QueueSystem, loginEmail: Option[String])
{
	// ATTRIBUTES	---------------------------
	
	private val container = new OptionObjectFileContainer(containersDirectory/"user.json", UserWithLinks)
	private val queue = RequestQueue(queueSystem)
	
	private var userChanged = loginEmail.exists { email => container.current.forall { _.settings.email != email } }
	// private val
	
	// Immediately retrieves current user data from the server
	// queue.push(GetRequest("users/me"))
	
	
	// val userWasChanged = container.current.forall { user => LocalDevice.lastUserId.forall { _ == user.id } }
	
	
	// INITIAL CODE	---------------------------
	
	// If the user changed, forgets current data
}
