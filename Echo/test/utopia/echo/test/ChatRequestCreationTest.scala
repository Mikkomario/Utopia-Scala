package utopia.echo.test

import utopia.echo.controller.chat.{BufferingChatRequestExecutor, StatelessBufferedReplyGenerator}
import utopia.echo.model.ChatMessage
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.request.ChatParams
import utopia.echo.model.request.vllm.BufferedVllmChatCompletionRequest
import utopia.echo.model.response.openai.BufferedOpenAiReply
import utopia.flow.async.TryFuture
import utopia.flow.test.TestContext._

import scala.concurrent.Future
import scala.util.Try

/**
 * @author Mikko Hilpinen
 * @since 04.03.2026, v1.5
 */
object ChatRequestCreationTest extends App
{
	// ATTRIBUTES   ----------------
	
	implicit val llm: LlmDesignator = LlmDesignator("TEST", thinks = true)
	private var lastRequest = BufferedVllmChatCompletionRequest(ChatParams(ChatMessage("START")))
	private val gen = StatelessBufferedReplyGenerator(Executor).withMaxContextSize(4096)
	
	gen.bufferedReplyFor("TEST")
	gen.notThinking.bufferedReplyFor("Don't think")
	gen.withExpectedReplySize(1024).withMaxContextSize(1024).bufferedReplyFor("A test prompt with only a few tokens")
	
	gen.withMaxContextSize(65536)
		.withMinContextSize(1024).withMinContextSizeWhenThinking(2048)
		.withAdditionalContextSize(512)
		.withExpectedReplySize(800).withExpectedThinkSize(800)
		.withTemperature(0.25).bufferedReplyFor("Testing context size")
	
	
	// NESTED   --------------------
	
	private object Executor extends BufferingChatRequestExecutor[BufferedOpenAiReply]
	{
		override def apply(params: ChatParams): Future[Try[BufferedOpenAiReply]] = {
			println(params)
			val request = BufferedVllmChatCompletionRequest(params)
			lastRequest = request
			println(request.body)
			TryFuture.failure(new NotImplementedError("No response logic"))
		}
	}
}
