package utopia.journey.controller

import utopia.annex.controller.{QueueSystem, RequestQueue}
import utopia.annex.model.error.EmptyResponseException
import utopia.annex.model.request.GetRequest
import utopia.annex.model.response.RequestNotSent.RequestFailed
import utopia.annex.model.response.Response
import utopia.annex.model.response.ResponseBody.{Content, Empty}
import utopia.annex.model.schrodinger.CachedFindSchrodinger
import utopia.flow.container.OptionObjectFileContainer
import utopia.flow.util.FileExtensions._
import utopia.journey.util.JourneyContext._
import utopia.metropolis.model.combined.user.UserWithLinks

import scala.util.{Failure, Success}

/**
  * Used for accessing the current user's data
  * @author Mikko Hilpinen
  * @since 12.7.2020, v1
  */
class Me(queueSystem: QueueSystem, loginEmail: Option[String])
{
	// ATTRIBUTES	---------------------------
	
	private val container = new OptionObjectFileContainer(containersDirectory/"user.json", UserWithLinks)
	
	private var userChanged = loginEmail.exists { email => container.current.forall { _.settings.email != email } }
	private val schrodinger = new CachedFindSchrodinger(container.current)
	
	// Immediately retrieves current user data from the server
	schrodinger.completeWith(queueSystem.push(GetRequest("users/me"))) {
		case c: Content => c.single(UserWithLinks).parsed.map { Some(_) }
		case Empty => Failure(new EmptyResponseException("Expected user content but empty response received instead"))
	} { log(_) }
	
	
	// val userWasChanged = container.current.forall { user => LocalDevice.lastUserId.forall { _ == user.id } }
	
	
	// INITIAL CODE	---------------------------
	
	// If the user changed, forgets current data
}
