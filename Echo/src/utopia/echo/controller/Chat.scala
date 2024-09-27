package utopia.echo.controller

import utopia.annex.model.manifest.SchrodingerState.{Alive, Dead, Final, Flux, PositiveFlux}
import utopia.annex.model.manifest.{HasSchrodingerState, SchrodingerState}
import utopia.annex.model.response.{RequestFailure, Response}
import utopia.annex.schrodinger.Schrodinger
import utopia.annex.util.RequestResultExtensions._
import utopia.echo.model.ChatMessage
import utopia.echo.model.enumeration.ChatRole.{Assistant, System}
import utopia.echo.model.enumeration.ModelParameter.{ContextTokens, PredictTokens}
import utopia.echo.model.enumeration.{ChatRole, ModelParameter}
import utopia.echo.model.llm.{HasMutableModelSettings, LlmDesignator, ModelSettings}
import utopia.echo.model.request.chat.ChatParams
import utopia.echo.model.request.chat.tool.Tool
import utopia.echo.model.response.ResponseStatistics
import utopia.echo.model.response.chat.{BufferedReplyMessage, ReplyMessage, StreamedReplyMessage}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.TryFuture
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.Now
import utopia.flow.util.EitherExtensions._
import utopia.flow.util.UncertainNumber.{CertainNumber, UncertainInt}
import utopia.flow.util.logging.Logger
import utopia.flow.util.{Mutate, NotEmpty, UncertainNumber}
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.{EventfulPointer, IndirectPointer, LockablePointer, MutableOnce, OnceFlatteningPointer, ResettableFlag}
import utopia.flow.view.template.eventful.Flag

import scala.concurrent.{ExecutionContext, Promise}
import scala.util.{Failure, Success, Try}

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
  * @param ollama Ollama client utilized by this interface
  * @param initialLlm The initially targeted LLM
  * @param exc Implicit execution context used in asynchronous processing
  * @param log Implicit logging implementation for handling pointer-related failures and for
  *            recording chat request failures
  * @param jsonParser Json parser for interpreting Ollama's responses
  */
class Chat(ollama: OllamaClient, initialLlm: LlmDesignator)
          (implicit exc: ExecutionContext, log: Logger, jsonParser: JsonParser)
	extends HasSchrodingerState with HasMutableModelSettings
{
	// ATTRIBUTES   ---------------------------
	
	/**
	  * The LLM conversed with using this chat (mutable).
	  */
	implicit var llm: LlmDesignator = initialLlm
	
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
	var minContextSize = 512
	/**
	  * Number of tokens expected within a reply.
	  * Used when no appropriate statistical information has been collected yet.
	  * One token is about 1/2 a word.
	  */
	var expectedReplySize = 384
	/**
	  * Number of tokens added to the estimated required context size
	  */
	var additionalContextSize = 256
	
	// First value is the message, second value is its estimated size in tokens
	private val _messageHistoryPointer = EventfulPointer.emptySeq[(ChatMessage, Int)]
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
	val defaultSystemMessageTokensPointer = EventfulPointer(256)
	/**
	  * A pointer that contains the estimated size of the currently applied system messages.
	  * Measured in tokens.
	  */
	val systemMessageTokensPointer = systemMessagesPointer
		.mergeWith(defaultSystemMessageTokensPointer) { (messages, defaultSize) =>
			if (messages.isEmpty)
				defaultSize
			else
				messages.view.map(EstimateTokenCount.in).sum
		}
	
	private val lastPromptAndReplySizesPointer = EventfulPointer(Pair[UncertainInt](UncertainNumber.zeroOrMore, 0))
	private val _largestReplySizePointer = EventfulPointer[UncertainInt](0)
	/**
	  * A pointer that contains the largest encountered reply size during the current conversation.
	  * May contain inexact values, depending on the context (e.g. whether message history has been manually modified).
	  */
	lazy val largestReplySizePointer = _largestReplySizePointer.readOnly
	private val _historyTokensPointer = EventfulPointer(0)
	/**
	  * A pointer that contains the calculated estimation of the message history's token count.
	  * Does not include system message sizes.
	  */
	val historyTokensPointer = _messageHistoryPointer.map { _.view.map { _._2 }.sum }
	/**
	  * A pointer that contains the measured or estimated context size of the most recent completed query,
	  * i.e. the size of the system message, plus the conversation history.
	  */
	val usedContextSizePointer = lastPromptAndReplySizesPointer
		.mergeWith(systemMessageTokensPointer, historyTokensPointer) { (lastStatus, system, history) =>
			// If a measurement is available, uses that
			val measured = lastStatus.merge { _ + _ }
			measured.exact.getOrElse {
				// Otherwise uses an estimate
				val estimate = system + history
				measured.smallestPossibleValue match {
					case Some(minimum) => estimate max minimum
					case None => estimate
				}
			}
		}
	
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
			oldHistory.find { _._1 == message }.getOrElse { message -> EstimateTokenCount.in(message.text) }
		}
		_messageHistoryPointer.value = newHistoryWithSizes
		
		if (oldHistory != newHistoryWithSizes) {
			// Exact context size is no longer known when the history is manually altered
			lastPromptAndReplySizesPointer.update { contextSize =>
				// Checks whether the reply size is still known
				val replySize: UncertainInt = {
					// Case: Same last reply => Size is the same
					if (newHistoryWithSizes.lastOption.exists(oldHistory.lastOption.contains))
						contextSize.second
					// Case: Message history cleared => Size is known to be 0
					else if (newHistory.isEmpty)
						CertainNumber(0)
					// Case: Reply was altered => Exact size is unknown
					else
						UncertainNumber.positive
				}
				Pair(UncertainNumber.zeroOrMore, replySize)
			}
			
			// Checks whether the message history was appended
			val isAppend = {
				val parallelHistory = oldHistory.zip(newHistoryWithSizes)
				parallelHistory.hasSize.of(oldHistory) &&
					parallelHistory.forall { case (o, n) => o == n }
			}
			
			// Recalculates the largest reply size
			_largestReplySizePointer.update { previous =>
				lazy val estimated = newHistoryWithSizes.view
					.filter { _._1.senderRole == Assistant }
					.map { _._2 }
					.maxOption match
				{
					case Some(estimated) => UncertainNumber.greaterThan((estimated * 0.8).toInt, orEqual = true)
					case None => CertainNumber(0)
				}
				// The largest increase minimum value may be preserved, if the history is only appended
				previous.smallestPossibleValue match {
					case Some(smallest) =>
						// Case: Message history was appended
						//       => Keeps the current largest value,
						//          taking into consideration that the actual reply may be larger
						if (isAppend)
							UncertainNumber.greaterThan(smallest, orEqual = true)
						// Case: Message history was modified more => Calculates a new value
						else
							estimated
							
					case None => estimated
				}
			}
		}
	}
	
	/**
	  * A pointer that contains:
	  *     1. The context size (token count) at which the conversation history will be
	  *     automatically summarized in order to conserve space.
	  *     1. The minimum number of messages in the message history before auto-summarization may be applied.
	  *
	  * Contains None if auto-summarization is not used.
	  *
	  * During the auto-summarization process, no other messages are sent via this interface.
	  */
	val autoSummarizeAtTokensPointer = EventfulPointer.empty[Pair[Int]]
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
	  * @return Currently applied system messages
	  */
	def systemMessages = systemMessagesPointer.value
	def systemMessages_=(newMessages: Seq[String]) = systemMessagesPointer.value = newMessages
	
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
	def conversationHistoryTokens = _historyTokensPointer.value
	/**
	  * @return Number of tokens used in the previous fully completed request.
	  *         Contains an estimate if now requests have yet been completed within this conversation.
	  */
	def lastContextSize = usedContextSizePointer.value
	/**
	  * @return Largest single received within this conversation.
	  *         Uncertain when the message history has just been manually altered.
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
	  * @return A context size (token) threshold + minimum message history size (in number of messages),
	  *         at which summarization may be automatically performed.
	  *         None if auto-summarization is disabled.
	  */
	def autoSummarizeThresholds = autoSummarizeAtTokensPointer.value
	def autoSummarizeThresholds_=(newThreshold: Option[Pair[Int]]) =
		autoSummarizeAtTokensPointer.value = newThreshold
	
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
	
	
	// IMPLEMENTED  ---------------------------
	
	override def state: SchrodingerState = statePointer.value
	
	override def settings: ModelSettings = settingsPointer.value
	override def settings_=(newSettings: ModelSettings): Unit = settingsPointer.value = newSettings
	
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
	  * @param noStreaming Whether reply streaming should be disabled for this query (default = false)
	  * @return Returns 2 values:
	  *             1. A Schrödinger instance representing the acquired summary reply
	  *             1. A future that resolves into the summarized history, once the message history has been altered
	  *
	  *         Returns None if a summarization is already in progress.
	  *
	  * @see [[idleFuture]]
	  */
	def summarize(noStreaming: Boolean = false) = {
		if (isSummarizing)
			None
		else {
			// Requests a summary from the LLM
			val schrodinger = push("Summarize our conversation so far", noStreaming = noStreaming)
			_summarizingFlag.set()
			
			// Once the summary has been fully received, removes the summarized message history
			val historyFuture = schrodinger.finalResultFuture.map { _.wrapped.foreach { _ =>
				messageHistoryPointer.mutate { history => history.splitAt(history.size - 2) }
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
	  *                            Default = 6 = 3 requests + 3 replies.
	  */
	def setupAutoSummaries(contextSizeThreshold: Int = (maxContextSize * 0.8).toInt - (largestReplySize.smallestPossibleValue.getOrElse(0) max expectedReplySize),
	                       minimumMessageCount: Int = 4) =
		autoSummarizeThresholds = Some(Pair(contextSizeThreshold, minimumMessageCount))
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
	
	// Assumes that messages is not empty
	// 'replyIncoming' is called when a streamed reply is acquired. Not called if the reply is buffered.
	// Final call will be either 'replyCompleted' (on success) or 'handleFailure' (on failure)
	private def _push(messages: Seq[ChatMessage], extraTools: Seq[Tool], allowStreaming: Boolean)
	                 (replyIncoming: ReplyMessage => Unit)(replyCompleted: BufferedReplyMessage => Unit)
	                 (handleFailure: Throwable => Unit): Unit =
	{
		_queueSizePointer.update { _ + 1 }
		
		// Calculates the context size to prepare, unless user-defined
		val defaultSettings = settings
		val settingsWithContextSize = {
			if (defaultSettings.contains(ContextTokens))
				defaultSettings
			else {
				val estimate = contextSize(defaultSettings.get(PredictTokens).int.toRight { messages.mkString(". ") })
				defaultSettings + (ContextTokens -> (estimate: Value))
			}
		}
		
		// At this time, streaming is not supported with tools
		val defaultTools = this.tools
		val tools = defaultTools ++ extraTools.view.filterNot(defaultTools.contains)
		val systemMessage = NotEmpty(systemMessages).map { _.mkString("\n\n") }.map { System(_) }
		// Sends the chat request
		val replyFuture = ollama.push(
			ChatParams(messages.last, systemMessage.emptyOrSingle ++ messageHistory ++ messages.dropRight(1),
				tools, settingsWithContextSize)
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
					reply.statisticsFuture
						.tryFlatMapIfSuccess { statistics =>
							val responseSize = statistics.responseTokenCount
							// Updates the context size pointers and uses the known information to deduct the
							// size of the latest message
							val messageSize = lastPromptAndReplySizesPointer.mutate { previous =>
								// Current total prompt & latest reply sizes are known from the response
								val current = Pair[UncertainInt](statistics.promptTokenCount, responseSize)
								// If previous prompt & reply sizes are also known,
								// the size of the latest message may also be deducted
								val promptIncrease = (current.first - previous.first) max 0
								val messageSize = promptIncrease - previous.second
								
								messageSize -> current
							}
							_largestReplySizePointer.update { _ max responseSize }
							
							reply.future.map { _.map { r => (r, messageSize, responseSize) } }
						}
						.foreachResult {
							// Case: Reply fully parsed
							//       => Updates message history & finishes state (unless tools are called)
							case Success((reply, messageSize, replySize)) =>
								_messageHistoryPointer.update { history =>
									// Stores the calculated or estimated message sizes, also
									val newPromptMessages = messageSize.exact match {
										case Some(messageSize) =>
											// Case: Message size could be calculated
											if (messages.hasSize(1))
												messages.map { _ -> messageSize }
											// Case: No messages were present
											else if (messages.isEmpty)
												Empty
											// Case: Total message size was calculated,
											//       but there were more messages than 1
											//       => Divides the calculated amount between the messages
											else {
												val estimations = messages
													.map { m => m -> EstimateTokenCount.in(m.text) }
												val totalEstimation = estimations.view.map { _._2 }.sum.toDouble
												estimations.map { case (m, estimate) =>
													m -> ((estimate / totalEstimation) * messageSize).toInt
												}
											}
										// Case: The message size could not be calculated => Estimates instead
										case None => messages.map { m => m -> EstimateTokenCount.in(m.text) }
									}
									// Appends prompt & reply
									history ++ newPromptMessages :+ (reply.message -> replySize)
								}
								
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
	}
	
	/**
	  * Checks the auto-summary conditions and performs the summarization if those conditions are met.
	  * Won't auto-summarize while messaging or summarizing.
	  */
	private def summarizeIfAppropriate() = {
		autoSummarizeAtTokensPointer.value.foreach { case Pair(minContext, minMessageCount) =>
			if (lastContextSize >= minContext && messageHistory.hasSize >= minMessageCount && idle)
				summarize(noStreaming = true)
		}
	}
	
	private def contextSize(nextMessageOrNumPredict: Either[String, Int]) = {
		// Estimates the number of tokens in the message and in the reply
		val messageSize = nextMessageOrNumPredict.rightOrMap(EstimateTokenCount.in)
		val expectedReplySize = _largestReplySizePointer.value.smallestPossibleValue match {
			case Some(smallestLargest) => smallestLargest max this.expectedReplySize
			case None => this.expectedReplySize
		}
		val rawValue = usedContextSizePointer.value + messageSize + expectedReplySize + additionalContextSize
		
		// TODO: Remove test print
		println(s"Calculated context size:\n\t- System + history: ${
			usedContextSizePointer.value } tokens\n\t- Message: $messageSize tokens\n\t- Estimated reply: $expectedReplySize tokens\n\t- Result: $rawValue tokens (including a buffer of $additionalContextSize)")
		
		// Limits to the allowed range
		if (rawValue >= maxContextSize)
			maxContextSize
		else if (rawValue <= minContextSize)
			minContextSize
		else
			rawValue
	}
}
