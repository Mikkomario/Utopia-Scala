package utopia.disciple.controller

import utopia.disciple.http.request.Request
import utopia.disciple.http.response.{Response, StreamedResponse}
import utopia.flow.async.context.CloseHook
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.SettableOnce

import java.time.Instant
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

object AccessLogger
{
	// COMPUTED ------------------------------
	
	/**
	  * @param exc    Implicit execution context
	  * @param logger An implicit logging implementation
	  * @return A new access logger instance, which utilizes the specified logging implementation
	  */
	def newInstance(implicit exc: ExecutionContext, logger: Logger) = using(logger)
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param logger A logging implementation
	  * @param exc Implicit execution context
	  * @return A new access logger instance, which utilizes the specified logging implementation
	  */
	def using(logger: Logger)(implicit exc: ExecutionContext) = new AccessLogger(logger)
}

/**
  * Logs all outgoing requests and their responses.
  *
  * -- NOTICE --
  * This interceptor should be placed as the LAST interceptor in a Gateway instance.
  * Failure to do so may cause logging not to function correctly.
  * Also, please add this logger as both a request AND a response interceptor.
  *
  * @author Mikko Hilpinen
  * @since 19.1.2023, v1.5.4
  * @param logger The underlying logger implementation
  * @param exc Implicit execution context, used upon JVM shutdown to log the remaining requests
  */
class AccessLogger(logger: Logger)(implicit exc: ExecutionContext) extends RequestInterceptor with ResponseInterceptor
{
	// ATTRIBUTES   ----------------------------
	
	private lazy val withSecondsFormat = DateTimeFormatter.ofPattern("HH:mm:ss")
	private lazy val secondsFormat = DateTimeFormatter.ofPattern("ss")
	
	private val queue = Volatile.seq[(Request, SettableOnce[(Try[Response], Instant)], Instant)]()
	
	
	// INITIAL CODE ----------------------------
	
	// When the JVM is scheduled to close, logs all remaining items
	CloseHook.registerAction {
		lazy val jvmClosedFailure = Failure[Response](
			new InterruptedException("JVM was scheduled to shut down during a request"))
		queue.value.foreach { case (request, promise, requestTime) =>
			val (response, responseTime) = promise.value match {
				case Some(result) => result
				case None => jvmClosedFailure -> Now.toInstant
			}
			log(request, response, requestTime, responseTime)
		}
	}
	
	
	// IMPLEMENTED  ----------------------------
	
	override def intercept(request: Request): Request = {
		// Remembers the request, as well as the time, in order to log it later, once the response has been received
		implicit val log: Logger = SysErrLogger
		queue :+= (request, new SettableOnce(), Now)
		request
	}
	
	override def intercept(response: Try[StreamedResponse], request: Request): Try[StreamedResponse] = {
		// Attaches the acquired response to the queued request.
		// Logs request response -pairs in the same order as they were sent
		// This means that sometimes logging may be delayed and sometimes multiple items are logged at once
		val toLog = queue.mutate { queue =>
			// Checks whether the acquired response fits the first queued request
			queue.headOption.filter { _._1 == request } match {
				// Case: First request matches => Logs that request, as well as potential delayed items
				case Some(head) =>
					head._2.set(response -> Now)
					// The logged items get removed from the queue
					queue.popWhile { _._2.isCompleted }
				// Case: This response was not for the first queued request => Remembers the response but delays logging
				case None =>
					queue.find { case (req, p, _) => p.isEmpty && req == request } match {
						case Some((_, promise, _)) =>
							promise.set(response -> Now)
							Empty -> queue
						// Case: The response doesn't fit any of the queued requests
						// This may cause severe logic errors, as the queue might now be broken
						// This may happen if another interceptor modifies the outgoing requests
						// after this logger has received them
						case None =>
							logger(s"WARNING: Request matching failed for ${ request.method } ${ request.requestUri }")
							Vector((request, SettableOnce.set(response -> Now.toInstant), Now.toInstant)) -> queue
					}
			}
		}
		// Performs the actual logging
		toLog.foreach { case (request, promise, requestTime) =>
			val (response, responseTime) = promise.value.get
			log(request, response, requestTime, responseTime)
		}
		response
	}
	
	
	// OTHER    --------------------------------
	
	private def log(request: Request, response: Try[Response], requestTime: Instant, responseTime: Instant) = {
		val requestDuration = responseTime - requestTime
		val localRequestTime = requestTime.toLocalTime
		lazy val localResponseTime = responseTime.toLocalTime
		val timePart = {
			if (requestDuration < 1.seconds)
				localRequestTime.format(withSecondsFormat)
			else if (requestDuration < 1.minutes && localRequestTime.getMinute == localResponseTime.getMinute)
				s"${ localRequestTime.format(withSecondsFormat) }-${ localResponseTime.format(secondsFormat) }"
			else
				s"${ localRequestTime.format(withSecondsFormat) } - ${ localResponseTime.format(withSecondsFormat) }"
		}
		val requestPart = s"$timePart: ${ request.method } ${ request.requestUri }"
		response match {
			case Success(response) =>
				val length = response.contentLength
				val sizePart = {
					if (length == 0)
						"(empty)"
					else {
						val bytesPart = {
							if (length < 1000)
								s"$length bytes"
							else if (length < 1000000)
								s"${ length / 1000 } Kb"
							else
								s"${ length / 1000000 } Mb"
						}
						val typePart = response.contentType match {
							case Some(cType) => s" of $cType"
							case None => ""
						}
						s"$bytesPart$typePart"
					}
				}
				val paramsPart = {
					if (request.params.hasNonEmptyValues)
						s"\nRequest parameters: ${ request.params }"
					else
						""
				}
				logger(s"$requestPart => ${ response.status }; $sizePart$paramsPart\nRequest took ${
					requestDuration.description
				}")
			case Failure(error) => logger(error, s"$requestPart (${ requestDuration.description })")
		}
	}
}
