package utopia.echo.controller.chat

import utopia.echo.controller.client.LlmServiceClient
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.request.ChatParams
import utopia.echo.model.request.openai.BufferedOpenAiChatRequest
import utopia.echo.model.response.ReplyLike
import utopia.echo.model.response.openai.OpenAiResponse
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger
import utopia.flow.view.template.eventful.{Changing, Flag}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
 * A chat implementation for Open AI APIs.
 * Note: This implementation is pretty much beta.
 * @param client LLM service -client utilized
 * @param initialLlm Initially used LLM
 * @author Mikko Hilpinen
 * @since 29.01.2026, v1.4.1
 */
@deprecated("The current version of this interface is not suitable for proper chatting. Use StatelessBufferedReplyGenerator instead.", "v1.4.1")
class OpenAiChat(client: LlmServiceClient, initialLlm: LlmDesignator)
                (implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger)
	extends AbstractChat[ReplyLike[OpenAiResponse], OpenAiResponse, OpenAiChat](client, initialLlm,
		OpenAiResponse.empty, OpenAiResponse.empty)
		with Chat
{
	override def self: OpenAiChat = this
	
	override protected def copyWithoutState: OpenAiChat = new OpenAiChat(client, llm)
	
	override protected def makeRequest(params: ChatParams, allowStreaming: Boolean) =
		BufferedOpenAiChatRequest(params)
	
	override protected def streamedReplyFrom(textPointer: Changing[String], thoughtsPointer: Changing[String],
	                                         newTextPointer: Changing[String], thinkingFlag: Flag,
	                                         lastUpdatedPointer: Changing[Instant],
	                                         resultFuture: Future[Try[OpenAiResponse]]) =
		ReplyLike.streaming(textPointer, thoughtsPointer, newTextPointer, thinkingFlag, lastUpdatedPointer,
			resultFuture)
}
