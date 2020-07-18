package utopia.journey.controller

import utopia.annex.controller.{QueueSystem, RequestQueue}
import utopia.annex.model.error.EmptyResponseException
import utopia.annex.model.request.GetRequest
import utopia.annex.model.response.RequestNotSent.RequestFailed
import utopia.annex.model.response.Response
import utopia.annex.model.response.ResponseBody.{Content, Empty}
import utopia.annex.model.schrodinger.{CachedFindSchrodinger, TryFindSchrodinger}
import utopia.flow.container.OptionObjectFileContainer
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.FileExtensions._
import utopia.journey.model.error.NoUserDataError
import utopia.journey.util.JourneyContext._
import utopia.metropolis.model.combined.user.UserWithLinks

import scala.util.{Failure, Success}

/**
  * Used for accessing the current user's data
  * @author Mikko Hilpinen
  * @since 12.7.2020, v1
  * @param queueSystem An authorized interface for making requests to server
  * @param loginEmail The email address the user used for logging in this time.
  *                   None if device key was used instead.
  */
class Me(queueSystem: QueueSystem, loginEmail: Option[String])
{
	// ATTRIBUTES	---------------------------
	
	private val container = new OptionObjectFileContainer(containersDirectory/"user.json", UserWithLinks)
	
	private val schrodinger = new TryFindSchrodinger(container.current.filter { local =>
		loginEmail.forall { local.settings.email == _ } }.toTry { new NoUserDataError("No local user data stored") })
	
	// Immediately retrieves current user data from the server
	schrodinger.completeWith(queueSystem.push(GetRequest("users/me"))) {
		case c: Content => c.single(UserWithLinks).parsed
		case Empty => Failure(new EmptyResponseException("Expected user content but empty response received instead"))
	}
	
	
	// INITIAL CODE	---------------------------
	
	// Stores the user data when it arrives (if it arrives)
	schrodinger.serverResultFuture.foreach {
		case Success(user) => container.current = Some(user)
		case Failure(error) => log(error, "User data retrieval failed")
	}
}
