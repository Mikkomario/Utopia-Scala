package utopia.echo.controller.chat

import utopia.annex.controller.RequestQueue
import utopia.annex.model.manifest.SchrodingerState
import utopia.annex.model.manifest.SchrodingerState._
import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.{RequestFailure, Response}
import utopia.annex.schrodinger.Schrodinger
import utopia.annex.util.RequestResultExtensions._
import utopia.echo.controller.EstimateTokenCount
import utopia.echo.model.ChatMessage
import utopia.echo.model.enumeration.ChatRole.System
import utopia.echo.model.enumeration.ModelParameter.{ContextTokens, PredictTokens}
import utopia.echo.model.enumeration.{ChatRole, ModelParameter}
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.request.ChatParams
import utopia.echo.model.request.tool.Tool
import utopia.echo.model.response.{BufferedReply, ReplyLike}
import utopia.echo.model.tokenization.{EstimatedTokenCount, PartiallyEstimatedTokenCount, TokenCounts}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.TryFuture
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.Now
import utopia.flow.util.logging.Logger
import utopia.flow.util.{NotEmpty, UncertainBoolean}
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful._
import utopia.flow.view.template.eventful.Changing

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
 * @param initialLlm The initially assigned LLM
 * @param requestQueue Queue for sending out API requests
 * @param emptyReply An empty reply instance. May be used as a placeholder.
 * @param emptyBufferedReply An empty buffered reply instance. May be used as a placeholder.
 * @param log Implicit logging implementation for handling pointer-related failures and for
 *            recording chat request failures
 * @param exc Implicit execution context used in asynchronous processing
 * @param jsonParser Json parser for processing API responses
  */
abstract class AbstractChat[R <: ReplyLike[BR], BR <: BufferedReply, +Repr <: AbstractChat[R, _, _]]
(requestQueue: RequestQueue, initialLlm: LlmDesignator, emptyReply: R, emptyBufferedReply: BR)
(implicit override protected val log: Logger, override protected val exc: ExecutionContext, jsonParser: JsonParser)
	extends ChatLike[R, BR, Repr]
{
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
	 * @param newTextPointer New text pointer to wrap
	 * @param lastUpdatedPointer A pointer that will contain the last text update time
	 * @param resultFuture A future that will yield the final buffered reply, if successful
	 * @return A new streamed reply instance
	 */
	protected def streamedReplyFrom(textPointer: Changing[String], newTextPointer: Changing[String],
	                                lastUpdatedPointer: Changing[Instant], resultFuture: Future[Try[BR]]): R
	
	
	// ATTRIBUTES   ---------------------------
	
	/**
	 * A mutable pointer that contains the LLM conversed with using this chat
	 */
	override val llmPointer: EventfulPointer[LlmDesignator] = Pointer.eventful(initialLlm)
	
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
	
	
	// IMPLEMENTED  ---------------------------
	
	/**
	 * @return A copy of this chat at its current state
	 */
	def copy = {
		val copy = copyWithoutState
		
		copy.maxContextSize = maxContextSize
		copy.minContextSize = minContextSize
		copy.expectedReplySize = expectedReplySize
		copy.additionalContextSize = additionalContextSize
		copy.thinkingContextSize = thinkingContextSize
		copy.additionalThinkingContextSize = additionalThinkingContextSize
		
		copy.thinkingEnabled = thinkingEnabled
		copy.messageHistoryWithSizes = messageHistoryWithSizes
		copy.systemMessagesPointer.value = systemMessagesPointer.value
		copy._lastResultPointer.value = _lastResultPointer.value
		copy.settingsPointer.value = settingsPointer.value
		copy.toolsPointer.value = toolsPointer.value
		copy.defaultSystemMessageTokensPointer.value = defaultSystemMessageTokensPointer.value
		copy.knownLastPromptAndReplySizePointer.value = knownLastPromptAndReplySizePointer.value
		copy.largestReplySize = largestReplySize
		copy.autoSummarizeAtTokensPointer.value = autoSummarizeAtTokensPointer.value
		copy.summarizing = summarizing
		
		copy
	}
	
	
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
		val statePointer = LockablePointer[(SchrodingerState, Try[BR])](
			Flux(lastResult.isSuccess) -> Failure(new IllegalArgumentException("No response has been acquired yet")))
		
		// Prepares the text, newText & lastUpdated pointers
		// These depend on whether streaming or tools are used
		val finalReplyPromise = Promise[Try[BR]]()
		val (textPointer, newTextPointer, lastUpdatedPointer, replyIncoming, replyCompleted, handleFailure) = {
			// Case: Tools are used or streaming is disabled => Prepares to receive only the final reply
			if (noStreaming || extraTools.nonEmpty || this.usesTools) {
				val textPointer = new MutableOnce("")
				val lastUpdatedPointer = new MutableOnce(Now.toInstant)
				
				def replyCompleted(reply: BR) = {
					textPointer.value = reply.text
					lastUpdatedPointer.value = reply.lastUpdated
				}
				
				(textPointer, textPointer, lastUpdatedPointer, None, Some[BR => Unit](replyCompleted), None)
			}
			// Case: Streaming is enabled
			//       => Prepares to receive the text incrementally, but only expects to receive a single response
			else {
				val textPointer = OnceFlatteningPointer("")
				val newTextPointer = OnceFlatteningPointer("")
				val lastUpdatedPointer = OnceFlatteningPointer(Now.toInstant)
				
				def replyIncoming(reply: R) = {
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
					Some[R => Unit](replyIncoming), None, Some[() => Unit](handleFailure))
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
					finalReplyPromise.success(Success(completedReply))
					statePointer.value = Alive -> Success(completedReply) } {
				error =>
					handleFailure.foreach { _() }
					finalReplyPromise.success(Failure(error))
					statePointer.value = Dead -> Failure(error)
					statePointer.lock()
					log(error, "Messaging failed") }
		}
		
		// Returns a Schrödinger
		val reply = streamedReplyFrom(textPointer, newTextPointer, lastUpdatedPointer, finalReplyPromise.future)
		Schrodinger.wrap(statePointer.strongMap { case (state, finalReply) => (reply, finalReply, state) })
	}
	
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
	                 (replyIncoming: R => Unit)(replyCompleted: BR => Unit)
	                 (handleFailure: Throwable => Unit): Unit =
	{
		updateQueueSize { _ + 1 }
		
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
					makeRequest(
						ChatParams(messages.last,
							systemMessage.emptyOrSingle ++ messageHistory ++ messages.dropRight(1), tools, settings,
							think = if (thinkingDisabled && llm.thinks) false else UncertainBoolean),
						allowStreaming))
					.future
				
				// Updates the pointers once a response is received
				replyFuture.forResult { result =>
					_lastResultPointer.value = result.toTry
					updateQueueSize { _ - 1 }
					
					result match {
						// Case: Successfully acquired a response => Processes the streamed response contents
						case Response.Success(reply, _, _) =>
							if (reply.isStreaming)
								replyIncoming(reply)
							// Handles the reply completion asynchronously
							reply.future.forResult {
								// Case: Reply fully parsed
								//       => Updates message history & finishes state (unless tools are called)
								case Success(reply) =>
									val totalPromptSize = reply.tokenUsage.input
									// If a <think> block was present, the reply size can't be known for certain
									val totalResponseSize = reply.tokenUsage.output
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
									updateLargestReplySize { _ max totalResponseSize }
									
									val replyMessage = reply.message
									appendMessageHistory(messages, tokenCounts.newMessages, messageSize,
										replyMessage, responseSize, previousContextSize)
									
									// Case: No tool-calls => Finishes
									if (replyMessage.toolCalls.isEmpty)
										replyCompleted(reply)
									// Case: Tool-calls => Applies the tools and forms a new request
									else {
										val toolMessages = replyMessage.toolCalls.map { call =>
											// TODO: Add OpenAI support: https://platform.openai.com/docs/guides/function-calling
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
			modifiedSizeHistory ++ newPromptMessages :+ (receivedReply -> replySizeEstimate)		}
	}
	
	private def countContextSize(nextMessages: Iterable[String], numPredict: Option[Int]) = {
		// Estimates the number of tokens in the message and in the reply
		val messageSize = {
			if (nextMessages.isEmpty)
				EstimatedTokenCount.zero
			else
				nextMessages.iterator.map(EstimateTokenCount.in).reduce { _ + _ }
		}
		val expectedReplySize = numPredict.getOrElse { this.expectedReplySize max largestReplySize }
		val used = lastContextSize
		
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
