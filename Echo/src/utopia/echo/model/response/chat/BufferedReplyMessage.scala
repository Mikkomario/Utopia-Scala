package utopia.echo.model.response.chat

import utopia.annex.model.manifest.SchrodingerState
import utopia.annex.model.manifest.SchrodingerState.Alive
import utopia.echo.model.ChatMessage
import utopia.echo.model.enumeration.ChatRole
import utopia.echo.model.enumeration.ChatRole.Assistant
import utopia.echo.model.response.ResponseStatistics
import utopia.flow.async.TryFuture
import utopia.flow.generic.model.immutable.Model
import utopia.flow.time.Now
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.Extender
import utopia.flow.view.template.eventful.Changing

import java.time.Instant
import scala.concurrent.Future
import scala.util.Try

object BufferedReplyMessage
{
	// OTHER    --------------------------
	
	/**
	  * Converts an Ollama API -originated model into a reply message.
	  * Expects the whole message to be present in the specified model.
	  * @param responseModel Response model to parse.
	  *                      Should include statistical information, also.
	  * @return A message read from the specified model
	  */
	def fromOllamaResponse(responseModel: Model) =
		apply(ChatMessage.parseFrom(responseModel, Assistant), ResponseStatistics.fromOllamaResponse(responseModel))
}

/**
  * A reply from an LLM which has been fully read to memory
  * @param message The reply message itself
  * @param statistics Statistics about the response-generation process
  * @param lastUpdated Origin time of the latest version of message's contents
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
case class BufferedReplyMessage(message: ChatMessage, statistics: ResponseStatistics, lastUpdated: Instant = Now)
	extends Extender[ChatMessage] with ReplyMessage
{
	override def wrapped: ChatMessage = message
	
	override def text: String = message.text
	override def textPointer: Changing[String] = Fixed(text)
	override def newTextPointer: Changing[String] = Fixed(text)
	
	override def senderRole: ChatRole = message.senderRole
	
	override def lastUpdatedPointer: Changing[Instant] = Fixed(lastUpdated)
	
	override def future: Future[Try[BufferedReplyMessage]] = TryFuture.success(this)
	override def statisticsFuture: Future[Try[ResponseStatistics]] = TryFuture.success(statistics)
	
	override def state: SchrodingerState = Alive
}
