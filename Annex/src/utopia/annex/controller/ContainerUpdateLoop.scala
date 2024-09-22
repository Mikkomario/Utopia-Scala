package utopia.annex.controller

import utopia.access.http.Status
import utopia.access.http.Status.NotModified
import utopia.annex.model.response.{RequestFailure, RequestResult, Response}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.LoopingProcess
import utopia.flow.async.process.WaitTarget.WaitDuration
import utopia.flow.parse.file.container.FileContainer
import utopia.flow.time.Now
import utopia.flow.util.EitherExtensions._
import utopia.flow.util.UncertainBoolean
import utopia.flow.util.UncertainBoolean.CertainBoolean
import utopia.flow.util.logging.Logger

import java.time.Instant
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * A looping background process used for requesting up-to-date container data
  * @tparam A Type of contents stored in the container
  * @tparam R Type of parsed results acquired from the server
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
abstract class ContainerUpdateLoop[A, R](container: FileContainer[A])(implicit exc: ExecutionContext, logger: Logger)
	extends LoopingProcess
{
	// ABSTRACT	--------------------------
	
	/**
	  * @return Container used for holding the last request time, which is then used as a request content threshold
	  */
	protected def requestTimeContainer: FileContainer[Option[Instant]]
	
	/**
	  * @param status Response status (not OK)
	  * @param message Response message. Empty if no message was provided with the response.
	  */
	protected def handleFailureResponse(status: Status, message: String): Unit
	
	/**
	  * Performs an update request
	  * @param timeThreshold Update time threshold to use. None if all data should be included
	  * @return Eventual request results
	  */
	protected def makeRequest(timeThreshold: Option[Instant]): Future[RequestResult[R]]
	
	/**
	  * @param oldData Old container content
	  * @param readData Content read from a server response (may be empty)
	  * @return Content that should be stored in the container + scheduled duration until the next check
	  */
	protected def merge(oldData: A, readData: R): (A, FiniteDuration)
	
	/**
	  * @return Normal interval between updates
	  */
	def standardUpdateInterval: FiniteDuration
	
	
	// IMPLEMENTED	-----------------------
	
	override protected def isRestartable = true
	
	override def iteration() = {
		val newRequestTime = Now.toInstant
		val lastRequestTime = requestTimeContainer.current
		
		// Prepares and performs the request
		val waitModifier = makeRequest(lastRequestTime).waitFor() match {
			case Success(result) =>
				result match {
					case success: Response.Success[R] =>
						// Updates last update time locally
						// Reads the update time from the response headers, if available
						requestTimeContainer.current = Some(success.headers.date.getOrElse(newRequestTime))
						// If there was new data, updates container
						if (success.status != NotModified)
							Right(container.pointer.mutate { old => merge(old, success.value).swap })
						else
							Left(1)
					case Response.Failure(status, message, _) =>
						handleFailureResponse(status, message)
						Left(status.isTemporary match {
							case CertainBoolean(isTemporary) => if (isTemporary) 5 else 50
							case UncertainBoolean => 15
						})
					case _: RequestFailure => Left(2)
				}
			case Failure(_) => Left(2)
		}
		val waitDuration = waitModifier.rightOrMap { standardUpdateInterval * _ }
		Some(WaitDuration(waitDuration))
	}
}

