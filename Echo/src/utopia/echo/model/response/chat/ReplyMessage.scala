package utopia.echo.model.response.chat

import utopia.echo.model.ChatMessage
import utopia.echo.model.enumeration.ChatRole
import utopia.echo.model.enumeration.ChatRole.Assistant
import utopia.echo.model.response.{OllamaResponse, OllamaResponseLike}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.TryFuture
import utopia.flow.time.Now
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.OnceFlatteningPointer

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object ReplyMessage
{
	// OTHER    --------------------------
	
	/**
	  * @param response A response received from Ollama
	  * @param exc Implicit execution context
	  * @return Specified response as a reply message
	  */
	def from(response: OllamaResponse)(implicit exc: ExecutionContext) = response match {
		// Case: Already a reply message
		case r: ReplyMessage => r
		case r =>
			// Checks whether already completed / buffered
			r.statisticsFuture.currentResult.map { _.flatten } match {
				// Case: Successfully buffered => Yields a buffered message
				case Some(Success(statistics)) => BufferedReplyMessage(ChatMessage(r.text), statistics, r.lastUpdated)
				// Case: Failed => Yields a failed streamed message
				case Some(Failure(error)) =>
					StreamedReplyMessage(Fixed(r.text), Fixed(r.text), Fixed(r.lastUpdated), TryFuture.failure(error))
				// Case: Not yet completed => Yields a streamed message
				case None =>
					StreamedReplyMessage(r.textPointer, r.newTextPointer, r.lastUpdatedPointer, r.statisticsFuture)
			}
	}
	
	/**
	  * Creates a new reply message by wrapping another, once it arrives
	  * @param replyFuture Future that resolves into another reply message
	  * @param role Role of this reply's sender
	  *             (sender is not updated from the 'replyFuture', unless that future has already completed)
	  * @param exc Implicit execution context
	  * @return A message that will contain contents of the specified reply once (if) it is received
	  */
	def async(replyFuture: Future[Try[OllamaResponse]], role: ChatRole = Assistant)
	         (implicit exc: ExecutionContext) =
	{
		replyFuture.currentResult.map { _.flatten } match {
			// Case: Already resolved => Returns or wraps the available reply
			case Some(Success(immediateReply)) => from(immediateReply)
			// Case: Already failed => Returns a failure reply
			case Some(Failure(cause)) => StreamedReplyMessage.failure(cause)
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
				
				StreamedReplyMessage(textPointer, newTextPointer, lastUpdatedPointer, statisticsFuture, role)
		}
	}
}

/**
  * Common trait for chat responses, whether buffered or streamed
  * @author Mikko Hilpinen
  * @since 21.07.2024, v1.0
  */
trait ReplyMessage extends OllamaResponse with OllamaResponseLike[BufferedReplyMessage]
{
	/**
	  * @return Role of the sender of this message
	  */
	def senderRole: ChatRole
}