package utopia.echo.controller.chat

import utopia.annex.model.response.RequestNotSent.RequestSendingFailed
import utopia.annex.model.response.RequestResult
import utopia.annex.util.RequestResultExtensions._
import utopia.echo.model.ChatMessage
import utopia.echo.model.enumeration.ChatRole.User
import utopia.echo.model.enumeration.ReasoningEffort
import utopia.echo.model.enumeration.ReasoningEffort.SkipReasoning
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.request.ChatParams
import utopia.echo.model.response.BufferedReply
import utopia.echo.model.settings.{HasImmutableContextSizeLimits, HasImmutableModelSettings}
import utopia.echo.model.tokenization.TokenCount
import utopia.flow.collection.immutable.Empty
import utopia.flow.util.Mutate

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Common trait for interfaces that convert prompts to buffered replies, without using a mutable state.
 * @tparam R Type of the generated replies
 * @tparam Repr Implementing type
 * @author Mikko Hilpinen
 * @since 23.02.2026, v1.5
 */
trait StatelessBufferedReplyGeneratorLike[+R <: BufferedReply, +Repr]
	extends BufferedReplyGenerator[R] with HasImmutableModelSettings[Repr] with HasImmutableContextSizeLimits[Repr]
{
	// ABSTRACT --------------------------
	
	/**
	 * @return Implicit execution context used for parallel tasks
	 */
	implicit protected def exc: ExecutionContext
	
	/**
	 * @return Designator of the utilized LLM
	 */
	implicit def llm: LlmDesignator
	
	/**
	 * @return Expected number of tokens in the LLM's reply. Used for calculating the context size.
	 */
	def expectedReplySize: TokenCount
	/**
	 * @return Expected number of thinking tokens in the LLM's reply. Used for calculating the context size.
	 */
	def expectedThinkSize: TokenCount
	
	/**
	 * @return An interface which executes prepared chat requests
	 */
	protected def requestExecutor: BufferingChatRequestExecutor[R]
	
	/**
	 * @return Requested reasoning effort. None if model default should be used.
	 */
	def reasoningEffort: Option[ReasoningEffort]
	
	/**
	 * @param llm Identifier of the LLM to use
	 * @return A copy of this generator using the specified LLM
	 */
	def withLlm(llm: LlmDesignator): Repr
	/**
	 * @param replySize Expected size of the received replies
	 * @return A copy of this generator using the specified estimate when computing context size limits
	 */
	def withExpectedReplySize(replySize: TokenCount): Repr
	/**
	 * @param thinkSize Expected size of the received think outputs, where applicable
	 * @return A copy of this generator using the specified estimate when computing context size limits
	 */
	def withExpectedThinkSize(thinkSize: TokenCount): Repr
	/**
	 * @param effort Reasoning effort to request. None if model default should be applied.
	 * @return A copy of this generator requesting the specified reasoning effort.
	 */
	def withReasoningEffort(effort: Option[ReasoningEffort]): Repr
	
	
	// COMPUTED --------------------------
	
	/**
	 * @return Whether the LLM is explicitly requested to skip thinking / reflecting mode, if such feature is present
	 */
	def thinkingDisabled: Boolean = reasoningEffort.contains(SkipReasoning)
	/**
	 * @return Whether the LLM should be allowed to enter thinking / reflecting mode, if such feature is present
	 */
	def thinkingEnabled = !thinkingDisabled
	
	/**
	 * @return A copy of this interface with thinking mode explicitly disabled
	 */
	def notThinking = withReasoningEffort(SkipReasoning)
	
	
	// IMPLEMENTED  ----------------------
	
	/**
	 * @return Whether the LLM is expected to enter thinking / reflection mode when prompted through this interface
	 */
	override def thinks = llm.thinks && thinkingEnabled
	
	override def bufferedReplyFor(message: String): Future[RequestResult[R]] =
		bufferedReplyFor(message, Empty, None, None)
	
	
	// OTHER    --------------------------
	
	/**
	 * @param effort Reasoning effort to request.
	 * @return A copy of this generator requesting the specified reasoning effort.
	 */
	def withReasoningEffort(effort: ReasoningEffort): Repr = withReasoningEffort(Some(effort))
	
	/**
	 * @param f A mapping function applied to expected reply size
	 * @return Copy of this generator with mapped expected reply size
	 */
	def mapExpectedReplySize(f: Mutate[TokenCount]) = withExpectedReplySize(f(expectedReplySize))
	/**
	 * @param f A mapping function applied to expected think size
	 * @return Copy of this generator with mapped expected think size
	 */
	def mapExpectedThinkSize(f: Mutate[TokenCount]) = withExpectedThinkSize(f(expectedThinkSize))
	
	/**
	 * @param message A message / prompt to send out
	 * @param history Conversation history to apply
	 * @param expectedReplySize A custom reply size estimation for this request.
	 *                          Default = None = Applies [[expectedReplySize]].
	 * @param expectedThinkSize A custom think size estimation for this request.
	 *                          Default = None = Applies [[expectedThinkSize]].
	 * @return A future that will yield the LLM's reply, if successful
	 */
	def bufferedReplyFor(message: String, history: Seq[ChatMessage] = Empty,
	                     expectedReplySize: Option[TokenCount] = None,
	                     expectedThinkSize: Option[TokenCount] = None): Future[RequestResult[R]] =
	{
		// Modifies the settings to include context size limits
		val (appliedSettings, lazyTokenUsage) = applyLimitsTo(settings,
			history.iterator.map { _.text } ++ Iterator.single(message),
			expectedReplySize.getOrElse(this.expectedReplySize), expectedThinkSize.getOrElse(this.expectedThinkSize))
		
		appliedSettings match {
			case Success(settings) =>
				val resultFuture = requestExecutor(ChatParams(User(message), conversationHistory = history,
					settings = settings, reasoningEffort = if (llm.thinks) reasoningEffort else None))
				// Updates the token count estimator based on the acquired reply
				resultFuture.forSuccess { (reply, _, _) =>
					tokenCounter.feedback(lazyTokenUsage.value.newMessages, reply.tokenUsage.input)
					// Won't assume that reflection tokens are always included in the response
					if (!thinks || reply.thoughts.nonEmpty)
						tokenCounter.train(s"${reply.thoughts}${reply.text}", reply.tokenUsage.output)
				}
				resultFuture
			
			case Failure(error) => Future.successful(RequestSendingFailed(error))
		}
	}
}
