package utopia.journey.controller

import java.time.Instant

import utopia.access.http.Status.Unauthorized
import utopia.annex.controller.QueueSystem
import utopia.annex.model.request.GetRequest
import utopia.annex.model.response.Response
import utopia.annex.model.schrodinger.CachedFindSchrodinger
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.journey.model.error.RequestFailedException
import utopia.journey.util.JourneyContext._
import utopia.metropolis.model.combined.organization.DescribedInvitation

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

/**
  * Used for accessing currently pending invitations
  * @author Mikko Hilpinen
  * @since 12.7.2020, v1
  */
class Invitations(queue: QueueSystem, maxResponseWait: FiniteDuration = 5.seconds)
{
	// ATTRIBUTES	---------------------------
	
	private var cached = Vector[DescribedInvitation]()
	
	
	// COMPUTED	-------------------------------
	
	/**
	  * @return A Schrödinger that will contain the pending invitations. Prepopulated with cached data.
	  */
	def pending =
	{
		val requestDeprecationTime = Instant.now() + maxResponseWait
		val request = GetRequest("users/me/invitations", Instant.now() < requestDeprecationTime)
		val schrodinger = new CachedFindSchrodinger(cached)
		
		// Completes the schrödinger asynchronously
		queue.push(request).foreach { result =>
			schrodinger.complete(result.mapRight {
				case Response.Success(_, body) =>
					body.vector(DescribedInvitation).parsed match
					{
						case Success(invitations) =>
							cached = invitations
							invitations
						case Failure(error) =>
							log(error, "Couldn't parse invitations from a server response")
							cached
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
					cached
			})
		}
		
		schrodinger
	}
}
