package utopia.echo.model.response.ollama

import utopia.annex.model.manifest.SchrodingerState
import utopia.annex.model.manifest.SchrodingerState.{Final, PositiveFlux}
import utopia.echo.model.response.Reply
import utopia.echo.util.ReplyParseUtils
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.TryFuture
import utopia.flow.time.Now
import utopia.flow.util.EitherExtensions._
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.OnceFlatteningPointer
import utopia.flow.view.template.eventful.Changing

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object OllamaReply
{
	// OTHER    ----------------------
	
	/**
	 * Creates a failed (final) reply
	 * @param cause Cause of failure
	 * @param exc Implicit execution context
	 * @return A failed final reply
	 */
	def failure(cause: Throwable)(implicit exc: ExecutionContext) =
		apply(Fixed(""), Fixed(""), Fixed(Now.toInstant)).futureBuffered(TryFuture.failure(cause))
	/**
	 * Creates a completed (final) reply
	 * @param result The acquired buffered response, or a failure
	 * @param exc Implicit execution context
	 * @return A completed / final reply
	 */
	def completed(result: Try[BufferedOllamaReply])(implicit exc: ExecutionContext) = result match {
		case Success(response) => response
		case Failure(error) => failure(error)
	}
	
	/**
	 * Prepares a new streamed reply
	 * @param textPointer Pointer that contains the latest version of this reply's text contents
	 * @param newTextPointer A pointer which contains the latest read reply message addition.
	 *                       Will stop changing once this reply has been fully read.
	 * @param lastUpdatedPointer Pointer that contains the origin time of the latest version of this reply
	 * @param exc Implicit execution context
	 * @return A factory for constructing the streamed reply from the final result future
	 */
	def apply(textPointer: Changing[String], newTextPointer: Changing[String], lastUpdatedPointer: Changing[Instant])
	         (implicit exc: ExecutionContext) =
		OllamaResponseFactory(textPointer, newTextPointer, lastUpdatedPointer)
	
	/**
	 * Creates a new reply message by wrapping another, once it arrives
	 * @param replyFuture Future that resolves into another reply message
	 * @param exc Implicit execution context
	 * @return A message that will contain contents of the specified reply once (if) it is received
	 */
	def async(replyFuture: Future[Try[OllamaReply]])(implicit exc: ExecutionContext) = {
		replyFuture.currentResult.map { _.flatten } match {
			// Case: Already resolved => Returns or wraps the available reply
			case Some(Success(immediateReply)) => immediateReply
			// Case: Already failed => Returns a failure reply
			case Some(Failure(cause)) => failure(cause)
			// Case: Unresolved => Forms pointers that are completed once the reply is received
			case None =>
				val textPointer = OnceFlatteningPointer("")
				val newTextPointer = OnceFlatteningPointer("")
				val lastUpdatedPointer = OnceFlatteningPointer(Now.toInstant)
				
				// Also updates the pointers here
				val statisticsFuture = replyFuture.flatMap {
					case Success(reply) =>
						textPointer.complete(reply.textPointer)
						newTextPointer.complete(reply.newTextPointer)
						lastUpdatedPointer.complete(reply.lastUpdatedPointer)
						reply.statisticsFuture
						
					case Failure(error) =>
						val alwaysEmpty = Fixed("")
						textPointer.complete(alwaysEmpty)
						newTextPointer.complete(alwaysEmpty)
						lastUpdatedPointer.complete(Fixed(Now))
						TryFuture.failure(error)
				}
				
				apply(textPointer, newTextPointer, lastUpdatedPointer).futureStatistics(statisticsFuture)
		}
	}
		
	
	// NESTED   --------------------------
	
	case class OllamaResponseFactory(textPointer: Changing[String], newTextPointer: Changing[String],
	                                 lastUpdatedPointer: Changing[Instant])
	                                (implicit exc: ExecutionContext)
	{
		def futureBuffered(future: Future[Try[BufferedOllamaReply]]): OllamaReply =
			new Streamed(textPointer, newTextPointer, lastUpdatedPointer, Right(future))
			
		def futureStatistics(future: Future[Try[OllamaResponseStatistics]]): OllamaReply =
			new Streamed(textPointer, newTextPointer, lastUpdatedPointer, Left(future))
	}
	
	private class Streamed(val textPointer: Changing[String], val newTextPointer: Changing[String],
	                       val lastUpdatedPointer: Changing[Instant],
	                       val _future: Either[Future[Try[OllamaResponseStatistics]], Future[Try[BufferedOllamaReply]]])
	                      (implicit exc: ExecutionContext)
		extends OllamaReply
	{
		// ATTRIBUTES   --------------------------
		
		override lazy val future: Future[Try[BufferedOllamaReply]] = _future.rightOrMap { statisticsFuture =>
			statisticsFuture.mapIfSuccess { statistics =>
				val (textWithoutThink, thoughts) = ReplyParseUtils.separateThinkFrom(text)
				BufferedOllamaReply(textWithoutThink, thoughts, statistics, lastUpdated)
			}
		}
		override lazy val statisticsFuture: Future[Try[OllamaResponseStatistics]] = _future.leftOrMap { replyFuture =>
			replyFuture.mapIfSuccess { _.statistics }
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
trait OllamaReply extends Reply with OllamaReplyLike[BufferedOllamaReply]
