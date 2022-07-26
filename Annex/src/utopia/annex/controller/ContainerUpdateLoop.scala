package utopia.annex.controller

import java.time.Instant
import utopia.access.http.Status
import utopia.access.http.Status.NotModified
import utopia.annex.model.response.{Response, ResponseBody}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.LoopingProcess
import utopia.flow.container.FileContainer
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.WaitTarget.WaitDuration
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.logging.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

/**
  * A looping background process used for requesting up-to-date container data
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
abstract class ContainerUpdateLoop[A](container: FileContainer[A])(implicit exc: ExecutionContext, logger: Logger)
	extends LoopingProcess
{
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
	  * @return Content that should be stored in the container + scheduled duration until the next check
	  */
	protected def merge(oldData: A, readData: ResponseBody): (A, FiniteDuration)
	
	/**
	  * @return Normal interval between updates
	  */
	def standardUpdateInterval: FiniteDuration
	
	
	// IMPLEMENTED	-----------------------
	
	override protected def isRestartable = true
	
	override def iteration() =
	{
		val newRequestTime = Instant.now()
		val lastRequestTime = requestTimeContainer.current.instant
		
		// Prepares and performs the request
		val waitModifier = makeRequest(lastRequestTime).waitForResult() match {
			case Success(response) =>
				response match {
					case Response.Success(status, body) =>
						// Updates last update time locally
						requestTimeContainer.current = Some(newRequestTime)
						// If there was new data, updates container
						if (status != NotModified && body.nonEmpty)
							Right(container.pointer.pop { old => merge(old, body).swap })
						else
							Left(1)
					case Response.Failure(status, message) =>
						handleFailureResponse(status, message)
						Left(if (status.isTemporary) 5 else 15)
				}
			case Failure(_) => Left(2)
		}
		val waitDuration = waitModifier.rightOrMap { standardUpdateInterval * _ }
		Some(WaitDuration(waitDuration))
	}
}

