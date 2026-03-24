package utopia.echo.controller.chat

import utopia.echo.controller.client.LlmServiceClient
import utopia.echo.controller.tokenization.TokenCounter
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.request.ChatParams
import utopia.echo.model.request.openai.GetBufferedOpenAiResponse
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
 * @since 29.01.2026, v1.5
 */
@deprecated("The current version of this interface is not suitable for proper chatting. Use StatelessBufferedReplyGenerator instead.", "v1.5")
class OpenAiChat(client: LlmServiceClient, initialLlm: LlmDesignator)
                (implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger, tokenCounter: TokenCounter)
	extends AbstractChat[ReplyLike[OpenAiResponse], OpenAiResponse, OpenAiChat](client, initialLlm,
		OpenAiResponse.empty, OpenAiResponse.empty)
		with Chat
{
	override def self: OpenAiChat = this
	
	override protected def copyWithoutState: OpenAiChat = new OpenAiChat(client, llm)
	
	override protected def makeRequest(params: ChatParams, allowStreaming: Boolean) =
		GetBufferedOpenAiResponse(params)
	
	override protected def streamedReplyFrom(textPointer: Changing[String], thoughtsPointer: Changing[String],
	                                         newTextPointer: Changing[String], thinkingFlag: Flag,
	                                         lastUpdatedPointer: Changing[Instant],
	                                         resultFuture: Future[Try[OpenAiResponse]]) =
		ReplyLike.streaming(textPointer, thoughtsPointer, newTextPointer, thinkingFlag, lastUpdatedPointer,
			resultFuture)
}
