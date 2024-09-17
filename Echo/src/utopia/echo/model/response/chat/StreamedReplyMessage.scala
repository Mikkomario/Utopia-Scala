package utopia.echo.model.response.chat

import utopia.annex.model.manifest.SchrodingerState
import utopia.annex.model.manifest.SchrodingerState.{Final, PositiveFlux}
import utopia.echo.model.ChatMessage
import utopia.echo.model.enumeration.ChatRole
import utopia.echo.model.enumeration.ChatRole.Assistant
import utopia.echo.model.response.{OllamaResponse, ResponseStatistics}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.time.Now
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.OnceFlatteningPointer
import utopia.flow.view.template.eventful.Changing

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object StreamedReplyMessage
{
	/**
	 * Creates an empty message that has been successfully read already
	 * @param lastUpdated Time when this message was last updated (default = now)
	 * @param role Role of the message sender (default = Assistant)
	 * @param exc Implicit execution context
	 * @return A new completed message
	 */
	def empty(lastUpdated: Instant = Now, role: ChatRole = Assistant)(implicit exc: ExecutionContext) =
		completed(Success(ResponseStatistics.empty), lastUpdated = lastUpdated, role = role)
	
	/**
	 * Creates a message that has been successfully streamed already
	 * @param text Read message text
	 * @param statistics Statistics of this message
	 * @param lastUpdated Time when this message was last updated (default = now)
	 * @param role Role of the message sender (default = Assistant)
	 * @param exc Implicit execution context
	 * @return A new completed message
	 */
	def success(text: String, statistics: ResponseStatistics, lastUpdated: Instant = Now, role: ChatRole = Assistant)
	           (implicit exc: ExecutionContext) =
		completed(Success(statistics), text, lastUpdated, role)
	
	/**
	 * Creates a message that represents a read-failure
	 * @param cause Cause of this failure
	 * @param exc Implicit execution context
	 * @return A new read-failure message
	 */
	def failure(cause: Throwable)(implicit exc: ExecutionContext) = completed(Failure(cause))
	
	/**
	 * Creates a message that has been completely streamed already
	 * @param statistics Statistics of this message. Failure if message-reading failed.
	 * @param text Read message text (default = empty)
	 * @param lastUpdated Time when this message was last updated (default = now)
	 * @param role Role of the message sender (default = Assistant)
	 * @param exc Implicit execution context
	 * @return A new completed message
	 */
	def completed(statistics: Try[ResponseStatistics], text: String = "", lastUpdated: Instant = Now,
	              role: ChatRole = Assistant)
	             (implicit exc: ExecutionContext) =
		apply(Fixed(text), Fixed(text), Fixed(lastUpdated), Future.successful(statistics), role)
	
	/**
	 * Creates a new streamed reply message
	 * @param textPointer A pointer that contains the reply text that has been received so far
	 * @param newTextPointer A pointer that contains the latest addition to the reply text
	 * @param lastUpdatedPointer A pointer that contains the last update time of this reply
	 * @param statisticsFuture A future that resolves into statistics about this reply, once reading has completed.
	 *                         Will resolve into a failure if reading or parsing fails.
	 * @param role Role of the entity that sent this reply (default = Assistant)
	 * @param exc Implicit execution context
	 * @return
	 */
	def apply(textPointer: Changing[String], newTextPointer: Changing[String], lastUpdatedPointer: Changing[Instant],
	          statisticsFuture: Future[Try[ResponseStatistics]], role: ChatRole = Assistant)
	         (implicit exc: ExecutionContext) =
		new StreamedReplyMessage(textPointer, newTextPointer, lastUpdatedPointer, statisticsFuture, role)
	
	/**
	  * @param response A response received from Ollama
	  * @param exc Implicit execution context
	  * @return Specified response converted into a streamed reply message
	  */
	def from(response: OllamaResponse)(implicit exc: ExecutionContext) = response match {
		case s: StreamedReplyMessage => s
		case r: ReplyMessage =>
			apply(r.textPointer, r.newTextPointer, r.lastUpdatedPointer, r.statisticsFuture, r.senderRole)
		case r => apply(r.textPointer, r.newTextPointer, r.lastUpdatedPointer, r.statisticsFuture)
	}
	
	/**
	  * Creates a new reply message by wrapping another, once it arrives
	  * @param replyFuture Future that resolves into another reply message
	  * @param role Role of this reply's sender
	  *             (sender is not updated from the 'replyFuture', unless that future has already completed)
	  * @param exc Implicit execution context
	  * @return A message that will contain contents of the specified reply once (if) it is received
	  */
	def async(replyFuture: Future[Try[OllamaResponse]], role: ChatRole = Assistant)(implicit exc: ExecutionContext) =
	{
		replyFuture.currentResult.map { _.flatten } match {
			// Case: Already resolved => Returns or wraps the available reply
			case Some(Success(immediateReply)) => from(immediateReply)
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
						Future.successful(Failure(error))
				}
				
				apply(textPointer, newTextPointer, lastUpdatedPointer, statisticsFuture, role)
		}
	}
}

/**
  * Represents a chat reply received from an LLM. Incrementally built based on the streamed data.
  * @param textPointer A pointer that contains the reply text that has been received so far
 * @param newTextPointer A pointer that contains the latest addition to the reply text
 * @param lastUpdatedPointer A pointer that contains the last update time of this reply
 * @param statisticsFuture A future that resolves into statistics about this reply, once reading has completed.
 *                         Will resolve into a failure if reading or parsing fails.
 * @param senderRole Role of the entity that sent this reply (default = Assistant)
 * @param exc Implicit execution context
 * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
class StreamedReplyMessage(override val textPointer: Changing[String], val newTextPointer: Changing[String],
                           override val lastUpdatedPointer: Changing[Instant],
                           override val statisticsFuture: Future[Try[ResponseStatistics]],
                           override val senderRole: ChatRole = Assistant)
                          (implicit exc: ExecutionContext)
	extends ReplyMessage
{
	// ATTRIBUTES   -----------------------
	
	override lazy val future = statisticsFuture.mapIfSuccess { stats =>
		BufferedReplyMessage(ChatMessage(text, senderRole), stats)
	}
	
	
	// COMPUTED ---------------------------
	
	override def isBuffered: Boolean = statisticsFuture.isCompleted
	
	override def text = textPointer.value
	override def lastUpdated = lastUpdatedPointer.value
	
	override def state: SchrodingerState = future.currentResult match {
		case Some(result) => Final(result.flatten.isSuccess)
		case None => PositiveFlux
	}
}
