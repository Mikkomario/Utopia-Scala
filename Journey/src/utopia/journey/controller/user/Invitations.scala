package utopia.journey.controller.user

import utopia.access.http.Status.{Forbidden, Unauthorized}
import utopia.annex.controller.{PersistedRequestHandler, PersistingRequestQueue, QueueSystem}
import utopia.annex.model.error.{RequestDeniedException, UnauthorizedRequestException}
import utopia.annex.model.request.{GetRequest, PostSpiritRequest, RequestQueueable}
import utopia.annex.model.response.RequestNotSent.RequestWasDeprecated
import utopia.annex.model.response.ResponseBody.{Content, Empty}
import utopia.annex.model.response.{RequestFailure, RequestResult, Response}
import utopia.annex.model.schrodinger.CachedFindSchrodinger
import utopia.disciple.model.error.RequestFailedException
import utopia.flow.collection.mutable.VolatileList
import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.StringExtensions._
import utopia.journey.model.InvitationResponseSpirit
import utopia.journey.util.JourneyContext._
import utopia.metropolis.model.combined.organization.{DetailedInvitation, InvitationWithResponse}

import java.time.Instant
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

/**
  * Used for accessing currently pending invitations
  * @author Mikko Hilpinen
  * @since 12.7.2020, v0.1
  * @param queueSystem An authorized queue system used when making queries to server
  * @param maxResponseWait Maximum time to wait for pending requests before discarding the request (default = 10 seconds)
  * @param isSameUser Whether the current user is the same as the previously logged user (default = true)
  */
class Invitations(queueSystem: QueueSystem, maxResponseWait: FiniteDuration = 10.seconds, isSameUser: Boolean = true)
{
	// ATTRIBUTES	---------------------------
	
	private val cached = VolatileList[DetailedInvitation]()
	private val hiddenIds = VolatileList[Int]()
	
	// Handles persisted requests only when the same user is still logged in
	private val (queue, loadErrors) = PersistingRequestQueue(queueSystem, requestsDirectory/"invitations.json",
		if (isSameUser) Vector(PostAnswerRequestHandler) else Vector(), 2)
	
	
	// INITIAL CODE	--------------------------
	
	if (isSameUser)
	{
		// Logs possible load errors
		loadErrors.headOption.foreach { error =>
			logger(error, s"Failed to handle persisted requests (${loadErrors.size} errors)")
		}
	}
	
	
	// COMPUTED	-------------------------------
	
	private def activeCached =
	{
		// Checks which invitations are still active
		val now = Instant.now()
		val result = cached.value.filterNot { _.wrapped.expires > now }
		// Updates cached data if necessary
		cached.setIf { _.size != result.size }(result)
		// Applies id filtering if necessary
		val extraFilterIds = hiddenIds.toSet
		if (extraFilterIds.nonEmpty)
			result.filterNot { i => extraFilterIds.contains(i.id) }
		else
			result
	}
	
	/**
	  * @return A Schrödinger that will contain the pending invitations. Prepopulated with cached data.
	  */
	def pending =
	{
		val requestDeprecationTime = Now + maxResponseWait
		val request = GetRequest("users/me/invitations", Now < requestDeprecationTime)
		val schrodinger = new CachedFindSchrodinger(activeCached)
		
		// Completes the schrödinger asynchronously
		schrodinger
			.completeWith(queue.push(request)) { _.vector(DetailedInvitation).parsed.map { _.toVector } } { logger(_) }
		
		schrodinger.view
	}
	
	
	// OTHER	--------------------------------
	
	/**
	  * Answers an invitation by sending data to the server
	  * @param invitationId Id of the invitation to answer
	  * @param isAccepted Whether the invitation should be accepted
	  * @param isBlocked Whether future invitations should be blocked (default = false)
	  * @return Eventual server response. Contains a failure if answering failed at some point. Successful case contains
	  *         a list of newly updated (locally) invitations
	  */
	def answer(invitationId: Int, isAccepted: Boolean, isBlocked: Boolean = false) =
	{
		// Temporarily or permanently removes the targeted invitation from the cached data
		hiddenIds :+= invitationId
		
		// Sends the request, returns a modified version of the response
		queue.push(PostSpiritRequest(InvitationResponseSpirit(invitationId, isAccepted, isBlocked))).map {
			case _: Response.Success =>
				// Removes the invitation from locally cached data
				cached.update { _.filterNot { _.id == invitationId } }
				hiddenIds -= invitationId
				Success(activeCached)
			case Response.Failure(status, message, _) =>
				status match {
					case Unauthorized => Failure(new UnauthorizedRequestException(
						message.nonEmptyOrElse("Unauthorized to answer the invitation")))
					case Forbidden => Failure(new RequestDeniedException(message.nonEmptyOrElse(
						"Invitation response was denied")))
					case _ => Failure(new RequestFailedException(message.nonEmptyOrElse(
						s"Server responded with status $status")))
				}
			case failure: RequestFailure => failure.toFailure
		}
	}
	
	
	// NESTED	--------------------------------
	
	// Used for handling request responses for previous session invitation answers
	private object PostAnswerRequestHandler extends PersistedRequestHandler
	{
		// ATTRIBUTES	------------------------
		
		private val bodyFactory = InvitationResponseSpirit
		
		override val factory = PostSpiritRequest.factory(bodyFactory).mapParseResult { Right(_) }
		
		
		// IMPLEMENTED	------------------------
		
		override def shouldHandle(requestModel: Model) =
			bodyFactory.isProbablyValidModel(requestModel)
		
		override def handle(requestModel: Model, request: RequestQueueable, result: RequestResult) = {
			result match {
				case Response.Success(status, body, _) =>
					body match {
						case c: Content =>
							c.single(InvitationWithResponse).parsed match {
								case Success(invitation) =>
									// Removes the invitation from the cache, as well as from among hidden ids
									val invitationId = invitation.id
									cached.update { _.filterNot { _.id == invitationId } }
									hiddenIds -= invitationId
								case Failure(error) => logger(error,
									"Couldn't interpret response to invitation answer")
							}
						case Empty => logger(s"Invitation answer response didn't contain a body. Status: $status")
					}
				case Response.Failure(status, message, _) => logger(new RequestFailedException(message.nonEmptyOrElse(
					s"Server rejected invitation answer. Status: $status")))
				case RequestWasDeprecated => () // Ignored
				case failure: RequestFailure => logger(failure.cause, "Failed to send invitation answer")
			}
		}
	}
}
