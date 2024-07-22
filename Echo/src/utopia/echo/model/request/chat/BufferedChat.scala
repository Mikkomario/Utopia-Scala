package utopia.echo.model.request.chat

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.RequestResult
import utopia.echo.model.ChatMessage
import utopia.echo.model.response.chat.BufferedReplyMessage
import utopia.flow.collection.immutable.Empty

import scala.annotation.unused
import scala.concurrent.Future
import scala.language.implicitConversions

object BufferedChat
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * Factory used for constructing chat requests
	  */
	lazy val factory = ChatRequestFactory { (message, history, deprecationCondition) =>
		new BufferedChat(message, history, deprecationCondition())
	}
	
	
	// IMPLICIT ----------------------------
	
	implicit def objectToFactory(@unused o: BufferedChat.type): ChatRequestFactory[BufferedChat] = factory
}

/**
  * Requests an LLM to respond to a chat message. Buffers the whole reply message to memory before returning it.
  * @author Mikko Hilpinen
  * @since 21.07.2024, v1.0
  */
class BufferedChat(override val message: ChatMessage, override val conversationHistory: Seq[ChatMessage] = Empty,
                   testDeprecation: => Boolean = false)
	extends Chat[BufferedReplyMessage]
{
	override def stream: Boolean = false
	
	override def deprecated: Boolean = testDeprecation
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[BufferedReplyMessage]] =
		prepared.mapModel(BufferedReplyMessage.fromOllamaResponse)
}
