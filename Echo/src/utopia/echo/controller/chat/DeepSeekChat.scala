package utopia.echo.controller.chat

import utopia.echo.controller.client.LlmServiceClient
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.request.ChatParams
import utopia.echo.model.request.deepseek.BufferedDeepSeekChatRequest
import utopia.echo.model.response.ReplyLike
import utopia.echo.model.response.deepseek.BufferedDeepSeekReply
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
class DeepSeekChat(client: LlmServiceClient, initialLlm: LlmDesignator)
                  (implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger)
	extends AbstractChat[ReplyLike[BufferedDeepSeekReply], BufferedDeepSeekReply, DeepSeekChat](client, initialLlm,
		BufferedDeepSeekReply.empty, BufferedDeepSeekReply.empty)
		with Chat
{
	override def self: DeepSeekChat = this
	
	override protected def copyWithoutState: DeepSeekChat = new DeepSeekChat(client, llm)
	
	override protected def makeRequest(params: ChatParams, allowStreaming: Boolean) =
		BufferedDeepSeekChatRequest(params)
	
	override protected def streamedReplyFrom(textPointer: Changing[String], thoughtsPointer: Changing[String],
	                                         newTextPointer: Changing[String], thinkingFlag: Flag,
	                                         lastUpdatedPointer: Changing[Instant],
	                                         resultFuture: Future[Try[BufferedDeepSeekReply]]) =
		ReplyLike.streaming(textPointer, thoughtsPointer, newTextPointer, thinkingFlag, lastUpdatedPointer,
			resultFuture)
}
