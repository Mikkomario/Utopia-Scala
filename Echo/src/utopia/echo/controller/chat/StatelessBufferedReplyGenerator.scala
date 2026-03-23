package utopia.echo.controller.chat

import utopia.echo.model.enumeration.ReasoningEffort
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.response.BufferedReply
import utopia.echo.model.settings.{ContextSizeLimits, ModelSettings}
import utopia.echo.model.tokenization.TokenCount

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
		new _StatelessBufferedReplyGenerator[R](ModelSettings.empty, ContextSizeLimits.default, 800, 800, None,
			executor)
	
	
	// NESTED   ----------------------------
	
	private class _StatelessBufferedReplyGenerator[+R <: BufferedReply]
	(override val settings: ModelSettings, override val contextSizeLimits: ContextSizeLimits,
	 override val expectedReplySize: TokenCount, override val expectedThinkSize: TokenCount,
	 override val reasoningEffort: Option[ReasoningEffort],
	 override protected val requestExecutor: BufferingChatRequestExecutor[R])
	(implicit override val llm: LlmDesignator, override protected val exc: ExecutionContext)
		extends StatelessBufferedReplyGenerator[R]
	{
		// IMPLEMENTED  ---------------------
		
		override def withLlm(llm: LlmDesignator): StatelessBufferedReplyGenerator[R] =
			new _StatelessBufferedReplyGenerator[R](settings, contextSizeLimits, expectedReplySize, expectedThinkSize,
				reasoningEffort, requestExecutor)(llm, exc)
		
		override def withExpectedReplySize(replySize: TokenCount): StatelessBufferedReplyGenerator[R] = {
			if (expectedReplySize == replySize)
				this
			else
				copy(expectedReplySize = replySize)
		}
		override def withExpectedThinkSize(thinkSize: TokenCount): StatelessBufferedReplyGenerator[R] = {
			if (expectedThinkSize == thinkSize)
				this
			else
				copy(expectedThinkSize = thinkSize)
		}
		
		override def withSettings(settings: ModelSettings): StatelessBufferedReplyGenerator[R] =
			copy(settings = settings)
		override def withContextSizeLimits(limits: ContextSizeLimits): StatelessBufferedReplyGenerator[R] =
			copy(contextSizeLimits = limits)
		
		override def withReasoningEffort(effort: Option[ReasoningEffort]): StatelessBufferedReplyGenerator[R] =
			copy(reasoningEffort = effort)
		
		
		// OTHER    ---------------------------
		
		private def copy(settings: ModelSettings = settings, contextSizeLimits: ContextSizeLimits = contextSizeLimits,
		                 expectedReplySize: TokenCount = expectedReplySize,
		                 expectedThinkSize: TokenCount = expectedThinkSize,
		                 reasoningEffort: Option[ReasoningEffort] = reasoningEffort) =
			new _StatelessBufferedReplyGenerator[R](settings, contextSizeLimits, expectedReplySize, expectedThinkSize,
				reasoningEffort, requestExecutor)
	}
}

/**
 * Common trait for interfaces that convert prompts to buffered replies, without using a mutable state.
 * @tparam R Type of the generated replies
 * @author Mikko Hilpinen
 * @since 23.02.2026, v1.5
 */
trait StatelessBufferedReplyGenerator[+R <: BufferedReply]
	extends StatelessBufferedReplyGeneratorLike[R, StatelessBufferedReplyGenerator[R]]