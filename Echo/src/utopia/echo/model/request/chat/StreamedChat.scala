package utopia.echo.model.request.chat

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.RequestResult
import utopia.echo.controller.parser.StreamedReplyMessageResponseParser
import utopia.echo.model.ChatMessage
import utopia.echo.model.response.chat.StreamedReplyMessage
import utopia.flow.collection.immutable.Empty
import utopia.flow.parse.json.JsonParser

import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

object StreamedChat
{
	// ATTRIBUTES   -------------------------
	
	/**
	  * @param exc Implicit execution context to use when parsing responses
	  * @param jsonParser Json parser used in response-parsing
	  * @return A factory for constructing streamed chat requests
	  */
	def factory(implicit exc: ExecutionContext, jsonParser: JsonParser) =
		ChatRequestFactory { (msg, history, testDeprecation) => new StreamedChat(msg, history, testDeprecation()) }
		
	
	// IMPLICIT -----------------------------
	
	implicit def objectToFactory(@unused o: StreamedChat.type)
	                            (implicit exc: ExecutionContext, jsonParser: JsonParser): ChatRequestFactory[StreamedChat] =
		factory
}

/**
  * A request where a chat message is sent to an LLM in order to acquire a reply.
  * The replies are read in streamed format, i.e. read / updated word-by-word.
  * @author Mikko Hilpinen
  * @since 21.07.2024, v1.0
  */
class StreamedChat(override val message: ChatMessage, override val conversationHistory: Seq[ChatMessage] = Empty,
                   testDeprecation: => Boolean = false)
                  (implicit exc: ExecutionContext, jsonParser: JsonParser)
	extends Chat[StreamedReplyMessage]
{
	// ATTRIBUTES   ------------------------
	
	private lazy val responseParser = new StreamedReplyMessageResponseParser().toResponse
	
	
	// IMPLEMENTED  ------------------------
	
	override def stream: Boolean = true
	
	override def deprecated: Boolean = testDeprecation
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[StreamedReplyMessage]] =
		prepared.send(responseParser)
}
