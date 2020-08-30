package utopia.journey.controller.user

import java.time.Instant

import utopia.access.http.Status.{Forbidden, Unauthorized}
import utopia.annex.controller.{PersistedRequestHandler, PersistingRequestQueue, QueueSystem}
import utopia.annex.model.error.{RequestDeniedException, RequestFailedException, UnauthorizedRequestException}
import utopia.annex.model.request.{GetRequest, PostRequest}
import utopia.annex.model.response.RequestNotSent.{RequestFailed, RequestWasDeprecated}
import utopia.annex.model.response.ResponseBody.{Content, Empty}
import utopia.annex.model.response.{RequestNotSent, Response}
import utopia.annex.model.schrodinger.CachedFindSchrodinger
import utopia.flow.collection.VolatileList
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.util.FileExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.journey.model.InvitationResponseSpirit
import utopia.journey.util.JourneyContext._
import utopia.metropolis.model.combined.organization.{DescribedInvitation, InvitationWithResponse}

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

/**
  * Used for accessing currently pending invitations
  * @author Mikko Hilpinen
  * @since 12.7.2020, v1
  * @param queueSystem An authorized queue system used when making queries to server
  * @param maxResponseWait Maximum time to wait for pending requests before discarding the request (default = 10 seconds)
  * @param isSameUser Whether the current user is the same as the previously logged user (default = true)
  */
class Invitations(queueSystem: QueueSystem, maxResponseWait: FiniteDuration = 10.seconds, isSameUser: Boolean = true)
{
	// ATTRIBUTES	---------------------------
	
	private val cached = VolatileList[DescribedInvitation]()
	private val hiddenIds = VolatileList[Int]()
	
	// Handles persisted requests only when the same user is still logged in
	private val (queue, loadErrors) = PersistingRequestQueue(queueSystem, requestsDirectory/"invitations.json",
		if (isSameUser) Vector(PostAnswerRequestHandler) else Vector(), 2)
	
	
	// INITIAL CODE	--------------------------
	
	if (isSameUser)
	{
		// Logs possible load errors
		loadErrors.headOption.foreach { error =>
			log(error, s"Failed to handle persisted requests (${loadErrors.size} errors)")
		}
	}
	
	
	// COMPUTED	-------------------------------
	
	private def activeCached =
	{
		// Checks which invitations are still active
		val now = Instant.now()
		val result = cached.get.filterNot { _.wrapped.expireTime > now }
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
		val requestDeprecationTime = Instant.now() + maxResponseWait
		val request = GetRequest("users/me/invitations", Instant.now() < requestDeprecationTime)
		val schrodinger = new CachedFindSchrodinger(activeCached)
		
		// Completes the schrödinger asynchronously
		schrodinger.completeWith(queue.push(request)) { _.vector(DescribedInvitation).parsed } { log(_) }
		
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
		queue.push(PostRequest(InvitationResponseSpirit(invitationId, isAccepted, isBlocked))).map {
			case Right(response) =>
				response match
				{
					case _: Response.Success  =>
						// Removes the invitation from locally cached data
						cached.update { _.filterNot { _.id == invitationId } }
						hiddenIds -= invitationId
						Success(activeCached)
					case Response.Failure(status, message) =>
						status match
						{
							case Unauthorized => Failure(new UnauthorizedRequestException(
								message.getOrElse("Unauthorized to answer the invitation")))
							case Forbidden => Failure(new RequestDeniedException(message.getOrElse(
								"Invitation response was denied")))
							case _ => Failure(new RequestFailedException(message.getOrElse(
								s"Server responded with status $status")))
						}
				}
			case Left(notSent) =>
				notSent match
				{
					case RequestFailed(error) => Failure(error)
					case RequestWasDeprecated => Failure(
						new RequestFailedException("Invitation response was deprecated before it could be sent"))
				}
		}
	}
	
	
	// NESTED	--------------------------------
	
	// Used for handling request responses for previous session invitation answers
	private object PostAnswerRequestHandler extends PersistedRequestHandler
	{
		// ATTRIBUTES	------------------------
		
		private val bodyFactory = InvitationResponseSpirit
		
		override val factory = PostRequest.factory(bodyFactory)
		
		
		// IMPLEMENTED	------------------------
		
		override def shouldHandle(requestModel: Model[Constant]) =
			bodyFactory.isProbablyValidModel(requestModel)
		
		override def handle(result: Either[RequestNotSent, Response]) =
		{
			result match
			{
				case Right(response) =>
					response match
					{
						case Response.Success(status, body) =>
							body match
							{
								case c: Content =>
									c.single(InvitationWithResponse).parsed match
									{
										case Success(invitation) =>
											// Removes the invitation from the cache, as well as from among hidden ids
											val invitationId = invitation.id
											cached.update { _.filterNot { _.id == invitationId } }
											hiddenIds -= invitationId
										case Failure(error) => log(error,
											"Couldn't interpret response to invitation answer")
									}
								case Empty => log(s"Invitation answer response didn't contain a body. Status: $status")
							}
						case Response.Failure(status, message) => log(new RequestFailedException(message.getOrElse(
							s"Server rejected invitation answer. Status: $status")))
					}
				case Left(notSent) =>
					notSent match
					{
						case RequestFailed(error) => log(error, "Failed to send invitation answer")
						case RequestWasDeprecated => () // Ignored
					}
			}
		}
	}
}
