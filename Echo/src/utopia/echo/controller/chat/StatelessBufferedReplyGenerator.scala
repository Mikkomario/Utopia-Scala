package utopia.echo.controller.chat

import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.response.BufferedReply
import utopia.echo.model.settings.{ContextSizeLimits, ModelSettings}

import scala.concurrent.ExecutionContext

object StatelessBufferedReplyGenerator
{
	// OTHER    ----------------------------
	
	/**
	 * Creates a new reply generator
	 * @param executor An interface for executing prepared chat requests
	 * @param llm Implicit LLM to use
	 * @param exc Implicit execution context to use
	 * @tparam R Type of the generated responses
	 * @return A new reply generator using the specified settings
	 */
	def apply[R <: BufferedReply](executor: BufferingChatRequestExecutor[R])
	                             (implicit llm: LlmDesignator, exc: ExecutionContext): StatelessBufferedReplyGenerator[R] =
		new _StatelessBufferedReplyGenerator[R](ModelSettings.empty, ContextSizeLimits.default, 800, 800, executor,
			thinkingEnabled = true)
	
	
	// NESTED   ----------------------------
	
	private class _StatelessBufferedReplyGenerator[+R <: BufferedReply]
	(override val settings: ModelSettings, override val contextSizeLimits: ContextSizeLimits,
	 override val expectedReplySize: Int, override val expectedThinkSize: Int,
	 override protected val requestExecutor: BufferingChatRequestExecutor[R],
	 override val thinkingEnabled: Boolean = true)
	(implicit override val llm: LlmDesignator, override protected val exc: ExecutionContext)
		extends StatelessBufferedReplyGenerator[R]
	{
		// IMPLEMENTED  ---------------------
		
		override def withLlm(llm: LlmDesignator): StatelessBufferedReplyGenerator[R] =
			new _StatelessBufferedReplyGenerator[R](settings, contextSizeLimits, expectedReplySize, expectedThinkSize,
				requestExecutor, thinkingEnabled)(llm, exc)
		
		override def withExpectedReplySize(replySize: Int): StatelessBufferedReplyGenerator[R] = {
			if (expectedReplySize == replySize)
				this
			else
				copy(expectedReplySize = replySize)
		}
		override def withExpectedThinkSize(thinkSize: Int): StatelessBufferedReplyGenerator[R] = {
			if (expectedThinkSize == thinkSize)
				this
			else
				copy(expectedThinkSize = thinkSize)
		}
		
		override def withSettings(settings: ModelSettings): StatelessBufferedReplyGenerator[R] =
			copy(settings = settings)
		override def withContextSizeLimits(limits: ContextSizeLimits): StatelessBufferedReplyGenerator[R] =
			copy(contextSizeLimits = limits)
		
		override def withThinkingEnabled(enabled: Boolean): StatelessBufferedReplyGenerator[R] = {
			if (thinkingEnabled == enabled)
				this
			else
				copy(thinkingEnabled = enabled)
		}
		
		
		// OTHER    ---------------------------
		
		private def copy(settings: ModelSettings = settings, contextSizeLimits: ContextSizeLimits = contextSizeLimits,
		                 expectedReplySize: Int = expectedReplySize, expectedThinkSize: Int = expectedThinkSize,
		                 thinkingEnabled: Boolean = thinkingEnabled) =
			new _StatelessBufferedReplyGenerator[R](settings, contextSizeLimits, expectedReplySize, expectedThinkSize,
				requestExecutor, thinkingEnabled)
	}
}

/**
 * Common trait for interfaces that convert prompts to buffered replies, without using a mutable state.
 * @tparam R Type of the generated replies
 * @author Mikko Hilpinen
 * @since 23.02.2026, v1.4.1
 */
trait StatelessBufferedReplyGenerator[+R <: BufferedReply]
	extends StatelessBufferedReplyGeneratorLike[R, StatelessBufferedReplyGenerator[R]]