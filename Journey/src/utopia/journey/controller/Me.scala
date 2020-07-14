package utopia.journey.controller

import utopia.annex.controller.{QueueSystem, RequestQueue}
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
	queueSystem.push(GetRequest("users/me")).foreach {
		case Right(response) =>
			response match
			{
				case Response.Success(status, body) =>
					body match
					{
						case c: Content =>
							c.single(UserWithLinks).parsed match
							{
								case Success(user) =>
									// Remembers the user and completes the schrödinger
									/*
									container.current = Some(user)
									schrodinger.complete(Right(Some(user)))
									*/
								case Failure(error) =>
									/*
									log(error, "Failed to parse user data sent by the server")
									schrodinger.complete(Right(container.current))
									
									 */
							}
						case Empty =>
							log(s"No body received in response when requesting current user data. Response status: $status")
							
					}
				case Response.Failure(status, message) =>
			}
		case Left(notSent) =>
			notSent match
			{
				case RequestFailed(error) =>
				case _ =>
			}
	}
	
	
	// val userWasChanged = container.current.forall { user => LocalDevice.lastUserId.forall { _ == user.id } }
	
	
	// INITIAL CODE	---------------------------
	
	// If the user changed, forgets current data
}
