package utopia.journey.controller.user

import utopia.annex.controller.QueueSystem
import utopia.annex.model.error.EmptyResponseException
import utopia.annex.model.request.GetRequest
import utopia.annex.model.response.ResponseBody.{Content, Empty}
import utopia.annex.model.schrodinger.TryFindSchrodinger
import utopia.flow.parse.file.container.OptionObjectFileContainer
import utopia.flow.util.CollectionExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.journey.model.error.NoUserDataError
import utopia.journey.util.JourneyContext._
import utopia.metropolis.model.combined.user.UserWithLinks

import scala.util.{Failure, Success}

/**
  * Used for accessing the current user's data
  * @author Mikko Hilpinen
  * @since 12.7.2020, v0.1
  * @param queueSystem An authorized interface for making requests to server
  * @param loginEmail The email address the user used for logging in this time.
  *                   None if device key was used instead.
  */
class Me(queueSystem: QueueSystem, loginEmail: Option[String])
{
	// ATTRIBUTES	---------------------------
	
	private val container = new OptionObjectFileContainer(containersDirectory/"user.json", UserWithLinks)
	
	private val previousUserId = container.current.map { _.id }
	// This value is updated when more information becomes available
	// FIXME: This implementation expects email address to always be specified
	private var userWasChanged = container.current.forall { user =>
		loginEmail.exists { email => !user.settings.email.contains(email) }
	}
	// FIXME: Same problem here
	private val schrodinger = new TryFindSchrodinger(container.current
		.filter { local => loginEmail.forall { local.settings.email.contains(_) } }
		.toTry { new NoUserDataError("No local user data stored") })
	
	// When initializing the invitations access point (lazy), uses the latest known status about active user changing
	// If this information is incorrect, cached invitation answers may not get posted or they may be rejected by
	// the server (which is not a huge problem)
	/**
	  * An access point to the invitations concerning this user
	  */
	lazy val invitations = new Invitations(queueSystem, isSameUser = !userWasChanged)
	// Same thing with organizations
	/**
	  * An access point to this user's organizations
	  */
	lazy val organizations = new MyOrganizations(queueSystem, isSameUser = !userWasChanged)
	
	
	// INITIAL CODE	---------------------------
	
	// Immediately retrieves current user data from the server
	schrodinger.completeWith(queueSystem.push(GetRequest("users/me"))) {
		case c: Content => c.single(UserWithLinks).parsed
		case Empty => Failure(new EmptyResponseException("Expected user content but empty response received instead"))
	}
	
	// Stores the user data when it arrives (if it arrives). Also checks whether the active user changed.
	schrodinger.serverResultFuture.foreach {
		case Success(user) =>
			userWasChanged = !previousUserId.contains(user.id)
			container.current = Some(user)
		case Failure(error) => log(error, "User data retrieval failed")
	}
}
