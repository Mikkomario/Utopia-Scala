package utopia.annex.controller

import java.time.Instant

import utopia.access.http.Status
import utopia.access.http.Status.NotModified
import utopia.annex.model.response.{Response, ResponseBody}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.Loop
import utopia.flow.container.FileContainer
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.WaitTarget.WaitDuration

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

/**
  * A looping background process used for requesting up-to-date container data
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
abstract class ContainerUpdateLoop[A](container: FileContainer[A])(implicit exc: ExecutionContext) extends Loop
{
	// ATTRIBUTES	----------------------
	
	private var nextRequestWaitModifier = 0.0
	
	
	// ABSTRACT	--------------------------
	
	/**
	  * @return Container used for holding the last request time, which is then used as a request content threshold
	  */
	protected def requestTimeContainer: FileContainer[Value]
	
	/**
	  * @param status Response status (not OK)
	  * @param message Response message. None if no message was provided with the response.
	  */
	protected def handleFailureResponse(status: Status, message: Option[String]): Unit
	
	/**
	  * Performs an update request
	  * @param timeThreshold Update time threshold to use. None if all data should be included
	  * @return Eventual request results
	  */
	protected def makeRequest(timeThreshold: Option[Instant]): Future[Try[Response]]
	
	/**
	  * @param oldData Old container content
	  * @param readData Content read from a server response
	  * @return Content that should be stored in the container
	  */
	protected def merge(oldData: A, readData: ResponseBody): A
	
	/**
	  * @return Normal interval between updates
	  */
	def standardUpdateInterval: FiniteDuration
	
	
	// IMPLEMENTED	-----------------------
	
	override def runOnce() =
	{
		val newRequestTime = Instant.now()
		val lastRequestTime = requestTimeContainer.current.instant
		
		// Prepares and performs the request
		nextRequestWaitModifier = makeRequest(lastRequestTime).waitForResult() match
		{
			case Success(response) =>
				response match
				{
					case Response.Success(status, body) =>
						// Updates last update time locally
						requestTimeContainer.current = Some(newRequestTime)
						// If there was new data, updates container
						if (status != NotModified)
						{
							container.pointer.update { old => merge(old, body) }
							0.75
						}
						else
							1
					case Response.Failure(status, message) =>
						handleFailureResponse(status, message)
						if (status.isTemporary)
							5
						else
							15
				}
			case Failure(_) => 2
		}
	}
	
	override def nextWaitTarget = WaitDuration(standardUpdateInterval * nextRequestWaitModifier)
}

