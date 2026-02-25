package utopia.echo.controller.chat

import utopia.echo.controller.client.LlmServiceClient
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.request.ChatParams
import utopia.echo.model.request.deepseek.BufferedDeepSeekChatRequest
import utopia.echo.model.response.ReplyLike
import utopia.echo.model.response.openai.BufferedOpenAiReply
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
// TODO: Make sure the output size is not unnecessarily limited
@deprecated("The current version of this interface is not suitable for proper chatting. Use StatelessBufferedReplyGenerator instead.", "v1.5")
class DeepSeekChat(client: LlmServiceClient, initialLlm: LlmDesignator)
                  (implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger)
	extends AbstractChat[ReplyLike[BufferedOpenAiReply], BufferedOpenAiReply, DeepSeekChat](client, initialLlm,
		BufferedOpenAiReply.empty, BufferedOpenAiReply.empty)
		with Chat
{
	override def self: DeepSeekChat = this
	
	override protected def copyWithoutState: DeepSeekChat = new DeepSeekChat(client, llm)
	
	override protected def makeRequest(params: ChatParams, allowStreaming: Boolean) =
		BufferedDeepSeekChatRequest(params)
	
	override protected def streamedReplyFrom(textPointer: Changing[String], thoughtsPointer: Changing[String],
	                                         newTextPointer: Changing[String], thinkingFlag: Flag,
	                                         lastUpdatedPointer: Changing[Instant],
	                                         resultFuture: Future[Try[BufferedOpenAiReply]]) =
		ReplyLike.streaming(textPointer, thoughtsPointer, newTextPointer, thinkingFlag, lastUpdatedPointer,
			resultFuture)
}
