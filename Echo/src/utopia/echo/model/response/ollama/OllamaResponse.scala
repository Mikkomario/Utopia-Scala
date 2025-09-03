package utopia.echo.model.response.ollama

import utopia.annex.model.manifest.SchrodingerState
import utopia.annex.model.manifest.SchrodingerState.{Final, PositiveFlux}
import utopia.echo.util.ReplyParseUtils
import utopia.flow.async.AsyncExtensions._
import utopia.flow.time.Now
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object OllamaResponse
{
	// COMPUTED -----------------------
	
	/**
	 * @param exc Implicit execution context
	 * @return An empty reply (final)
	 */
	def empty(implicit exc: ExecutionContext) = success("", OllamaResponseStatistics.empty)
	
	
	// OTHER    ----------------------
	
	/**
	 * Creates a successfully streamed (final) reply
	 * @param text Reply text
	 * @param statistics Reply statistics
	 * @param lastUpdated Origin time of this reply
	 * @param exc Implicit execution context
	 * @return A successful final reply
	 */
	def success(text: String, statistics: OllamaResponseStatistics, lastUpdated: Instant = Now)
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
	def completed(statistics: Try[OllamaResponseStatistics], text: String = "", lastUpdated: Instant = Now)
	             (implicit exc: ExecutionContext) =
		apply(Fixed(text), Fixed(text), Fixed(lastUpdated), Future.successful(statistics))
	
	/**
	 * Creates a new streamed reply
	 * @param textPointer Pointer that contains the latest version of this reply's text contents
	 * @param newTextPointer A pointer which contains the latest read reply message addition.
	 *                       Will stop changing once this reply has been fully read.
	 * @param lastUpdatedPointer Pointer that contains the origin time of the latest version of this reply
	 * @param statisticsFuture A future that resolves into reply statistics or a failure once
	 *                         reply reading / parsing has completed or failed.
	 * @param exc Implicit execution context
	 * @return A new streamed reply
	 */
	def apply(textPointer: Changing[String], newTextPointer: Changing[String], lastUpdatedPointer: Changing[Instant],
	          statisticsFuture: Future[Try[OllamaResponseStatistics]])
	         (implicit exc: ExecutionContext): OllamaResponse =
		new Streamed(textPointer, newTextPointer, lastUpdatedPointer, statisticsFuture)
	
	// NESTED   --------------------------
	
	private class Streamed(val textPointer: Changing[String], val newTextPointer: Changing[String],
	                       val lastUpdatedPointer: Changing[Instant],
	                       val statisticsFuture: Future[Try[OllamaResponseStatistics]])
	                      (implicit exc: ExecutionContext)
		extends OllamaResponse
	{
		// ATTRIBUTES   --------------------------
		
		override lazy val future: Future[Try[BufferedOllamaResponse]] =
			statisticsFuture.mapIfSuccess { statistics =>
				val (textWithoutThink, thoughts) = ReplyParseUtils.separateThinkFrom(text)
				BufferedOllamaResponse(textWithoutThink, thoughts, statistics, lastUpdated)
			}
		
		
		// IMPLEMENTED ---------------------------
		
		override def isBuffered: Boolean = statisticsFuture.isCompleted
		
		override def text: String = textPointer.value
		override def lastUpdated: Instant = lastUpdatedPointer.value
		
		override def state: SchrodingerState = statisticsFuture.currentResult match {
			case Some(result) => Final(result.flatten.isSuccess)
			case None => PositiveFlux
		}
	}
}

/**
 * Common trait for Ollama's text-based responses.
 * This trait removes generic typing, being useful in situations where the specific (Repr) types are not important.
 * @author Mikko Hilpinen
 * @since 04.09.2024, v1.1
 */
trait OllamaResponse extends OllamaResponseLike[OllamaResponse]
