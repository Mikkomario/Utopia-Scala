package utopia.echo.model.response.generate

import utopia.annex.model.manifest.SchrodingerState
import utopia.annex.model.manifest.SchrodingerState.{Final, PositiveFlux}
import utopia.echo.model.response.ResponseStatistics
import utopia.flow.async.AsyncExtensions._
import utopia.flow.time.Now
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object StreamedReply
{
	// COMPUTED -----------------------
	
	/**
	  * @param exc Implicit execution context
	  * @return An empty reply (final)
	  */
	def empty(implicit exc: ExecutionContext) = success("", ResponseStatistics.empty)
	
	
	// OTHER    ----------------------
	
	/**
	  * Creates a successfully streamed (final) reply
	  * @param text Reply text
	  * @param statistics Reply statistics
	  * @param lastUpdated Origin time of this reply
	  * @param exc Implicit execution context
	  * @return A successful final reply
	  */
	def success(text: String, statistics: ResponseStatistics, lastUpdated: Instant = Now)
	           (implicit exc: ExecutionContext) =
		completed(Success(statistics), text, lastUpdated)
	/**
	  * Creates a failed (final) reply
	  * @param cause Cause of failure
	  * @param exc Implicit execution context
	  * @return A failed final reply
	  */
	def failure(cause: Throwable)(implicit exc: ExecutionContext) = completed(Failure(cause))
	/**
	  * Creates a completed (final) reply
	  * @param statistics Reply statistics. Failure if this is a failed reply.
	  * @param text Reply text
	  * @param lastUpdated Origin time of this reply
	  * @param exc Implicit execution context
	  * @return A completed / final reply
	  */
	def completed(statistics: Try[ResponseStatistics], text: String = "", lastUpdated: Instant = Now)
	             (implicit exc: ExecutionContext) =
		apply(Fixed(text), Fixed(lastUpdated), Future.successful(statistics))
	
	/**
	  * Creates a new streamed reply
	  * @param textPointer Pointer that contains the latest version of this reply's text contents
	  * @param lastUpdatedPointer Pointer that contains the origin time of the latest version of this reply
	  * @param statisticsFuture A future that resolves into reply statistics or a failure once
	  *                         reply reading / parsing has completed or failed.
	  * @param exc Implicit execution context
	  * @return A new streamed reply
	  */
	def apply(textPointer: Changing[String], lastUpdatedPointer: Changing[Instant],
	          statisticsFuture: Future[Try[ResponseStatistics]])
	         (implicit exc: ExecutionContext) =
		new StreamedReply(textPointer, lastUpdatedPointer, statisticsFuture)
}

/**
  * Represents a reply from an LLM
  * @author Mikko Hilpinen
  * @since 18.07.2024, v1.0
  * @param textPointer A pointer which contains the reply message as text.
  *                    Will stop changing once this reply has been fully generated / read.
  * @param lastUpdatedPointer A pointer which contains origin time of the latest version of text in this reply
  * @param statisticsFuture A future that resolves into statistics about this response,
  *                         once this response has been fully generated.
  *                         Will contain a failure if reply parsing failed.
  */
class StreamedReply(val textPointer: Changing[String], val lastUpdatedPointer: Changing[Instant],
                    val statisticsFuture: Future[Try[ResponseStatistics]])
                   (implicit exc: ExecutionContext)
	extends Reply
{
	// ATTRIBUTES   --------------------------
	
	override lazy val future: Future[Try[BufferedReply]] =
		statisticsFuture.mapIfSuccess { statistics => BufferedReply(text, statistics, lastUpdated) }
	
	
	// COMPUTED ------------------------------
	
	override def text: String = textPointer.value
	override def lastUpdated: Instant = lastUpdatedPointer.value
	
	override def state: SchrodingerState = statisticsFuture.currentResult match {
		case Some(result) => Final(result.flatten.isSuccess)
		case None => PositiveFlux
	}
}