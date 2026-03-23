package utopia.echo.controller.chat

import utopia.annex.model.manifest.SchrodingerState._
import utopia.annex.model.manifest.{HasSchrodingerState, SchrodingerState}
import utopia.annex.schrodinger.Schrodinger
import utopia.echo.controller.EstimateTokenCount
import utopia.echo.model.ChatMessage
import utopia.echo.model.enumeration.ChatRole.Assistant
import utopia.echo.model.enumeration.ModelParameter.ContextTokens
import utopia.echo.model.enumeration.ReasoningEffort.SkipReasoning
import utopia.echo.model.enumeration.{ModelParameter, ReasoningEffort}
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.request.tool.Tool
import utopia.echo.model.response.ReplyLike
import utopia.echo.model.settings.{ContextSizeLimits, HasMutableContextSizeLimits, HasMutableModelSettings, ModelSettings}
import utopia.echo.model.tokenization.{PartiallyEstimatedTokenCount, TokenCount}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.operator.{Identity, ScopeUsable}
import utopia.flow.util.Mutate
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.{AsyncMirror, Fixed}
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful._
import utopia.flow.view.template.eventful.{Changing, Flag}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * An interface for interactive chat which supports conversation history and tools.
 * This version of this interface is abstract, and provides a framework for both Ollama and OpenAI API calls.
  *
  * Note: While this interface supports request-queueing and other asynchronous processes,
  *       one must be careful when manually modifying the message history and/or system messages.
  *       It is safest to do so only once the previously queued requests have completed.
 *
 * Note: Although this is a trait, it is intended to be used like an abstract class
  *
  * @author Mikko Hilpinen
  * @since 16.09.2024, v1.1
  * @tparam R Type of (streaming) responses received
 * @tparam BR Type of buffered responses received
 * @tparam Repr Implementing chat type
  */
trait ChatLike[+R <: ReplyLike[BR], +BR, +Repr]
	extends HasSchrodingerState with HasMutableModelSettings with HasMutableContextSizeLimits with ModelConvertible
		with ScopeUsable[Repr] with BufferedReplyGenerator[BR]
{
	// ABSTRACT -------------------------------
	
	/**
	 * @return Implicit logging implementation for handling pointer-related failures and for
	 *         recording chat request failures
	 */
	protected implicit def log: Logger
	/**
	 * @return Implicit execution context used in asynchronous processing
	 */
	protected implicit def exc: ExecutionContext
	
	/**
	 * A mutable pointer that contains the LLM conversed with using this chat
	 */
	def llmPointer: EventfulPointer[LlmDesignator]
	
	/**
	 * Contains the result (streaming or buffered) of the latest message push.
	 * Contains an empty placeholder until the first result has been acquired.
	 */
	def lastResultPointer: Changing[Try[R]]
	/**
	 * Pointer that contains the last reply message that was successfully received.
	 * The reply may be incomplete / streaming.
	 */
	def lastReplyPointer: Changing[R]
	/**
	 * A pointer that contains the completed last result,
	 * including information on whether that final result is still pending.
	 */
	def lastResultCompletionPointer: Changing[AsyncMirror.AsyncMirrorValue[Try[R], Try[BR]]]
	
	/**
	 * @return A copy of this chat at its current state
	 */
	def copy: Repr
	
	/**
	 * Sends a chat message to the LLM.
	 * Tool calls are automatically resolved.
	 * @param message Outgoing message
	 * @param images Images to attach to this message (base64 encoded). Default = empty.
	 * @param noStreaming Whether streaming should be disabled
	 *                    (meaning that the reply will be received as a single message)
	 * @return A Schrödinger that contains a (streamed) reply at all times and will resolve into either a
	 *         buffered reply on success, or a failure.
	 */
	def push(message: String, images: Seq[String] = Empty, extraTools: Seq[Tool] = Empty,
	         noStreaming: Boolean = false): Schrodinger[R, Try[BR]]
	
	
	// ATTRIBUTES   ---------------------------
	
	/**
	  * Number of tokens expected to appear in replies.
	 * The actual expectation depends on this value, as well as the largest received reply (in the message history).
	  */
	var expectedReplySize: TokenCount = 512
	/**
	 * Context size reserved for the thinking output by default, in cases where thinking is utilized.
	 * The actual expectation depends on this value,
	 * as well as the largest received think output (in the message history).
	 */
	var expectedThinkSize: TokenCount = 800
	
	/**
	 * A mutable pointer that contains the reasoning effort to request.
	 * May contain None, in which case the model's default behavior is applied.
	 *
	 * Note: Only applies to LLMs that support reasoning / thinking.
	 */
	val reasoningEffortPointer = Pointer.eventful.empty[ReasoningEffort]
	/**
	  * A flag that is set when the LLM is allowed to think.
	  * Note: Non-thinking LLMs will ignore this flag.
	  */
	val thinkingEnabledFlag: Flag = reasoningEffortPointer.lightMap { !_.contains(SkipReasoning) }
	/**
	  * A flag that contains true while LLM thinking is used
	  */
	val thinksFlag: Flag = llmPointer.mergeWith(thinkingEnabledFlag) { _.thinks && _ }
	
	/**
	 * A mutable pointer containing the current message history.
	 * The first value is the historical message.
	 * The second value is a (partially) estimated token count for that message.
	 */
	private val _messageHistoryPointer = EventfulPointer.emptySeq[(ChatMessage, PartiallyEstimatedTokenCount)]
	/**
	  * A mutable pointer that contains the system messages applied to the beginning of the conversation history.
	  * If this pointer is mutated, the new messages are applied to all future outbound messages,
	  * possibly even queued ones.
	  */
	val systemMessagesPointer = EventfulPointer.emptySeq[String]
	
	private val _queueSizePointer = Volatile.eventful(0)
	/**
	  * A pointer that contains the number of current outbound messages that have not received any reply yet.
	  * Once even a streaming reply is received, a message no longer counts as queued.
	  */
	val queueSizePointer = _queueSizePointer.readOnly
	
	private val lazyPendingFlag = Lazy[Flag] {
		queueSizePointer.mergeWith(lastResultCompletionPointer) { (queueSize, completion) =>
			queueSize > 0 || completion.isProcessing
		}
	}
	
	/**
	  * A mutable pointer that contains the currently applied LLM options / parameters.
	  *
	  * Note: If [[ContextTokens]] is defined here,
	  * that overrides / disables the automatic context size management -feature.
	  */
	val settingsPointer = EventfulPointer(ModelSettings.empty)
	/**
	 * @return A mutable pointer that contains the applied context size limits
	 */
	val contextSizeLimitsPointer: EventfulPointer[ContextSizeLimits] = Pointer.eventful(ContextSizeLimits.default)
	/**
	  * A mutable pointer that contains the tools that are currently made available for the LLM
	  * (besides possible request-specific tools).
	  */
	val toolsPointer = EventfulPointer.emptySeq[Tool]
	
	/**
	  * A mutable pointer that contains the number of tokens assumed to be present within the default system message
	  */
	val defaultSystemMessageTokensPointer = EventfulPointer(TokenCount.zero)
	/**
	 * Records system message tokens, if they can be fully deduced
	 */
	protected val knownSystemMessageTokensPointer = Pointer.eventful.empty[TokenCount]
	/**
	  * A pointer that contains the estimated size of the currently applied system messages.
	  * Measured in tokens.
	  */
	val systemMessageTokensPointer: Changing[PartiallyEstimatedTokenCount] = knownSystemMessageTokensPointer.flatMap {
		// Case: Size is already known => Uses the known size
		case Some(knownSize) => Fixed(PartiallyEstimatedTokenCount.confirmed(knownSize))
		// Case: Size is not known => Estimates based on message content
		case None =>
			systemMessagesPointer.flatMap { messages =>
				// Case: No system messages => Applies the value of the default system message tokens -pointer
				if (messages.isEmpty)
					defaultSystemMessageTokensPointer.map[PartiallyEstimatedTokenCount] { _.asEstimate }
				// Case: System messages defined
				//       => Estimates the token count in these messages.
				//          Updates the estimate as the estimation interface gets more accurate.
				else {
					lazy val defaultEstimate = messages.iterator.map(EstimateTokenCount.in).reduce { _ + _ }
					EstimateTokenCount.correctionModPointer
						.map { defaultEstimate.withCorrectionModifier(_): PartiallyEstimatedTokenCount }
				}
			}
	}
	/**
	 * A pointer that contains the calculated estimation of the message history's token count.
	 * Does not include system message sizes.
	 */
	val historyTokensPointer = _messageHistoryPointer.map { history =>
		if (history.isEmpty) PartiallyEstimatedTokenCount.zero else history.iterator.map { _._2 }.reduce { _ + _ }
	}
	/**
	 * A mutable pointer that records the prompt & reply size as reported by the LLM.
	 *
	 * Contains None values while this information is not available. Such is the case when:
	 *      1. This interface has just been set up
	 *      1. Message history or system message has been manually modified
	 */
	protected val knownLastPromptAndReplySizePointer = EventfulPointer(Pair(None, Some(TokenCount.zero)))
	/**
	 * A (mutable) pointer that contains:
	 *      1. The latest total prompt size, including message history & system message
	 *      1. The size of the latest received reply
	 *
	 * Both values may be estimates
	 */
	private val lastPromptAndReplySizesPointer =
		knownLastPromptAndReplySizePointer.flatMap[Pair[PartiallyEstimatedTokenCount]] { knownSizes =>
			knownSizes.findForBoth(Identity) match {
				// Case: Size is known => Wraps that value
				case Some(known) => Fixed(known.map(PartiallyEstimatedTokenCount.confirmed))
				// Case: Size is not known => Calculates it based on system message & chat history size estimations
				case None =>
					knownSizes.first match {
						// Special case: Prompt size is known while reply size is not
						//               => Only calculates the reply size based on chat history
						case Some(knownPromptSize) =>
							val promptSize = PartiallyEstimatedTokenCount.confirmed(knownPromptSize)
							_messageHistoryPointer.map { history =>
								val replySize = history.lastOption.filter { _._1.senderRole == Assistant } match {
									case Some((_, lastReplySize)) => lastReplySize
									case None => PartiallyEstimatedTokenCount.zero
								}
								Pair(promptSize, replySize)
							}
						
						// Default case: Prompt size is not known
						//               => Calculates it using system message size & chat history
						case None =>
							systemMessageTokensPointer.mergeWith(_messageHistoryPointer) { (system, history) =>
								// Applies the known reply size, if appropriate
								val replySize = knownSizes.second match {
									// Case: Last reply size already known
									case Some(known) => PartiallyEstimatedTokenCount.confirmed(known)
									// Case: Last reply size not known
									case None =>
										history.lastOption.filter { _._1.senderRole == Assistant } match {
											case Some((_, replySize)) => replySize
											case None => PartiallyEstimatedTokenCount.zero
										}
								}
								// Computes the chat history's contribution to the prompt size
								val historyPromptSize: PartiallyEstimatedTokenCount = {
									if (history.isEmpty)
										PartiallyEstimatedTokenCount.zero
									else if (history.last._1.senderRole == Assistant) {
										if (history.hasSize > 1)
											history.view.dropRight(1).iterator.map { _._2 }.reduce { _ + _ }
										else
											PartiallyEstimatedTokenCount.zero
									}
									else
										history.iterator.map { _._2 }.reduce { _ + _ }
								}
								// Combines this information
								Pair[PartiallyEstimatedTokenCount](historyPromptSize + system, replySize)
							}
					}
			}
		}
	/**
	 * A (mutable) pointer that contains the largest reply token count within the message history.
	 * May be an estimated value.
	 */
	private val _largestReplySizePointer = EventfulPointer(TokenCount.zero)
	/**
	  * A pointer that contains the largest encountered reply size during the current conversation.
	  * May contain an estimate, depending on the context (e.g. when message history has been manually modified).
	  */
	val largestReplySizePointer = _largestReplySizePointer.readOnly
	/**
	 * A mutable pointer that contains the largest think output within the message history. May be an estimate.
	 */
	private val _largestThinkSizePointer = Pointer.eventful(TokenCount.zero)
	/**
	 * A pointer that contains the largest encountered think output size during the current conversation.
	 * May contain an estimate, depending on the context.
	 */
	val largestThinkSizePointer = _largestThinkSizePointer.readOnly
	/**
	  * A pointer that contains the measured or estimated context size of the most recent completed query,
	  * i.e. the size of the system message, plus the conversation history.
	  */
	val usedContextSizePointer = lastPromptAndReplySizesPointer.map { _.merge { _ + _ } }
	
	/**
	  * A mutable pointer that contains the current message history.
	  * Messages are automatically added to this history once their replies have been fully and successfully read.
	  *
	  * Mutate this pointer with caution, since it is also modified from within this instance.
	  * It is not recommended to mutate this pointer while messaging is ongoing / has not completed.
	  */
	lazy val messageHistoryPointer = IndirectPointer(_messageHistoryPointer.map { _.map { _._1 } }) { newHistory =>
		val oldHistory = _messageHistoryPointer.value
		// Calculates the sizes of the new messages, either by copying the values from the previous versions or
		// by estimating their token counts
		val newHistoryWithSizes = newHistory.map { message =>
			oldHistory.find { _._1 == message }.getOrElse {
				message -> (EstimateTokenCount.in(message.text): PartiallyEstimatedTokenCount)
			}
		}
		_messageHistoryPointer.value = newHistoryWithSizes
		
		// Case: Message history changed
		if (oldHistory != newHistoryWithSizes) {
			// Exact context size is no longer known when the history is manually altered
			knownLastPromptAndReplySizePointer.update { knownSizes =>
				// Checks whether the reply size is still known
				val replySize = {
					lazy val preservesLastReply = newHistoryWithSizes
						.reverseIterator.find { _._1.senderRole == Assistant } match
					{
						case Some((newLastReply, _)) =>
							oldHistory.reverseIterator.find { _._1.senderRole == Assistant }
								.exists { _._1 == newLastReply }
						case None => oldHistory.forall { _._1.senderRole != Assistant }
					}
					
					// Case: Same last reply => Size is the same
					if (knownSizes.second.isDefined && preservesLastReply)
						knownSizes.second
					// Case: Message history cleared => Size is known to be 0
					else if (newHistory.isEmpty)
						Some(TokenCount.zero)
					// Case: Reply was altered => Exact size is unknown
					else
						None
				}
				Pair(None, replySize)
			}
			
			// Recalculates the largest reply size
			updateLargestReplySize {
				_.max(newHistoryWithSizes.iterator
					.filter { _._1.senderRole == Assistant }.map { _._2.corrected }.maxOption.getOrElse(TokenCount.zero))
			}
		}
	}
	
	/**
	  * A pointer that contains:
	  *     1. The context size (token count) at which the conversation history will be
	  *        automatically summarized in order to conserve space.
	  *     1. The minimum number of messages to summarize before auto-summarization may be applied.
	  *     1. The number of latest messages that will be preserved
	  *
	  * Contains None if auto-summarization is not used.
	  *
	  * During the auto-summarization process, no other messages are sent via this interface.
	  */
	val autoSummarizeAtTokensPointer = EventfulPointer.empty[(TokenCount, Int, Int)]
	/**
	 * A pointer that contains the prompt given to the LLM when requesting auto-summarization
	 */
	val summarizationPromptPointer = Pointer.eventful("Summarize our conversation so far")
	private val _summarizingFlag = ResettableFlag()
	/**
	  * A flag that contains true while this interface is forming a conversation summary.
	  * While true, no chat messages will be sent.
	  */
	val summarizingFlag = _summarizingFlag.view
	private lazy val testAutoSummaryListener = ChangeListener.continuousOnAnyChange { summarizeIfAppropriate() }
	
	/**
	  * A pointer that contains the current chat state.
	  *     - Contains Flux while messaging or summarizing is ongoing.
	  *     - Contains Alive if the last reply has been fully and successfully received.
	  *     - Contains Dead if failed to acquire a (full) reply for the latest outgoing message
	  */
	lazy val statePointer = _queueSizePointer
		.mergeWith(lastResultCompletionPointer, _summarizingFlag) { (queued, lastResultState, summarizing) =>
			lastResultState.queuedOrigin.orElse(lastResultState.activeOrigin) match {
				// Case: Processing a reply => Flux
				case Some(pending) => Flux(pending.isSuccess)
				// Case: Latest reply fully processed
				case None =>
					val lastResultWasSuccess = lastResultState.current.isSuccess
					// Case: Messages have been queued => Flux state, otherwise Final (alive or dead)
					if (summarizing || queued > 0) Flux(lastResultWasSuccess) else Final(lastResultWasSuccess)
			}
		}
	
	
	// INITIAL CODE ---------------------------
	
	// Clears the known system message size when the message is changed
	systemMessagesPointer.addAnyChangeListener { knownSystemMessageTokensPointer.clear() }
	
	// Sets up auto-summary logic once appropriate
	autoSummarizeAtTokensPointer.addContinuousListener { e =>
		// Case: Auto-summary is on
		if (e.newValue.isDefined) {
			// Case: Auto-summary was enabled => Starts tracking the summary conditions
			if (e.oldValue.isEmpty) {
				lastPromptAndReplySizesPointer.addListener(testAutoSummaryListener)
				_messageHistoryPointer.addListener(testAutoSummaryListener)
			}
			summarizeIfAppropriate()
		}
		// Case: Auto-summary was disabled => Ends tracking
		else {
			lastPromptAndReplySizesPointer.removeListener(testAutoSummaryListener)
			_messageHistoryPointer.removeListener(testAutoSummaryListener)
		}
	}
	
	
	// COMPUTED -------------------------------
	
	/**
	  * The LLM conversed with using this chat (mutable).
	  */
	implicit def llm: LlmDesignator = llmPointer.value
	def llm_=(newLlm: LlmDesignator) = llmPointer.value = newLlm
	
	/**
	 * @return Requested reasoning effort. None if model default should be used.
	 */
	def reasoningEffort = reasoningEffortPointer.value
	def reasoningEffort_=(effort: Option[ReasoningEffort]): Unit = reasoningEffortPointer.value = effort
	def reasoningEffort_=(effort: ReasoningEffort): Unit = reasoningEffortPointer.setOne(effort)
	/**
	 * @return Whether the LLM should be allowed to enter thinking mode.
	 *         Only applies to thinking LLMs.
	 */
	def thinkingEnabled = thinkingEnabledFlag.value
	/**
	 * @return Whether the LLM should be prevented from entering thinking mode.
	 *         Only affects thinking LLMs.
	 */
	def thinkingDisabled = !thinkingEnabled
	
	/**
	  * @return Currently applied system messages
	  */
	def systemMessages = systemMessagesPointer.value
	def systemMessages_=(newMessages: Seq[String]) = systemMessagesPointer.value = newMessages
	
	/**
	 * @return The number of tokens estimated to be present in the model's default system message.
	 */
	def defaultSystemMessageTokens = defaultSystemMessageTokensPointer.value
	def defaultSystemMessageTokens_=(defaultTokens: TokenCount) =
		defaultSystemMessageTokensPointer.value = defaultTokens
	
	/**
	  * @return Currently applied message history.
	  *         Note: Message history is automatically appended once requests fully and successfully complete.
	  */
	def messageHistory = messageHistoryPointer.value
	// Note: Only expected to be called from the outside
	def messageHistory_=(newHistory: Seq[ChatMessage]) = messageHistoryPointer.value = newHistory
	
	/**
	 * @return Current message history, including message size information
	 */
	protected def messageHistoryWithSizes = _messageHistoryPointer.value
	protected def messageHistoryWithSizes_=(newHistory: Seq[(ChatMessage, PartiallyEstimatedTokenCount)]) =
		_messageHistoryPointer.value = newHistory
	
	/**
	  * @return The current number of queued messages which have not yet received any reply, even a streamed one.
	  */
	def queueSize = _queueSizePointer.value
	protected def queueSize_=(newSize: Int) = _queueSizePointer.value = newSize
	
	/**
	  * @return Applied LLM options / parameters
	  */
	def options = settingsPointer.value
	def options_=(newOptions: Map[ModelParameter, Value]) = settingsPointer.value = newOptions
	
	/**
	  * @return Tools that are currently made available to the LLM for all outgoing messages
	  */
	def tools = toolsPointer.value
	def tools_=(newTools: Seq[Tool]) = toolsPointer.value = newTools
	
	/**
	  * @return Whether tools are made available for the LLM.
	  *         Note: Specifying request-specific tools may override this value.
	  */
	def usesTools = tools.nonEmpty
	
	/**
	  * @return Result of the last chat request. May contain a failure and may also still be pending (if streamed).
	  */
	def lastResult: Try[R] = lastResultPointer.value
	/**
	  * @return Result of the last fully completed chat request. May contain a failure.
	  */
	def lastCompletedResult = lastResultCompletionPointer.value.current
	/**
	  * @return Last successfully received reply message. May still be streaming.
	  *         Not necessarily fully successfully parsed, however.
	  */
	def lastReply = lastReplyPointer.value
	
	/**
	  * @return Estimated number of tokens in the system messages
	  */
	def systemMessageTokens = systemMessageTokensPointer.value
	/**
	  * @return Calculated number of tokens in the current conversation history
	  */
	def conversationHistoryTokens = historyTokensPointer.value
	/**
	  * @return Number of tokens used in the previous fully completed request.
	  *         May contain an estimate if chat history has been manually modified.
	  */
	def lastContextSize = usedContextSizePointer.value
	/**
	 * @return Largest single received within this conversation. May consist of an estimate.
	 */
	def largestReplySize = _largestReplySizePointer.value
	protected def largestReplySize_=(replySize: TokenCount) = _largestReplySizePointer.value = replySize
	/**
	 * @return The largest think output encountered during the current conversation. May be an estimate.
	 */
	def largestThinkSize = _largestThinkSizePointer.value
	protected def largestThinkSize_=(thinkSize: TokenCount) = _largestThinkSizePointer.value = thinkSize
	
	/**
	  * @return Whether this interface is currently performing a summary of the current conversation history
	  */
	def summarizing = _summarizingFlag.value
	protected def summarizing_=(summarizing: Boolean) = _summarizingFlag.value = summarizing
	
	@deprecated("Renamed to .summarizing", "v1.4")
	def isSummarizing = summarizing
	/**
	  * @return A future which resolves once this interface is no longer summarizing its conversation history.
	  *         Immediately resolved if no summarization is in process.
	  */
	def summarizingFinishedFuture = _summarizingFlag.futureWhere { !_ }
	/**
	  * @return A context size (token) threshold + minimum summarized message count + number of messages preserved,
	  *         at which summarization may be automatically performed.
	  *         None if auto-summarization is disabled.
	  */
	def autoSummarizeThresholds = autoSummarizeAtTokensPointer.value
	def autoSummarizeThresholds_=(newThreshold: Option[(TokenCount, Int, Int)]) =
		autoSummarizeAtTokensPointer.value = newThreshold
	/**
	 * @return The prompt used when requesting the LLM to summarize the current chat history
	 */
	def summarizationPrompt = summarizationPromptPointer.value
	def summarizationPrompt_=(prompt: String) = summarizationPromptPointer.value = prompt
	
	/**
	  * @return A flag that contains true while there's at least one message which has not fully resolved yet.
	  *         When false, no messages are pending or queued.
	  */
	def pendingFlag = lazyPendingFlag.value
	/**
	  * @return Whether there is an ongoing (or queued) chat message that has not yet been fully completed.
	  */
	def pending = lazyPendingFlag.current match {
		case Some(f) => f.value
		case None => queueSize > 0 || lastReply.isStreaming
	}
	/**
	  * @return Whether the messaging has completed for now (i.e. there are no queued messages nor unresolved replies)
	  */
	def messagingCompleted = !pending
	/**
	  * @return A future that resolves once all queued messages have been fully resolved
	  *         (i.e. have been sent & their replies have been fully parsed).
	  *         May be immediately resolved.
	  */
	def messagingCompletedFuture = pendingFlag.futureWhere { !_ }
	/**
	  * @return Whether this interface is currently idle,
	  *         i.e. not sending or receiving any messages, nor performing a summary.
	  */
	def idle = statePointer.value.isFlux
	/**
	  * @return A future that resolves once all messages have been fully processed, and there is no summarization
	  *         going on.
	  *         Contains Alive if the last message succeeded and Dead if it failed.
	  *         May be immediately resolved.
	  */
	def idleFuture = statePointer.findMapFuture {
		case r: Final => Some(r)
		case _ => None
	}
	
	/**
	 * Context size assigned by default in thinking mode, where the LLM produces reflective content.
	 * Context size may be increased up to [[maxContextSize]], based on other parameters, however.
	 */
	@deprecated("Deprecated for removal", "v1.5")
	def thinkingContextSize = contextSizeLimits.minWithThink
	@deprecated("Please use setMinthinkingContextSize(Int) instead", "v1.5")
	def thinkingContextSize_=(tokens: TokenCount) = setMinThinkingContextSize(tokens)
	
	@deprecated("Renamed to expectedThinkSize", "v1.5")
	def additionalThinkingContextSize = expectedThinkSize
	@deprecated("Renamed to expectedThinkSize", "v1.5")
	def additionalThinkingContextSize_=(tokens: TokenCount) = expectedThinkSize = tokens
	
	
	// IMPLEMENTED  ---------------------------
	
	/**
	 * @return Whether thinking mode is currently active.
	 *         This is based on two factors:
	 *              1. Whether the used LLM supports thinking
	 *              1. Whether thinking is enabled in this interface
	 * @see [[thinkingEnabled]]
	 */
	override def thinks = thinksFlag.value
	
	override def state: SchrodingerState = statePointer.value
	
	override def settings: ModelSettings = settingsPointer.value
	override def settings_=(newSettings: ModelSettings): Unit = settingsPointer.value = newSettings
	
	override def contextSizeLimits: ContextSizeLimits = contextSizeLimitsPointer.value
	override def contextSizeLimits_=(newLimits: ContextSizeLimits): Unit = contextSizeLimitsPointer.value = newLimits
	
	override def toModel: Model = {
		val lastPromptAndReplySize = knownLastPromptAndReplySizePointer.value
		Model.from(
			"llm" -> llm.llmName, "llmThinks" -> llm.thinks,
			"expectedReplySize" -> expectedReplySize.value, "expectedThinkSize" -> expectedThinkSize.value,
			"reasoningEffort" -> reasoningEffort,
			"systemMessages" -> systemMessagesPointer.value,
			"messageHistory" -> _messageHistoryPointer.value.map { case (message, size) =>
				message.toModel + Constant("size", size)
			},
			"lastPromptSize" -> lastPromptAndReplySize.first.map { _.value },
			"lastReplySize" -> lastPromptAndReplySize.second.map { _.value },
			"settings" -> settings, "contextSizeLimits" -> contextSizeLimits, "tools" -> tools,
			"autoSummarizeAt" -> autoSummarizeAtTokensPointer.value
				.map { case (contextSize, messageCount, preserveCount) =>
					Model.from("tokens" -> contextSize.value, "messages" -> messageCount, "preserve" -> preserveCount)
				}
		)
	}
	
	override def bufferedReplyFor(message: String): Future[Try[BR]] =
		push(message, noStreaming = true).finalResultFuture.map { _.wrapped }
	
	override def mapSettings(f: Mutate[ModelSettings]) = settingsPointer.update(f)
	override def updateContextSizeLimits(f: Mutate[ContextSizeLimits]) = contextSizeLimitsPointer.update(f)
	
	
	// OTHER    -------------------------------
	
	/**
	 * Instructs the model not to enter reasoning mode
	 */
	def disableThinking(): Unit = reasoningEffort = SkipReasoning
	
	/**
	  * Adds a new system message at the end of existing custom messages
	  * @param message System message to add
	  */
	def appendSystemMessage(message: String) = mapSystemMessages { _ :+ message }
	/**
	  * Adds new messages to the message history
	  * @param newMessages Messages to append to the current history
	  */
	def appendMessageHistory(newMessages: IterableOnce[ChatMessage]) = mapMessageHistory { _ ++ newMessages }
	
	/**
	  * Removes the message history so that the next request will start from a clear foundation
	  */
	def clearMessageHistory() = messageHistory = Empty
	/**
	  * Clears all custom system messages, so that the LLM will default to the message specified in its model file.
	  */
	def clearSystemMessages() = systemMessages = Empty
	
	/**
	  * Summarizes the conversation so far, replacing the conversation history with the summary.
	  * This function may be used to compress the message history, so that a longer (although less specific)
	  * message history may be preserved.
	  *
	  * It is recommended to call this function only once all other processes have completed.
	  *
	  * @param prompt              Prompt sent to the LLM when requesting summarization.
	 *                             Default = Current value of [[summarizationPromptPointer]].
	 * @param excludedMessageCount The number of most recent messages to exclude from this summarization process.
	  *                            Default = 0 = summarize the whole chat history.
	  * @param noStreaming         Whether reply streaming should be disabled for this query (default = false)
	  * @return Returns 2 values:
	  *             1. A Schrödinger instance representing the acquired summary reply
	  *             1. A future that resolves into the summarized history, once the message history has been altered
	  *
	  *         Returns None if a summarization is already in progress.
	  * @see [[idleFuture]]
	  */
	def summarize(prompt: => String = summarizationPromptPointer.value, excludedMessageCount: Int = 0,
	              noStreaming: Boolean = false) =
	{
		if (summarizing || excludedMessageCount >= messageHistory.size)
			None
		else {
			// Extracts latest messages in order to preserve them
			val extractedMessages = {
				if (excludedMessageCount <= 0)
					Empty
				else
					messageHistoryPointer
						.mutate { history => history.splitAt(history.size - excludedMessageCount) }
			}
			
			// Requests a summary from the LLM
			val schrodinger = push(prompt, noStreaming = noStreaming)
			_summarizingFlag.set()
			
			// Once the summary has been fully received, removes the summarized message history
			// and adds back the extracted messages
			val historyFuture = schrodinger.finalResultFuture.map { _.wrapped.map { _ =>
				messageHistoryPointer.mutate { history =>
					val (summarized, summary) = history.splitAt(history.size - 2)
					summarized -> (summary ++ extractedMessages)
				}
			} }
			historyFuture.onComplete { _ => _summarizingFlag.reset() }
			
			// Returns the message schrodinger and the future which completes after the message history has been updated
			Some(schrodinger -> historyFuture)
		}
	}
	
	/**
	  * Sets up automatic message history compressions whenever the context becomes large enough
	  * @param contextSizeThreshold Minimum context size for the summarization to occur (in tokens).
	  *                             Includes system message, as well as conversation history.
	  *                             Default = Once the next request is expected to exceed 80% of the maximum context size.
	  * @param minimumMessageCount Minimum amount of historical messages for the summarization to occur.
	  *                            Counts both requests and replies.
	  *                            Default = 4 = 2 requests + 2 replies.
	  * @param keepMessageCount Number of latest messages to exclude from the summarization.
	  *                         Includes queries & replies separately.
	  *                         For best performance, you should specify an even number.
	  *                         Default = 2 = the latest query & reply will be excluded.
	  */
	def setupAutoSummaries(contextSizeThreshold: TokenCount =
	                       (maxContextSize * 0.8) - (largestReplySize max expectedReplySize),
	                       minimumMessageCount: Int = 4, keepMessageCount: Int = 2) =
		autoSummarizeThresholds = Some((contextSizeThreshold, minimumMessageCount, keepMessageCount))
	/**
	  * Disables the auto-summary feature, if it has been enabled.
	  */
	def disableAutoSummaries() = autoSummarizeThresholds = None
	
	/**
	  * Registers a new tool for the LLM to utilize
	  * @param tool Tool made available for the LLM
	  */
	def addTool(tool: Tool) = mapTools { _.appendIfDistinct(tool) }
	/**
	  * Registers 0-n new tools for the LLM to utilize
	  * @param tools New tools for the LLM
	  */
	def addTools(tools: IterableOnce[Tool]) =
		mapTools { existing => existing ++ tools.iterator.filterNot(existing.contains) }
	/**
	  * Removes a tool from the LLM's options
	  * @param tool Tool to remove
	  */
	def removeTool(tool: Any) = mapTools { _.filterNot { _ == tool } }
	/**
	  * Removes 0-n tools from the LLM's options
	  * @param tools Tools to remove
	  */
	def removeTools(tools: IterableOnce[Any]) = {
		val set = Set.from(tools)
		if (set.nonEmpty)
			mapTools { _.filterNot(set.contains) }
	}
	
	/**
	  * Registers a new tool for the LLM to utilize
	  * @param tool Tool made available for the LLM
	  */
	def +=(tool: Tool) = addTool(tool)
	/**
	  * Removes a tool from the LLM's options
	  * @param tool Tool to remove
	  */
	def -=(tool: Tool) = removeTool(tool)
	
	/**
	  * @param f Mapping function applied to system messages
	  */
	def mapSystemMessages(f: Mutate[Seq[String]]) = systemMessagesPointer.update(f)
	/**
	  * @param f Mapping function applied to chat history
	  */
	def mapMessageHistory(f: Mutate[Seq[ChatMessage]]) = messageHistoryPointer.update(f)
	/**
	  * @param f Mapping function applied to LLM tools
	  */
	def mapTools(f: Mutate[Seq[Tool]]) = toolsPointer.update(f)
	
	/**
	 * Marks the currently used LLM as a thinking LLM (wrapping the designator, if necessary).
	 * @return The LlmDesignator now used.
	 */
	def markLlmAsThinking() = llmPointer.updateIf { !_.thinks } { _.thinking }
	
	/**
	 * Alters the queued messages count -pointer
	 * @param f A mapping function applied to the current queued messages -pointer value
	 */
	protected def updateQueueSize(f: Mutate[Int]) = _queueSizePointer.update(f)
	/**
	 * Alters the largest reply size -pointer
	 * @param f A mapping function applied to the current largest reply size -value
	 */
	protected def updateLargestReplySize(f: Mutate[TokenCount]) = _largestReplySizePointer.update(f)
	/**
	 * Alters the largest think size -pointer
	 * @param f A mapping function applied to the current largest think size -value
	 */
	protected def updateLargestThinkSize(f: Mutate[TokenCount]) = _largestThinkSizePointer.update(f)
	/**
	 * Alters the message history
	 * @param f A mapping function applied to the current message history (also controlling message sizes)
	 */
	protected def updateMessageHistory(f: Mutate[Seq[(ChatMessage, PartiallyEstimatedTokenCount)]]) =
		_messageHistoryPointer.update(f)
	
	/**
	  * Checks the auto-summary conditions and performs the summarization if those conditions are met.
	  * Won't auto-summarize while messaging or summarizing.
	  */
	private def summarizeIfAppropriate() = {
		autoSummarizeAtTokensPointer.value.foreach { case (minContext, minMessageCount, excludeCount) =>
			if (lastContextSize.corrected >= minContext &&
				messageHistory.hasSize >= (minMessageCount + excludeCount) && idle)
				summarize(excludedMessageCount = excludeCount, noStreaming = true)
		}
	}
}
