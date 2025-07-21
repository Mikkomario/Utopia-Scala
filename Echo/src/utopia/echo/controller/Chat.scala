package utopia.echo.controller

import utopia.annex.controller.RequestQueue
import utopia.annex.model.manifest.SchrodingerState._
import utopia.annex.model.manifest.{HasSchrodingerState, SchrodingerState}
import utopia.annex.model.response.{RequestFailure, Response}
import utopia.annex.schrodinger.Schrodinger
import utopia.annex.util.RequestResultExtensions._
import utopia.echo.controller.Chat.noThinkFlag
import utopia.echo.model.ChatMessage
import utopia.echo.model.enumeration.ChatRole.{Assistant, System}
import utopia.echo.model.enumeration.ModelParameter.{ContextTokens, PredictTokens}
import utopia.echo.model.enumeration.{ChatRole, ModelParameter}
import utopia.echo.model.llm.{HasMutableModelSettings, LlmDesignator, ModelSettings}
import utopia.echo.model.request.ollama.chat.ChatParams
import utopia.echo.model.request.ollama.chat.tool.{Tool, ToolFactory}
import utopia.echo.model.response.ollama.ResponseStatistics
import utopia.echo.model.response.ollama.chat.{BufferedReplyMessage, ReplyMessage, StreamedReplyMessage}
import utopia.echo.model.tokenization.{EstimatedTokenCount, PartiallyEstimatedTokenCount, TokenCounts}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.TryFuture
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.operator.{Identity, ScopeUsable}
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.Now
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.util.{Mutate, NotEmpty, UncertainBoolean}
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful._
import utopia.flow.view.template.eventful.{Changing, Flag}

import scala.concurrent.{ExecutionContext, Promise}
import scala.util.{Failure, Success, Try}

object Chat
{
	// ATTRIBUTES   --------------------------
	
	private val noThinkFlag = "/no_think"
	
	
	// OTHER    ------------------------------
	
	/**
	  * Parses a chat from a model, applying that model's state (system messages, message history, settings, etc.)
	  * Note: Non-critical failures are logged.
	  * @param requestQueue Request queue used by this chat interface
	  * @param model Model representing chat state
	  * @param toolFactory Factory used for defining tool functionality.
	  *                    Default = not implemented.
	  *                    Note: The default parameter throws if any tools have been defined!
	  * @param exc Implicit execution context
	  * @param log Implicit logging interface used
	  * @param jsonParser Json parser used in response-processing
	  * @return Parsed chat interface. Failure if the specified model didn't specify the targeted LLM.
	  */
	def parseFrom(requestQueue: RequestQueue, model: AnyModel, toolFactory: ToolFactory = ToolFactory.notImplemented)
	             (implicit exc: ExecutionContext, log: Logger, jsonParser: JsonParser) =
	{
		model("llm").string.toTry { new NoSuchElementException("Required parameter \"llm\" is missing") }.map { llm =>
			val chat = new Chat(requestQueue, LlmDesignator(llm, thinks = model("llmThinks").getBoolean))
			
			model("maxContextSize").int.foreach { chat.maxContextSize = _ }
			model("minContextSize").int.foreach { chat.minContextSize = _ }
			model("expectedReplySize").int.foreach { chat.expectedReplySize = _ }
			model("additionalContextSize").int.foreach { chat.additionalContextSize = _ }
			model("thinkingContextSize").int.foreach { chat.thinkingContextSize = _ }
			model("additionalThinkingContextSize").int.foreach { chat.additionalThinkingContextSize = _ }
			model("thinkingEnabled").boolean.foreach { chat.thinkingEnabled = _ }
			
			model("systemMessages").vector.filter { _.nonEmpty }.foreach { messages =>
				chat.systemMessages = messages.flatMap { _.string }
			}
			model("messageHistory").vector.filter { _.nonEmpty }.foreach { history =>
				// Parses the message history. Logs parsing failures.
				chat._messageHistoryPointer.value = history.flatMap { v =>
					v.tryModel
						.flatMap { model =>
							ChatMessage(model).flatMap { message =>
								model
									.tryGet("size") { v =>
										PartiallyEstimatedTokenCount.fromValue(v)
											.toTry { new IllegalArgumentException(s"Can't parse token count from $v") } }
									.map { message -> _ }
							}
						}
						.logWithMessage("Failed to parse a historical message")
				}
			}
			chat.knownLastPromptAndReplySizePointer.value =
				Pair(model("lastPromptSize"), model("lastReplySize")).map { _.int }
			model("settings").model.foreach { settingsModel =>
				ModelSettings(settingsModel).logWithMessage("Failed to parse chat settings")
					.foreach { chat.settings = _ }
			}
			model("tools").tryVectorWith { _.tryModel.flatMap(toolFactory.apply) }
				.logWithMessage("Failed to parse chat tools")
				.foreach { chat.tools = _ }
			model("autoSummarizeAt").model.foreach { autoSummarize =>
				chat.autoSummarizeThresholds = Some((
					autoSummarize("tokens").getInt, autoSummarize("messages").getInt, autoSummarize("preserve").getInt))
			}
			
			chat
		}
	}
}

/**
  * An interface for interactive chat which supports conversation history and tools.
  *
  * Note: While this interface supports request-queueing and other asynchronous processes,
  *       one must be careful when manually modifying the message history and/or system messages.
  *       It is safest to do so only once the previously queued requests have completed.
  *
  * @author Mikko Hilpinen
  * @since 16.09.2024, v1.1
  *
  * @constructor Initializes a new chat interface
  * @param requestQueue Queue to which chat requests will be pushed
  * @param initialLlm The initially targeted LLM
  * @param exc Implicit execution context used in asynchronous processing
  * @param log Implicit logging implementation for handling pointer-related failures and for
  *            recording chat request failures
  * @param jsonParser Json parser for interpreting Ollama's responses
  */
class Chat(requestQueue: RequestQueue, initialLlm: LlmDesignator)
          (implicit exc: ExecutionContext, log: Logger, jsonParser: JsonParser)
	extends HasSchrodingerState with HasMutableModelSettings with ModelConvertible with ScopeUsable[Chat]
{
	// ATTRIBUTES   ---------------------------
	
	/**
	  * Maximum number of tokens allowed for automatic context size management. Mutable.
	  * One token is about 1/2 a word.
	  * If the conversation context becomes larger than this value, the LLM may not apply it fully.
	  * Larger values may lead to larger (V-RAM) memory use
	  */
	var maxContextSize = 4096
	/**
	  * Smallest context size that may be requested from the LLM, in number of tokens.
	  * One token is about 1/2 a word.
	  */
	var minContextSize = 1024
	/**
	 * Context size assigned by default in thinking mode, where the LLM produces reflective content.
	 * Context size may be increased up to [[maxContextSize]], based on other parameters, however.
	 */
	var thinkingContextSize = 4096
	/**
	 * Additional context size reserved for the `<think>` response element, when thinking is utilized.
	 * May push the context size past [[thinkingContextSize]] (the default), when combined with other factors,
	 * such as a high [[expectedReplySize]].
	 *
	 * This value is automatically adjusted based on received replies, but may be set manually as well.
	 */
	var additionalThinkingContextSize = 800
	/**
	  * Number of tokens expected within a reply.
	  * Used when no appropriate statistical information has been collected yet.
	  * One token is about 1/2 a word.
	  */
	var expectedReplySize = 512
	/**
	  * Number of tokens added to the estimated required context size
	  */
	var additionalContextSize = 256
	
	/**
	  * A mutable pointer that contains the LLM conversed with using this chat
	  */
	val llmPointer = Pointer.eventful(initialLlm)
	/**
	  * A flag that is set when the LLM should be allowed to think.
	  * Note: Non-thinking LLMs will ignore this flag.
	  */
	val thinkingEnabledFlag = ResettableFlag(initialValue = true)
	/**
	  * A flag that contains true while LLM thinking is used
	  */
	lazy val thinksFlag = llmPointer.mergeWith(thinkingEnabledFlag) { _.thinks && _ }
	
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
	lazy val queueSizePointer = _queueSizePointer.readOnly
	
	private val _lastResultPointer = EventfulPointer[Try[ReplyMessage]](Success(StreamedReplyMessage.empty()))
	/**
	  * Pointer that contains the last reply message that was successfully received.
	  * The reply may be incomplete / streaming.
	  */
	val lastReplyPointer = _lastResultPointer.incrementalMap { _.get } { (previous, resultEvent) =>
		resultEvent.newValue.getOrElse(previous)
	}
	/**
	  * A pointer that contains the completed last result,
	  * including information on whether that final result is still pending.
	  */
	lazy val lastResultCompletionPointer = {
		val initialResult = lastResult.flatMap { message =>
			message.future.currentResult match {
				case Some(current) => current.flatten
				case None => Success(BufferedReplyMessage.empty)
			}
		}
		_lastResultPointer.mapToFuture(initialResult) {
			case Success(reply) => reply.future
			case Failure(error) => TryFuture.failure(error)
		}
	}
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
	  * A mutable pointer that contains the tools that are currently made available for the LLM
	  * (besides possible request-specific tools).
	  */
	val toolsPointer = EventfulPointer.emptySeq[Tool]
	
	/**
	  * A mutable pointer that contains the number of tokens assumed to be present within the default system message
	  */
	val defaultSystemMessageTokensPointer = EventfulPointer(0)
	/**
	 * Records system message tokens, if they can be fully deduced
	 */
	private val knownSystemMessageTokensPointer = Pointer.eventful.empty[Int]
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
					defaultSystemMessageTokensPointer.map { EstimatedTokenCount(_): PartiallyEstimatedTokenCount }
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
	private val knownLastPromptAndReplySizePointer = EventfulPointer(Pair(None, Some(0)))
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
							lazy val promptSize = PartiallyEstimatedTokenCount.confirmed(knownPromptSize)
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
											case Some((_, replySize)) => replySize: PartiallyEstimatedTokenCount
											case None => PartiallyEstimatedTokenCount.zero
										}
								}
								// Computes the chat history's contribution to the prompt size
								val historyPromptSize: PartiallyEstimatedTokenCount = {
									if (history.isEmpty)
										PartiallyEstimatedTokenCount.zero
									else if (history.last._1.senderRole == Assistant) {
										if (history.hasSize > 1)
											history.view.dropRight(1).map { _._2 }.reduce { _ + _ }
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
	private val _largestReplySizePointer = EventfulPointer(0)
	/**
	  * A pointer that contains the largest encountered reply size during the current conversation.
	  * May contain an estimate, depending on the context (e.g. when message history has been manually modified).
	  */
	lazy val largestReplySizePointer = _largestReplySizePointer.readOnly
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
						Some(0)
					// Case: Reply was altered => Exact size is unknown
					else
						None
				}
				Pair(None, replySize)
			}
			
			// Recalculates the largest reply size
			_largestReplySizePointer.update {
				_.max(newHistoryWithSizes.iterator
					.filter { _._1.senderRole == Assistant }.map { _._2.corrected }.maxOption.getOrElse(0))
			}
		}
	}
	
	/**
	  * A pointer that contains:
	  *     1. The context size (token count) at which the conversation history will be
	  *     automatically summarized in order to conserve space.
	  *     1. The minimum number of messages to summarize before auto-summarization may be applied.
	  *     1. The number of latest messages that will be preserved
	  *
	  * Contains None if auto-summarization is not used.
	  *
	  * During the auto-summarization process, no other messages are sent via this interface.
	  */
	val autoSummarizeAtTokensPointer = EventfulPointer.empty[(Int, Int, Int)]
	/**
	 * A pointer that contains the prompt given to the LLM when requesting auto-summarization
	 */
	val summarizationPromptPointer = Pointer.eventful("Summarize our conversation so far")
	private val _summarizingFlag = ResettableFlag()
	/**
	  * A flag that contains true while this interface is forming a conversation summary.
	  * While true, no chat messages will be sent.
	  */
	lazy val summarizingFlag = _summarizingFlag.view
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
	 * @return Whether thinking mode is currently active.
	 *         This is based on two factors:
	 *              1. Whether the used LLM supports thinking
	 *              1. Whether thinking is enabled in this interface
	 * @see [[thinkingEnabled]]
	 */
	def thinks = thinksFlag.value
	
	/**
	 * @return Whether the LLM should be allowed to enter thinking mode.
	 *         Only applies to thinking LLMs.
	 */
	def thinkingEnabled = thinkingEnabledFlag.value
	def thinkingEnabled_=(enabled: Boolean) = thinkingEnabledFlag.value = enabled
	/**
	 * @return Whether the LLM should be prevented from entering thinking mode.
	 *         Only affects thinking LLMs.
	 */
	def thinkingDisabled = !thinkingEnabled
	def thinkingDisabled_=(disabled: Boolean) = thinkingEnabled = !disabled
	
	/**
	  * @return Currently applied system messages
	  */
	def systemMessages = systemMessagesPointer.value
	def systemMessages_=(newMessages: Seq[String]) = systemMessagesPointer.value = newMessages
	
	/**
	 * @return The number of tokens estimated to be present in the model's default system message.
	 */
	def defaultSystemMessageTokens = defaultSystemMessageTokensPointer.value
	def defaultSystemMessageTokens_=(defaultTokens: Int) = defaultSystemMessageTokensPointer.value = defaultTokens
	
	/**
	  * @return Currently applied message history.
	  *         Note: Message history is automatically appended once requests fully and successfully complete.
	  */
	def messageHistory = messageHistoryPointer.value
	// Note: Only expected to be called from the outside
	def messageHistory_=(newHistory: Seq[ChatMessage]) = messageHistoryPointer.value = newHistory
	
	/**
	  * @return The current number of queued messages which have not yet received any reply, even a streamed one.
	  */
	def queueSize = _queueSizePointer.value
	
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
	def lastResult = _lastResultPointer.value
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
	
	/**
	  * @return Whether this interface is currently performing a summary of the current conversation history
	  */
	def isSummarizing = _summarizingFlag.value
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
	def autoSummarizeThresholds_=(newThreshold: Option[(Int, Int, Int)]) =
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
	 * @return A copy of this chat at its current state
	 */
	def copy = {
		val copy = new Chat(requestQueue, llm)
		
		copy.maxContextSize = maxContextSize
		copy.minContextSize = minContextSize
		copy.expectedReplySize = expectedReplySize
		copy.additionalContextSize = additionalContextSize
		copy.thinkingContextSize = thinkingContextSize
		copy.additionalThinkingContextSize = additionalThinkingContextSize
		
		copy.thinkingEnabled = thinkingEnabled
		copy._messageHistoryPointer.value = _messageHistoryPointer.value
		copy.systemMessagesPointer.value = systemMessagesPointer.value
		copy._lastResultPointer.value = _lastResultPointer.value
		copy.settingsPointer.value = settingsPointer.value
		copy.toolsPointer.value = toolsPointer.value
		copy.defaultSystemMessageTokensPointer.value = defaultSystemMessageTokensPointer.value
		copy.knownLastPromptAndReplySizePointer.value = knownLastPromptAndReplySizePointer.value
		copy._largestReplySizePointer.value = _largestReplySizePointer.value
		copy.autoSummarizeAtTokensPointer.value = autoSummarizeAtTokensPointer.value
		copy._summarizingFlag.value = _summarizingFlag.value
		
		copy
	}
	
	
	// IMPLEMENTED  ---------------------------
	
	override def self: Chat = this
	override def state: SchrodingerState = statePointer.value
	
	override def settings: ModelSettings = settingsPointer.value
	override def settings_=(newSettings: ModelSettings): Unit = settingsPointer.value = newSettings
	
	override def toModel: Model = {
		val lastPromptAndReplySize = knownLastPromptAndReplySizePointer.value
		Model.from(
			"llm" -> llm.llmName, "llmThinks" -> llm.thinks, "maxContextSize" -> maxContextSize,
			"minContextSize" -> minContextSize,
			"expectedReplySize" -> expectedReplySize, "additionalContextSize" -> additionalContextSize,
			"thinkingContextSize" -> thinkingContextSize,
			"additionalThinkingContextSize" -> additionalThinkingContextSize, "thinkingEnabled" -> thinkingEnabled,
			"systemMessages" -> systemMessagesPointer.value,
			"messageHistory" -> _messageHistoryPointer.value.map { case (message, size) =>
				message.toModel + Constant("size", size)
			},
			"lastPromptSize" -> lastPromptAndReplySize.first, "lastReplySize" -> lastPromptAndReplySize.second,
			"settings" -> settings, "tools" -> tools,
			"autoSummarizeAt" -> autoSummarizeAtTokensPointer.value
				.map { case (contextSize, messageCount, preserveCount) =>
					Model.from("tokens" -> contextSize, "messages" -> messageCount, "preserve" -> preserveCount)
				}
		)
	}
	
	override def mapSettings(f: Mutate[ModelSettings]) = settingsPointer.update(f)
	
	
	// OTHER    -------------------------------
	
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
	def push(message: String, images: Seq[String] = Empty, extraTools: Seq[Tool] = Empty, noStreaming: Boolean = false) =
	{
		// Prepares Schrödinger state & result pointer
		val statePointer = LockablePointer[(SchrodingerState, Try[BufferedReplyMessage])](
			Flux(lastResult.isSuccess) -> Failure(new IllegalArgumentException("No response has been acquired yet")))
		
		// Prepares the text, newText & lastUpdated pointers
		// These depend on whether streaming or tools are used
		val statisticsPromise = Promise[Try[ResponseStatistics]]()
		val (textPointer, newTextPointer, lastUpdatedPointer, replyIncoming, replyCompleted, handleFailure) = {
			// Case: Tools are used or streaming is disabled => Prepares to receive only the final reply
			if (extraTools.nonEmpty || this.usesTools) {
				val textPointer = new MutableOnce("")
				val lastUpdatedPointer = new MutableOnce(Now.toInstant)
				
				def replyCompleted(reply: BufferedReplyMessage) = {
					textPointer.value = reply.text
					lastUpdatedPointer.value = reply.lastUpdated
				}
				
				(textPointer, textPointer, lastUpdatedPointer,
					None, Some[BufferedReplyMessage => Unit](replyCompleted), None)
			}
			// Case: Streaming is enabled
			//       => Prepares to receive the text incrementally, but only expects to receive a single response
			else {
				val textPointer = OnceFlatteningPointer("")
				val newTextPointer = OnceFlatteningPointer("")
				val lastUpdatedPointer = OnceFlatteningPointer(Now.toInstant)
				
				def replyIncoming(reply: ReplyMessage) = {
					textPointer.complete(reply.textPointer)
					newTextPointer.complete(reply.newTextPointer)
					lastUpdatedPointer.complete(reply.lastUpdatedPointer)
				}
				def handleFailure() = {
					textPointer.tryComplete(Fixed(""))
					newTextPointer.tryComplete(Fixed(""))
					lastUpdatedPointer.tryComplete(Fixed(Now))
				}
				
				(textPointer, newTextPointer, lastUpdatedPointer,
					Some[ReplyMessage => Unit](replyIncoming), None, Some[() => Unit](handleFailure))
			}
		}
		
		// Waits until a previous summarization process has finished
		summarizingFinishedFuture.foreach { _ =>
			_push(Single(ChatMessage(message, encodedImages = images)), extraTools, allowStreaming = !noStreaming) {
				streamingReply =>
					replyIncoming.foreach { _(streamingReply) }
					statePointer.value =
						PositiveFlux -> Failure(new IllegalStateException("The reply hasn't been fully formed yet")) } {
				completedReply =>
					replyCompleted.foreach { _(completedReply) }
					statisticsPromise.success(Success(completedReply.statistics))
					statePointer.value = Alive -> Success(completedReply) } {
				error =>
					handleFailure.foreach { _() }
					statisticsPromise.success(Failure(error))
					statePointer.value = Dead -> Failure(error)
					statePointer.lock()
					log(error, "Messaging failed") }
		}
		
		// Returns a Schrödinger
		val reply = StreamedReplyMessage(textPointer, newTextPointer, lastUpdatedPointer, statisticsPromise.future)
		Schrodinger.wrap(statePointer.strongMap { case (state, finalReply) => (reply, finalReply, state) })
	}
	
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
		if (isSummarizing || excludedMessageCount >= messageHistory.size)
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
	def setupAutoSummaries(contextSizeThreshold: Int = (maxContextSize * 0.8).toInt - (largestReplySize max expectedReplySize),
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
	
	// Assumes that messages is not empty
	// 'replyIncoming' is called when a streamed reply is acquired. Not called if the reply is buffered.
	// Final call will be either 'replyCompleted' (on success) or 'handleFailure' (on failure)
	private def _push(messages: Seq[ChatMessage], extraTools: Seq[Tool], allowStreaming: Boolean)
	                 (replyIncoming: ReplyMessage => Unit)(replyCompleted: BufferedReplyMessage => Unit)
	                 (handleFailure: Throwable => Unit): Unit =
	{
		_queueSizePointer.update { _ + 1 }
		
		// Calculates and applies the context size & max response size, unless these are user-defined
		val defaultSettings = settings
		lazy val tokenCounts = countContextSize(messages.view.map { _.text }, defaultSettings.get(PredictTokens).int)
		val appliedSettings = {
			defaultSettings.get(ContextTokens).int match {
				// Case: Custom context size specified
				case Some(customContextSize) =>
					// Case: Custom max response size also specified => Won't modify settings
					if (defaultSettings.contains(PredictTokens))
						Success(defaultSettings)
					else {
						val contextSizeReduction = (tokenCounts.context - customContextSize) max 0
						// Case: The custom context size is too small to fit any response => Fails
						if (contextSizeReduction >= tokenCounts.maxResponse)
							Failure(new IllegalArgumentException(
								s"Specified context size of $customContextSize is too small to contain this request"))
						else
							Success(defaultSettings + (PredictTokens -> (tokenCounts.maxResponse - contextSizeReduction)))
					}
				case None =>
					if (tokenCounts.maxResponse > 0) {
						// Case: Custom max response size specified => Won't override it
						if (defaultSettings.contains(PredictTokens))
							Success(defaultSettings + (ContextTokens -> tokenCounts.context))
						else
							Success(defaultSettings ++ Pair[(ModelParameter, Value)](
								ContextTokens -> tokenCounts.context, PredictTokens -> tokenCounts.maxResponse))
					}
					// Case: Context size can't be increased enough to fit a response (max context size exceeded)
					//       => Fails
					else
						Failure(new IllegalStateException("Context size is not large enough to fit this request"))
			}
		}
		appliedSettings match {
			case Success(settings) =>
				// At this time, streaming is not supported with tools
				// TODO: Implement tools support for streams
				val defaultTools = this.tools
				val tools = defaultTools ++ extraTools.view.filterNot(defaultTools.contains)
				val systemMessage = NotEmpty(systemMessages).map { _.mkString("\n\n") }.map { System(_) }
				// Sends the chat request
				val replyFuture = requestQueue.push(
					ChatParams(messages.last, systemMessage.emptyOrSingle ++ messageHistory ++ messages.dropRight(1),
						tools, settings, think = if (thinkingDisabled && llm.thinks) false else UncertainBoolean)
						.toRequest(stream = allowStreaming && tools.isEmpty)).future
				
				// Updates the pointers once a response is received
				replyFuture.foreachResult { result =>
					_lastResultPointer.value = result.toTry
					_queueSizePointer.update { _ - 1 }
					
					result match {
						// Case: Successfully acquired a response => Processes the streamed response contents
						case Response.Success(reply: ReplyMessage, _, _) =>
							if (reply.isStreaming)
								replyIncoming(reply)
							// Handles the reply completion asynchronously
							reply.future.foreachResult {
								// Case: Reply fully parsed
								//       => Updates message history & finishes state (unless tools are called)
								case Success(reply) =>
									val totalPromptSize = reply.statistics.promptTokenCount
									// If a <think> block was present, the reply size can't be known for certain
									val totalResponseSize = reply.statistics.responseTokenCount
									val responseSize = {
										if (reply.thoughts.nonEmpty) {
											// Checks whether to reserve more tokens for the <think> block next time
											val thinkTokens = EstimateTokenCount.in(reply.thoughts).corrected
											if (thinkTokens > additionalThinkingContextSize)
												additionalThinkingContextSize = thinkTokens
											
											// Also, if the LLM was not described to think, modifies it accordingly
											llmPointer.updateIf { !_.thinks } { _.thinking }
											None
										}
										else
											Some(totalResponseSize)
									}
									// Updates the context size pointers and uses the known information to deduct the
									// size of the latest message
									val (messageSize, previousContextSize) = knownLastPromptAndReplySizePointer
										.mutate { previous =>
											// Current total prompt & latest reply sizes are known from the response
											val current = Pair(Some(totalPromptSize), responseSize)
											val previousContextSize = previous.second.map { totalPromptSize - _ }
											// If previous prompt & reply sizes are also known,
											// the size of the latest message may also be deducted
											val messageSize = previous.first.flatMap { previousPromptSize =>
												previous.second.map { previousReplySize =>
													totalPromptSize - previousPromptSize - previousReplySize
												}
											}
											
											(messageSize, previousContextSize) -> current
										}
									_largestReplySizePointer.update { _ max totalResponseSize }
									
									appendMessageHistory(messages, tokenCounts.newMessages, messageSize,
										reply.message, responseSize, previousContextSize)
									
									// Case: No tool-calls => Finishes
									if (reply.toolCalls.isEmpty)
										replyCompleted(reply)
									// Case: Tool-calls => Applies the tools and forms a new request
									else {
										val toolMessages = reply.toolCalls.map { call =>
											val text = tools.find { _.name ~== call.name } match {
												case Some(tool) => tool(call.args)
												case None => s"\"${ call.name }\" doesn't match any of the available tools"
											}
											ChatRole.Tool(text)
										}
										// Sends the new request next (recursively)
										_push(toolMessages, extraTools, allowStreaming)(
											replyIncoming)(replyCompleted)(handleFailure)
									}
								
								// Case: Reply parsing failed => Fails
								case Failure(error) => handleFailure(error)
							}
						
						// Case: Failed to acquire a response => Fails
						case f: RequestFailure => handleFailure(f.cause)
					}
				}
			
			// Case: Suitable settings couldn't be specified (max context size exceeded) => Fails
			case Failure(error) => handleFailure(error)
		}
	}
	
	private def appendMessageHistory(sentMessages: Seq[ChatMessage],
	                                 messageSizeEstimate: EstimatedTokenCount, deducedMessageSize: Option[Int],
	                                 receivedReply: ChatMessage, replySize: Option[Int],
	                                 previousContextSize: Option[Int]) =
	{
		// Provides feedback for the token estimator
		deducedMessageSize.foreach { EstimateTokenCount.feedback(messageSizeEstimate.raw, _) }
		replySize.foreach { EstimateTokenCount.train(receivedReply.text, _) }
		
		_messageHistoryPointer.update { history =>
			// Modifies the message history sizes or system message size to match the captured context size, if possible
			val modifiedSizeHistory = previousContextSize match {
				// Case: Previous context size deduced => Changes history or system message size to match
				case Some(previousContextSize) =>
					// Case: No message history at this time => Updates the system message size
					if (history.isEmpty) {
						knownSystemMessageTokensPointer.value = Some(previousContextSize)
						history
					}
					else {
						// Modifies the size estimates of the historical messages to match the measured context size
						val existingHistorySize = history.iterator.map { _._2 }.reduce { _ + _ }
						// Case: History size is already known with certainty
						//       => Any excessive size belongs to the system message
						if (existingHistorySize.isFullyConfirmed) {
							knownSystemMessageTokensPointer.value =
								Some(previousContextSize - existingHistorySize.confirmedPart)
							history
						}
						// Case: History size is at least partially based on estimates
						//       => Adjusts the estimates to match the specified size
						else {
							val correctedTotal = previousContextSize - existingHistorySize.confirmedPart
							val totalRawEstimates = existingHistorySize.estimatePart.raw.toDouble
							history.map { case (message, size) =>
								if (size.isFullyConfirmed)
									message -> size
								else
									message -> size.mapEstimatePart { e =>
										e.withCorrected((correctedTotal * (e.raw / totalRawEstimates)).round.toInt)
									}
							}
						}
					}
				// Case: Previous context size not known for certain => Won't change history sizes
				case None => history
			}
			
			// Stores the calculated or estimated message sizes, also
			val newPromptMessages = deducedMessageSize match {
				case Some(messageSize) =>
					// Case: Message size could be calculated
					if (sentMessages.hasSize(1))
						sentMessages.map { _ -> PartiallyEstimatedTokenCount.confirmed(messageSize) }
					// Case: No messages were present
					else if (sentMessages.isEmpty)
						Empty
					// Case: Total message size was calculated,
					//       but there were more messages than 1
					//       => Divides the calculated amount between the messages
					else {
						val estimations = sentMessages.map { m => m -> EstimateTokenCount.in(m.text) }
						val totalEstimation = estimations.view.map { _._2.corrected }.sum.toDouble
						estimations.map { case (m, estimate) =>
							val correctedValue = ((estimate.corrected / totalEstimation) * messageSize).round.toInt
							m -> (estimate.withCorrected(correctedValue): PartiallyEstimatedTokenCount)
						}
					}
					
				// Case: The message size could not be calculated => Estimates instead
				case None =>
					if (sentMessages.hasSize(1))
						sentMessages.map { _ -> (messageSizeEstimate: PartiallyEstimatedTokenCount) }
					else
						sentMessages.map { m => m -> (EstimateTokenCount.in(m.text): PartiallyEstimatedTokenCount) }
			}
			
			// Appends prompt & reply
			val replySizeEstimate: PartiallyEstimatedTokenCount = replySize match {
				case Some(knownSize) => PartiallyEstimatedTokenCount.confirmed(knownSize)
				case None => EstimateTokenCount.in(receivedReply.text)
			}
			modifiedSizeHistory ++ newPromptMessages :+ (receivedReply -> replySizeEstimate)
		}
	}
	
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
	
	private def countContextSize(nextMessages: Iterable[String], numPredict: Option[Int]) = {
		// Estimates the number of tokens in the message and in the reply
		val messageSize = {
			if (nextMessages.isEmpty)
				EstimatedTokenCount.zero
			else
				nextMessages.iterator.map(EstimateTokenCount.in).reduce { _ + _ }
		}
		val expectedReplySize = numPredict.getOrElse { this.expectedReplySize max _largestReplySizePointer.value }
		val used = usedContextSizePointer.value
		
		val total = {
			// Attempts to estimate the reply size. Limits the result to the current limits.
			val raw = used.corrected + messageSize.corrected + expectedReplySize + additionalContextSize
			val includingThink = if (thinks) raw + additionalThinkingContextSize else raw
			
			if (includingThink >= maxContextSize)
				maxContextSize
			else if (thinks && includingThink < thinkingContextSize)
				thinkingContextSize
			else if (includingThink <= minContextSize)
				minContextSize
			else
				includingThink
		}
		TokenCounts(messageSize, used, total)
	}
}
