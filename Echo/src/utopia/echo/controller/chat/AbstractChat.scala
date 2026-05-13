package utopia.echo.controller.chat

import utopia.annex.controller.RequestQueue
import utopia.annex.model.manifest.SchrodingerState
import utopia.annex.model.manifest.SchrodingerState._
import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.RequestNotSent.RequestSendingFailed
import utopia.annex.model.response.{RequestFailure, RequestResult, Response}
import utopia.annex.schrodinger.Schrodinger
import utopia.annex.util.RequestResultExtensions._
import utopia.echo.controller.tokenization.TokenCounter
import utopia.echo.model.ChatMessage
import utopia.echo.model.enumeration.{ChatRole, ReasoningEffort}
import utopia.echo.model.enumeration.ChatRole.{Assistant, System}
import utopia.echo.model.enumeration.ReasoningEffort.SkipReasoning
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.request.ChatParams
import utopia.echo.model.request.tool.Tool
import utopia.echo.model.response.{BufferedReply, ReplyLike}
import utopia.echo.model.settings.{ContextSizeLimits, ModelSettings}
import utopia.echo.model.tokenization.{EstimatedTokenCount, PartiallyEstimatedTokenCount, TokenCount}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.TryFuture
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.generic.model.immutable.{Constant, Model}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.operator.Identity
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.Now
import utopia.flow.util.{Mutate, NotEmpty}
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.{AlwaysFalse, Fixed}
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful._
import utopia.flow.view.template.eventful.{Changing, Flag}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

/**
  * An interface for interactive chat which supports conversation history and tools.
 * This version of this interface is abstract, and provides a framework for both Ollama and OpenAI API calls.
  *
  * Note: While this interface supports request-queueing and other asynchronous processes,
  *       one must be careful when manually modifying the message history and/or system messages.
  *       It is safest to do so only once the previously queued requests have completed.
  *
  * @author Mikko Hilpinen
  * @since 16.09.2024, v1.1
  * @tparam R Type of (streaming) responses received
 * @tparam BR Type of buffered responses received
 * @tparam Repr Implementing chat type
 *
 * @param initialLlm The initially assigned LLM
 * @param requestQueue Queue for sending out API requests
 * @param emptyReply An empty reply instance. May be used as a placeholder.
 * @param emptyBufferedReply An empty buffered reply instance. May be used as a placeholder.
 * @param log Implicit logging implementation for handling pointer-related failures and for
 *            recording chat request failures
 * @param exc Implicit execution context used in asynchronous processing
 * @param jsonParser JSON parser for processing API responses
  */
abstract class AbstractChat[R <: ReplyLike[BR], BR <: BufferedReply, +Repr <: AbstractChat[R, _, _]]
(requestQueue: RequestQueue, initialLlm: LlmDesignator, emptyReply: R, emptyBufferedReply: BR)
(implicit override protected val log: Logger, override protected val exc: ExecutionContext, jsonParser: JsonParser,
 override protected val tokenCounter: TokenCounter)
	extends ChatLike[R, BR, Repr]
{
	// ATTRIBUTES   ---------------------------
	
	/**
	 * A mutable pointer that contains the LLM conversed with using this chat
	 */
	override val llmPointer: EventfulPointer[LlmDesignator] = Pointer.eventful(initialLlm)
	
	override val settingsPointer: EventfulPointer[ModelSettings] = Pointer.eventful(ModelSettings.empty)
	
	override val contextSizeLimitsPointer: EventfulPointer[ContextSizeLimits] =
		Pointer.eventful(ContextSizeLimits.default)
		
	private var _expectedReplySize: TokenCount = 512
	private var _expectedThinkSize: TokenCount = 800
	
	override val reasoningEffortPointer: EventfulPointer[Option[ReasoningEffort]] =
		Pointer.eventful.empty[ReasoningEffort]
	override val thinkingEnabledFlag: Flag = reasoningEffortPointer.lightMap { !_.contains(SkipReasoning) }
	override val thinksFlag: Flag = llmPointer.mergeWith(thinkingEnabledFlag) { _.thinks && _ }
	
	override val defaultSystemMessageTokensPointer: EventfulPointer[TokenCount] = Pointer.eventful(TokenCount.zero)
	override val systemMessagesPointer: EventfulPointer[Seq[String]] = Pointer.eventful.emptySeq
	/**
	 * Records system message tokens, if they can be fully deduced
	 */
	protected val knownSystemMessageTokensPointer = Pointer.eventful.empty[TokenCount]
	override val systemMessageTokensPointer: Changing[PartiallyEstimatedTokenCount] =
		knownSystemMessageTokensPointer.flatMap {
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
						lazy val defaultEstimate = messages.iterator.map(tokenCounter.tokensIn).reduce { _ + _ }
						tokenCounter.correctionModPointer
							.map { defaultEstimate.withCorrectionModifier(_): PartiallyEstimatedTokenCount }
					}
				}
		}
	
	override val toolsPointer: EventfulPointer[Seq[Tool]] = Pointer.eventful.emptySeq
	
	private val _queueSizePointer = Volatile.eventful(0)
	override val queueSizePointer: Changing[Int] = _queueSizePointer.readOnly
	
	/**
	 * Contains the result (streaming or buffered) of the latest message push.
	 * Contains an empty placeholder until the first result has been acquired.
	 */
	protected val _lastResultPointer = EventfulPointer[Try[R]](Success(emptyReply))
	override lazy val lastResultPointer: Changing[Try[R]] = _lastResultPointer.readOnly
	/**
	 * Pointer that contains the last reply message that was successfully received.
	 * The reply may be incomplete / streaming.
	 */
	override val lastReplyPointer = _lastResultPointer.incrementalMap { _.get } { (previous, resultEvent) =>
		resultEvent.newValue.getOrElse(previous)
	}
	/**
	 * A pointer that contains the completed last result,
	 * including information on whether that final result is still pending.
	 */
	override lazy val lastResultCompletionPointer = {
		val initialResult = lastResult.flatMap { _.future.currentResult.getOrElse { Success(emptyBufferedReply) } }
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
	override val usedContextSizePointer: Changing[PartiallyEstimatedTokenCount] =
		lastPromptAndReplySizesPointer.map { _.merge { _ + _ } }
		
	/**
	 * A (mutable) pointer that contains the largest reply token count within the message history.
	 * May be an estimated value.
	 */
	private val _largestReplySizePointer = EventfulPointer(TokenCount.zero)
	override val largestReplySizePointer: Changing[TokenCount] = _largestReplySizePointer.readOnly
	/**
	 * A mutable pointer that contains the largest think output within the message history. May be an estimate.
	 */
	private val _largestThinkSizePointer = Pointer.eventful(TokenCount.zero)
	override val largestThinkSizePointer: Changing[TokenCount] = _largestThinkSizePointer.readOnly
	
	/**
	 * A mutable pointer containing the current message history.
	 * The first value is the historical message.
	 * The second value is a (partially) estimated token count for that message.
	 */
	private val _messageHistoryPointer = EventfulPointer.emptySeq[(ChatMessage, PartiallyEstimatedTokenCount)]
	override val messageHistoryPointer: EventfulPointer[Seq[ChatMessage]] =
		IndirectPointer(_messageHistoryPointer.map { _.map { _._1 } }) { newHistory =>
			val oldHistory = _messageHistoryPointer.value
			// Calculates the sizes of the new messages, either by copying the values from the previous versions or
			// by estimating their token counts
			val newHistoryWithSizes = newHistory.map { message =>
				oldHistory.find { _._1 == message }.getOrElse {
					message -> (tokenCounter.tokensIn(message.text): PartiallyEstimatedTokenCount)
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
	override val historyTokensPointer: Changing[PartiallyEstimatedTokenCount] = _messageHistoryPointer.map { history =>
		if (history.isEmpty) PartiallyEstimatedTokenCount.zero else history.iterator.map { _._2 }.reduce { _ + _ }
	}
	
	override val autoSummarizeAtTokensPointer: EventfulPointer[Option[(TokenCount, Int, Int)]] = Pointer.eventful.empty
	override val summarizationPromptPointer: EventfulPointer[String] =
		Pointer.eventful("Summarize our conversation so far")
	private val _summarizingFlag = ResettableFlag()
	override val summarizingFlag: Flag = _summarizingFlag.view
	
	override lazy val statePointer: Changing[SchrodingerState] = _queueSizePointer
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
	
	
	// ABSTRACT -------------------------------
	
	/**
	 * @return Creates a copy of this chat.
	 *         Won't (necessarily) include any stateful information, such as chat history.
	 */
	protected def copyWithoutState: Repr
	
	/**
	 * Prepares an API request for sending a chat message
	 * @param params Parameters for creating the request
	 * @param allowStreaming Whether it shall be allowed to create a streaming request, if such is possible
	 * @return A new request for sending a message to the LLM
	 */
	protected def makeRequest(params: ChatParams, allowStreaming: Boolean): ApiRequest[R]
	
	/**
	 * Converts a set of pointers into a streamed reply instance
	 * @param textPointer Text pointer to wrap
	 * @param thoughtsPointer Thoughts-pointer to wrap
	 * @param newTextPointer New text pointer to wrap
	 * @param thinkingFlag Thinking flag to wrap
	 * @param lastUpdatedPointer A pointer that will contain the last text update time
	 * @param resultFuture A future that will yield the final buffered reply, if successful
	 * @return A new streamed reply instance
	 */
	protected def streamedReplyFrom(textPointer: Changing[String], thoughtsPointer: Changing[String],
	                                newTextPointer: Changing[String], thinkingFlag: Flag,
	                                lastUpdatedPointer: Changing[Instant], resultFuture: Future[Try[BR]]): R
	
	
	// COMPUTED -------------------------------
	
	/**
	 * @return Current message history, including message size information
	 */
	protected def messageHistoryWithSizes = _messageHistoryPointer.value
	protected def messageHistoryWithSizes_=(newHistory: Seq[(ChatMessage, PartiallyEstimatedTokenCount)]) =
		_messageHistoryPointer.value = newHistory
	
	protected def largestReplySize_=(replySize: TokenCount) = _largestReplySizePointer.value = replySize
	protected def largestThinkSize_=(thinkSize: TokenCount) = _largestThinkSizePointer.value = thinkSize
	
	protected def summarizing_=(summarizing: Boolean) = _summarizingFlag.value = summarizing
	
	
	// IMPLEMENTED  ---------------------------
	
	override def expectedReplySize: TokenCount = _expectedReplySize
	override def expectedReplySize_=(replySize: TokenCount): Unit = _expectedReplySize = replySize
	
	override def expectedThinkSize: TokenCount = _expectedThinkSize
	override def expectedThinkSize_=(thinkSize: TokenCount): Unit = _expectedThinkSize = thinkSize
	
	override def pendingFlag: Flag = lazyPendingFlag.value
	override def pending: Boolean = lazyPendingFlag.current match {
		case Some(f) => f.value
		case None => queueSize > 0 || lastReply.isStreaming
	}
	
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
	
	/**
	 * @return A copy of this chat at its current state
	 */
	def copy = {
		val copy = copyWithoutState
		
		copy.contextSizeLimits = contextSizeLimits
		copy.expectedReplySize = expectedReplySize
		copy.expectedThinkSize = expectedThinkSize
		
		copy.reasoningEffort = reasoningEffort
		copy.messageHistoryWithSizes = messageHistoryWithSizes
		copy.systemMessagesPointer.value = systemMessagesPointer.value
		copy._lastResultPointer.value = _lastResultPointer.value
		copy.settingsPointer.value = settingsPointer.value
		copy.toolsPointer.value = toolsPointer.value
		copy.defaultSystemMessageTokensPointer.value = defaultSystemMessageTokensPointer.value
		copy.knownLastPromptAndReplySizePointer.value = knownLastPromptAndReplySizePointer.value
		copy.largestReplySize = largestReplySize
		copy.largestThinkSize = largestThinkSize
		copy.autoSummarizeAtTokensPointer.value = autoSummarizeAtTokensPointer.value
		copy.summarizing = summarizing
		
		copy
	}
	
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
	override def push(message: String, images: Seq[String] = Empty, extraTools: Seq[Tool] = Empty,
	                  noStreaming: Boolean = false) =
	{
		// Prepares Schrödinger state & result pointer
		val statePointer = LockablePointer[(SchrodingerState, RequestResult[BR])](
			Flux(lastResult.isSuccess) ->
				RequestSendingFailed(new IllegalArgumentException("No response has been acquired yet")))
		
		// Prepares the text, newText & lastUpdated pointers
		// These depend on whether streaming or tools are used
		val finalReplyPromise = Promise[Try[BR]]()
		val (textPointer, thoughtsPointer, newTextPointer, thinkingFlag, lastUpdatedPointer, replyIncoming,
		replyCompleted, handleFailure) =
		{
			// Case: Tools are used or streaming is disabled => Prepares to receive only the final reply
			if (noStreaming || extraTools.nonEmpty || this.usesTools) {
				val textPointer = MutableOnce("")
				val thoughtsPointer = MutableOnce("")
				val newTextPointer = Volatile.lockable("")
				val thinkingCompletionFlag = SettableFlag()
				val lastUpdatedPointer = new MutableOnce(Now.toInstant)
				
				def replyCompleted(reply: BR) = {
					newTextPointer.value = reply.thoughts
					thoughtsPointer.value = reply.thoughts
					newTextPointer.value = ""
					thinkingCompletionFlag.set()
					
					newTextPointer.value = reply.text
					textPointer.value = reply.text
					lastUpdatedPointer.value = reply.lastUpdated
				}
				
				(textPointer, thoughtsPointer, newTextPointer, !thinkingCompletionFlag, lastUpdatedPointer, None,
					Some[BR => Unit](replyCompleted), None)
			}
			// Case: Streaming is enabled
			//       => Prepares to receive the text incrementally, but only expects to receive a single response
			else {
				val textPointer = OnceFlatteningPointer("")
				val thoughtsPointer = OnceFlatteningPointer("")
				val newTextPointer = OnceFlatteningPointer("")
				val thinkingPointer = OnceFlatteningPointer(true)
				val lastUpdatedPointer = OnceFlatteningPointer(Now.toInstant)
				
				def replyIncoming(response: Response.Success[R]) = {
					val reply = response.value
					thoughtsPointer.complete(reply.thoughtsPointer)
					thinkingPointer.complete(reply.thinkingFlag)
					newTextPointer.complete(reply.newTextPointer)
					textPointer.complete(reply.textPointer)
					lastUpdatedPointer.complete(reply.lastUpdatedPointer)
				}
				def handleFailure() = {
					val alwaysEmpty = Fixed("")
					thoughtsPointer.tryComplete(alwaysEmpty)
					thinkingPointer.tryComplete(AlwaysFalse)
					newTextPointer.tryComplete(alwaysEmpty)
					textPointer.tryComplete(alwaysEmpty)
					lastUpdatedPointer.tryComplete(Fixed(Now))
				}
				
				(textPointer, thoughtsPointer, newTextPointer, thinkingPointer: Flag, lastUpdatedPointer,
					Some[Response.Success[R] => Unit](replyIncoming), None, Some[() => Unit](handleFailure))
			}
		}
		
		// Waits until a previous summarization process has finished
		summarizingFinishedFuture.foreach { _ =>
			_push(Single(ChatMessage(message, encodedImages = images)), extraTools, allowStreaming = !noStreaming) {
				streamingReply =>
					replyIncoming.foreach { _(streamingReply) }
					statePointer.value =
						PositiveFlux -> RequestSendingFailed(
							new IllegalStateException("The reply hasn't been fully formed yet")) } {
				completedReply =>
					replyCompleted.foreach { _(completedReply) }
					finalReplyPromise.success(Success(completedReply))
					statePointer.value = Alive -> Response.Success(completedReply) } {
				failure =>
					handleFailure.foreach { _() }
					finalReplyPromise.success(failure.toFailure)
					statePointer.value = Dead -> failure
					statePointer.lock()
					log(failure.cause, "Messaging failed") }
		}
		
		// Returns a Schrödinger
		val reply = streamedReplyFrom(textPointer, thoughtsPointer, newTextPointer, thinkingFlag, lastUpdatedPointer,
			finalReplyPromise.future)
		Schrodinger.wrap(statePointer.strongMap { case (state, finalReply) => (reply, finalReply, state) })
	}
	
	override def summarize(prompt: => String, excludedMessageCount: Int,
	                       noStreaming: Boolean): Option[(Schrodinger[R, RequestResult[BR]], Future[RequestResult[Seq[ChatMessage]]])] =
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
	
	
	// OTHER    -------------------------------
	
	/**
	 * Pushes chat messages to the API. Handles tool-calls and response-processing.
	 * 'replyIncoming' is called when a streamed reply is acquired. Not called if the reply is buffered.
	 * Final call will be either 'replyCompleted' (on success) or 'handleFailure' (on failure)
	 * @param messages Messages to send out. Not empty.
	 * @param extraTools Tools to allow the LLM to use, in addition to the ones already specified in this chat instance.
	 * @param allowStreaming Whether streaming shall be allowed, if technically possible
	 * @param replyIncoming A function called when a streaming reply is first received.
	 *                      Never called if requesting in buffering mode.
	 * @param replyCompleted A function called when the final reply is received, if received successfully.
	 * @param handleFailure If failed to perform the request or acquire a response, calls this function.
	 */
	private def _push(messages: Seq[ChatMessage], extraTools: Seq[Tool], allowStreaming: Boolean)
	                 (replyIncoming: Response.Success[R] => Unit)(replyCompleted: BR => Unit)
	                 (handleFailure: RequestFailure => Unit): Unit =
	{
		// Calculates and applies the context size & max response size, unless these are user-defined
		val (appliedSettings, lazyTokenCounts) = applyLimitsTo(settings, messages.iterator.map { _.text },
			expectedReplySize, expectedThinkSize, lastContextSize)
		appliedSettings match {
			case Success(settings) =>
				updateQueueSize { _ + 1 }
				// At this time, streaming is not supported with tools
				// TODO: Implement tools support for streams
				val defaultTools = this.tools
				val tools = defaultTools ++ extraTools.view.filterNot(defaultTools.contains)
				val systemMessage = NotEmpty(systemMessages).map { _.mkString("\n\n") }.map { System(_) }
				// Sends the chat request
				val replyFuture = requestQueue.push(
					makeRequest(
						ChatParams(messages.last,
							systemMessage.emptyOrSingle ++ messageHistory ++ messages.dropRight(1), tools, settings,
							if (llm.thinks) reasoningEffort else None),
						allowStreaming))
					.future
				
				// Updates the pointers once a response is received
				replyFuture.forResult { result =>
					_lastResultPointer.value = result.toTry
					updateQueueSize { _ - 1 }
					
					result match {
						// Case: Successfully acquired a response => Processes the streamed response contents
						case response @ Response.Success(reply, _, _) =>
							if (reply.isStreaming)
								replyIncoming(response)
							// Handles the reply completion asynchronously
							// TODO: Move this part to a separate function
							reply.future.forResult {
								// Case: Reply fully parsed
								//       => Updates message history & finishes state (unless tools are called)
								case Success(reply) =>
									onReplyReceived(reply, messages, lazyTokenCounts.value.newMessages)
									NotEmpty(reply.message.toolCalls) match {
										// Case: Tool-calls => Applies the tools and forms a new request
										case Some(toolCalls) =>
											val toolMessages = toolCalls.map { call =>
												// TODO: Add OpenAI support: https://platform.openai.com/docs/guides/function-calling
												val text = tools.find { _.name ~== call.name } match {
													case Some(tool) => tool(call.args)
													case None =>
														s"\"${ call.name }\" doesn't match any of the available tools"
												}
												ChatRole.Tool(text)
											}
											// Sends the new request next (recursively)
											_push(toolMessages, extraTools, allowStreaming)(replyIncoming)(
												replyCompleted)(handleFailure)
											
										// Case: No tool-calls => Finishes
										case None => replyCompleted(reply)
									}
								// Case: Reply parsing failed => Fails
								case Failure(error) =>
									handleFailure(Response.Failure(Response.parseFailureStatus, error.getMessage))
							}
						// Case: Failed to acquire a response => Fails
						case f: RequestFailure => handleFailure(f)
					}
				}
			// Case: Suitable settings couldn't be specified (max context size exceeded) => Fails
			case Failure(error) => handleFailure(RequestSendingFailed(error))
		}
	}
	
	private def onReplyReceived(reply: BufferedReply, outgoingMessages: Seq[ChatMessage],
	                            estimatedMessageTokenCount: EstimatedTokenCount) =
	{
		// If think output was present, the reply size can't be known for certain
		val totalResponseSize = reply.tokenUsage.output
		val replySize = {
			if (reply.thoughts.nonEmpty) {
				// Checks whether to reserve more tokens for the think output next time
				// TODO: Add handling for situations where think size is known
				//  (which is the case for providers other than Ollama)
				val thinkTokens = tokenCounter.tokensIn(reply.thoughts)
				updateLargestThinkSize { _ max thinkTokens }
				updateLargestReplySize { _ max (totalResponseSize - thinkTokens) }
				
				// Also, if the LLM was not described to think, modifies it accordingly
				llmPointer.updateIf { !_.thinks } { _.thinking }
				
				// Provides feedback for the estimator
				tokenCounter.train(s"${reply.thoughts}${reply.text}", totalResponseSize)
				
				None
			}
			else {
				// Sometimes think tokens are not included in the reply (usually with Open AI)
				// => We can't assume that the output tokens consist purely of reply tokens, unless thinking is not used
				if (!thinks)
					tokenCounter.train(reply.text, totalResponseSize)
				updateLargestReplySize { _ max totalResponseSize }
				Some(totalResponseSize)
			}
		}
		
		// Updates the context size pointers and uses the known information to deduct the
		// size of the latest message
		val totalPromptSize = reply.tokenUsage.input
		val (messageSize, previousContextSize) = knownLastPromptAndReplySizePointer.mutate { previous =>
			// Current total prompt & latest reply sizes are known from the response
			val current = Pair(Some(totalPromptSize), replySize)
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
		// Provides feedback for the token estimator
		messageSize.foreach { tokenCounter.feedback(estimatedMessageTokenCount.raw, _) }
		
		appendMessageHistory(outgoingMessages, estimatedMessageTokenCount, messageSize, reply.message, replySize,
			previousContextSize)
	}
	
	private def appendMessageHistory(sentMessages: Seq[ChatMessage],
	                                 messageSizeEstimate: EstimatedTokenCount, deducedMessageSize: Option[TokenCount],
	                                 receivedReply: ChatMessage, replySize: Option[TokenCount],
	                                 previousContextSize: Option[TokenCount]) =
	{
		updateMessageHistory { history =>
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
										e.withCorrected((correctedTotal * (e.raw / totalRawEstimates)).value)
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
						val estimations = sentMessages.map { m => m -> tokenCounter.tokensIn(m.text) }
						val totalEstimation = estimations.view.map { _._2.corrected }.sum.toDouble
						estimations.map { case (m, estimate) =>
							val correctedValue = (messageSize * (estimate.corrected / totalEstimation)).value
							m -> (estimate.withCorrected(correctedValue): PartiallyEstimatedTokenCount)
						}
					}
				
				// Case: The message size could not be calculated => Estimates instead
				case None =>
					if (sentMessages.hasSize(1))
						sentMessages.map { _ -> (messageSizeEstimate: PartiallyEstimatedTokenCount) }
					else
						sentMessages.map { m => m -> (tokenCounter.tokensIn(m.text): PartiallyEstimatedTokenCount) }
			}
			
			// Appends prompt & reply
			val replySizeEstimate: PartiallyEstimatedTokenCount = replySize match {
				case Some(knownSize) => PartiallyEstimatedTokenCount.confirmed(knownSize)
				case None => tokenCounter.tokensIn(receivedReply.text)
			}
			modifiedSizeHistory ++ newPromptMessages :+ (receivedReply -> replySizeEstimate)
		}
	}
	
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
}
