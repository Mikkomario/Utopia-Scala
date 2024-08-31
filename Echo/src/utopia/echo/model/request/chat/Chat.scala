package utopia.echo.model.request.chat

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.annex.model.request.ApiRequest
import utopia.disciple.http.request.Body
import utopia.echo.model.ChatMessage
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger

import scala.concurrent.ExecutionContext

object Chat
{
	/**
	  * @return Factory for creating chat requests which use buffering in response-parsing
	  */
	def buffered = BufferedChat.factory
	/**
	  * @param exc Implicit execution context used in asynchronous response-parsing
	  * @param jsonParser Implicit json parser used
	  * @return Factory for creating chat requests which use streamed response-parsing
	  */
	def streamed(implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger) =
		StreamedChat.factory
}

/**
  * Common trait for chat requests, which are used for conversing with LLMs
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
// FIXME: This is missing the model parameter
// TODO: Also add support for options
// TODO: Also add support for the new tools feature
trait Chat[+R] extends ApiRequest[R]
{
	// ABSTRACT -----------------------------
	
	/**
	  * @return Message to send out
	  */
	def message: ChatMessage
	/**
	  * @return Previously sent and received messages, if conversation history / contextual memory should be kept
	  */
	def conversationHistory: Seq[ChatMessage]
	
	/**
	  * @return Whether the response should be received as a stream (if true) or all at once (if false).
	  */
	def stream: Boolean
	
	
	// IMPLEMENTED  -------------------------
	
	override def method: Method = Post
	override def path: String = "chat"
	
	override def body: Either[Value, Body] = Left(Model.from(
		"messages" -> (conversationHistory :+ message),
		"stream" -> stream
	))
}
