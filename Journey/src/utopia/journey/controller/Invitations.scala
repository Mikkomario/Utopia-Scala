package utopia.journey.controller

import java.time.Instant

import utopia.access.http.Status.Unauthorized
import utopia.annex.controller.{PersistedRequestHandler, PersistingRequestQueue, QueueSystem}
import utopia.annex.model.request.{GetRequest, PostRequest}
import utopia.annex.model.response.RequestNotSent.{RequestFailed, RequestWasDeprecated}
import utopia.annex.model.response.ResponseBody.{Content, Empty}
import utopia.annex.model.response.{RequestNotSent, Response}
import utopia.annex.model.schrodinger.CachedFindSchrodinger
import utopia.flow.collection.VolatileList
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.FileExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.journey.model.InvitationResponseSpirit
import utopia.journey.model.error.RequestFailedException
import utopia.journey.util.JourneyContext._
import utopia.metropolis.model.combined.organization.{DescribedInvitation, InvitationWithResponse}

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

/**
  * Used for accessing currently pending invitations
  * @author Mikko Hilpinen
  * @since 12.7.2020, v1
  */
class Invitations(queueSystem: QueueSystem, maxResponseWait: FiniteDuration = 5.seconds)
{
	// ATTRIBUTES	---------------------------
	
	private val cached = VolatileList[DescribedInvitation]()
	private val hiddenIds = VolatileList[Int]()
	
	private val (queue, loadErrors) = PersistingRequestQueue(queueSystem, requestsDirectory/"invitations.json",
		Vector(PostAnswerRequestHandler), 2)
	
	
	// INITIAL CODE	--------------------------
	
	// Logs possible load errors
	loadErrors.headOption.foreach { error =>
		log(error, s"Failed to handle persisted requests (${loadErrors.size} errors)")
	}
	
	
	// COMPUTED	-------------------------------
	
	private def activeCached =
	{
		// Checks which invitations are still active
		val now = Instant.now()
		val result = cached.get.filterNot { _.wrapped.expireTime > now }
		// Updates cached data if necessary
		cached.setIf { _.size == result.size }(result)
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
		queue.push(request).foreach { result =>
			schrodinger.complete(result.mapRight {
				case Response.Success(_, body) =>
					body.vector(DescribedInvitation).parsed match
					{
						case Success(invitations) =>
							cached.set(invitations)
							invitations
						case Failure(error) =>
							log(error, "Couldn't parse invitations from a server response")
							activeCached
					}
				case Response.Failure(status, message) =>
					if (status != Unauthorized)
					{
						val errorMessage = message match
						{
							case Some(m) => s"Invitation retrieval failed ($status). Response message: $m"
							case None => s"Invitation retrieval failed with status $status"
						}
						log(new RequestFailedException(errorMessage))
					}
					activeCached
			})
		}
		
		schrodinger
	}
	
	
	// OTHER	--------------------------------
	
	def answer(invitationId: Int, isAccepted: Boolean, isBlocked: Boolean = false) =
	{
		// Temporarily or permanently removes the targeted invitation from the cached data
		hiddenIds :+= invitationId
		
		// TODO: Continue (send request, return a future of either success or failure)
	}
	
	
	// NESTED	--------------------------------
	
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
