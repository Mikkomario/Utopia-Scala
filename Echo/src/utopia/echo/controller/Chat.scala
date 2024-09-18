package utopia.echo.controller

import utopia.annex.model.manifest.{HasSchrodingerState, SchrodingerState}
import utopia.annex.model.manifest.SchrodingerState.{Alive, Dead, Flux, PositiveFlux}
import utopia.annex.model.response.{RequestFailure, Response}
import utopia.annex.schrodinger.Schrodinger
import utopia.echo.model.enumeration.ChatRole.System
import utopia.echo.model.enumeration.ModelParameter.ContextTokens
import utopia.echo.model.enumeration.{ChatRole, ModelParameter}
import utopia.echo.model.request.chat.ChatParams
import utopia.echo.model.request.chat.tool.Tool
import utopia.echo.model.response.chat.{BufferedReplyMessage, ReplyMessage, StreamedReplyMessage}
import utopia.echo.model.{ChatMessage, LlmDesignator}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.TryFuture
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.parse.json.JsonParser
import utopia.flow.parse.string.Regex
import utopia.flow.util.{Mutate, NotEmpty, UncertainNumber}
import utopia.flow.util.UncertainNumber.{CertainNumber, UncertainInt}
import utopia.flow.util.logging.Logger
import utopia.flow.util.StringExtensions._
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.{EventfulPointer, IndirectPointer, LockablePointer}

import scala.concurrent.{ExecutionContext, Future}
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
  * @param llm Targeted LLM
  * @param exc Implicit execution context used in asynchronous processing
  * @param log Implicit logging implementation for handling pointer-related failures and for
  *            recording chat request failures
  * @param jsonParser Json parser for interpreting Ollama's responses
  */
class Chat(ollama: OllamaClient)
          (implicit llm: LlmDesignator, exc: ExecutionContext, log: Logger, jsonParser: JsonParser)
	extends HasSchrodingerState
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
	var minContextSize = 512
	/**
	  * Number of tokens expected within a reply.
	  * Used when no appropriate statistical information has been collected yet.
	  * One token is about 1/2 a word.
	  */
	var expectedReplySize = 256
	
	private val _messageHistoryPointer = EventfulPointer.emptySeq[ChatMessage]
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
	  * A pointer that contains the current chat state.
	  *     - Contains Flux while messaging is ongoing.
	  *     - Contains Alive if the last reply has been fully and successfully received.
	  *     - Contains Dead if failed to acquire a (full) reply for the latest outgoing message
	  */
	lazy val statePointer = _queueSizePointer.mergeWith(_lastResultPointer) { (queued, lastResult) =>
		// Case: Messages have been queued => Flux state
		if (queued > 0)
			Flux(lastResult.isSuccess)
		else
			lastResult match {
				// Case: Reply at least partially received => Flux+ or Alive if completed
				case Success(reply) => if (reply.isBuffered) Alive else PositiveFlux
				// Case: Response-reading failed => Dead
				case Failure(_) => Dead
			}
	}
	
	/**
	  * A mutable pointer that contains the currently applied LLM options / parameters.
	  *
	  * Note: If [[ContextTokens]] is defined here,
	  * that overrides / disables the automatic context size management -feature.
	  */
	val optionsPointer = EventfulPointer(Map[ModelParameter, Value]())
	/**
	  * A mutable pointer that contains the tools that are currently made available for the LLM
	  * (besides possible request-specific tools).
	  */
	val toolsPointer = EventfulPointer.emptySeq[Tool]
	
	private val _lastContextSizePointer = EventfulPointer[UncertainInt](UncertainNumber.zeroOrMore)
	/**
	  * A pointer that contains the context size of the most recent successful request
	  * within the current conversation.
	  * May contain unknown. Resets if message history or system messages are modified.
	  */
	lazy val lastContextSizePointer = _lastContextSizePointer.readOnly
	// Largest encountered context-size increase within a single chat message (request + reply)
	private val _largestContextIncreasePointer = EventfulPointer[UncertainInt](UncertainNumber.positive)
	/**
	  * A pointer that contains the largest increase in context size during the current conversation.
	  * Contain unknown while no suitable measurements have yet been made.
	  * Resets when message history is manually modified.
	  */
	lazy val largestContextIncreasePointer = _largestContextIncreasePointer.readOnly
	
	/**
	  * A mutable pointer that contains the current message history.
	  * Messages are automatically added to this history once their replies have been fully and successfully read.
	  *
	  * Mutate this pointer with caution, since it is also modified from within this instance.
	  * It is not recommended to mutate this pointer while messaging is ongoing / has not completed.
	  */
	lazy val messageHistoryPointer = IndirectPointer(_messageHistoryPointer) { newHistory =>
		val oldHistory = _messageHistoryPointer.getAndSet(newHistory)
		if (oldHistory != newHistory) {
			// Resets the known context size whenever message history is manually edited
			_lastContextSizePointer.value = UncertainNumber.zeroOrMore
			_largestContextIncreasePointer.update { previous =>
				// The largest increase minimum value may be preserved, if the history is only appended
				previous.smallestPossibleValue match {
					case Some(smallest) =>
						val parallelHistory = oldHistory.zip(newHistory)
						if (parallelHistory.hasSize.of(oldHistory) &&
							parallelHistory.forall { case (o, n) => o.text == n.text })
							UncertainNumber.greaterThan(smallest, orEqual = true)
						else
							UncertainNumber.positive
					
					case None => UncertainNumber.positive
				}
			}
		}
	}
	
	
	// INITIAL CODE ---------------------------
	
	systemMessagesPointer.addContinuousAnyChangeListener {
		// Resets the known context size whenever the system message is updated
		_lastContextSizePointer.value = UncertainNumber.zeroOrMore
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
	def messageHistory = _messageHistoryPointer.value
	// Note: Only expected to be called from the outside
	def messageHistory_=(newHistory: Seq[ChatMessage]) = messageHistoryPointer.value = newHistory
	
	/**
	  * @return The current number of queued messages which have not yet received any reply, even a streamed one.
	  */
	def queueSize = _queueSizePointer.value
	
	/**
	  * @return Applied LLM options / parameters
	  */
	def options = optionsPointer.value
	def options_=(newOptions: Map[ModelParameter, Value]) = optionsPointer.value = newOptions
	
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
	  * @return Result of the last completed chat request. May contain a failure.
	  */
	def lastResult = _lastResultPointer.value
	/**
	  * @return Last successfully received reply message. May still be streaming.
	  *         Not necessarily fully successfully parsed, however.
	  */
	def lastReply = lastReplyPointer.value
	
	/**
	  * @return Number of tokens used in the previous fully completed request.
	  *         Uncertain while no requests have yet been completed within the current conversation.
	  */
	def lastContextSize = _lastContextSizePointer.value
	/**
	  * @return Largest single request context size increase encountered within this conversation.
	  */
	def largestContextIncrease = _largestContextIncreasePointer.value
	
	
	// IMPLEMENTED  ---------------------------
	
	override def state: SchrodingerState = statePointer.value
	
	
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
		
		val replyFuture = _push(Single(ChatMessage(message, encodedImages = images)), extraTools,
			allowStreaming = !noStreaming) {
			_ => statePointer.value =
				PositiveFlux -> Failure(new IllegalStateException("The reply hasn't been fully formed yet")) } {
			reply => statePointer.value = Alive -> Success(reply) } {
			error =>
				log(error, "Messaging failed")
				statePointer.value = Dead -> Failure(error)
				statePointer.lock()
		}
		
		// Returns a Schrödinger
		val asyncReply = ReplyMessage.async(replyFuture)
		Schrodinger.wrap(statePointer.strongMap { case (state, reply) => (asyncReply, reply, state) })
	}
	
	/**
	  * Updates the value of a model parameter
	  * @param param Modified parameter
	  * @param value New value assigned for the targeted parameter
	  */
	def update(param: ModelParameter, value: Value) = mapOptions { _ + (param -> value) }
	/**
	  * Modifies the value of a single model parameter
	  * @param option Targeted parameter
	  * @param f Function for mapping the current parameter value.
	  *          If this parameter has not yet been defined, receives that option's default value.
	  */
	def mapOption(option: ModelParameter)(f: Mutate[Value]) = mapOptions { o =>
		o + (option -> f(o.getOrElse(option, option.defaultValue)))
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
	  * Clears all custom LLM parameter definitions
	  */
	def clearOptions() = options = Map()
	/**
	  * Clears the value of a single option-definition, returning it to its default value
	  * @param option Option to clear
	  */
	def clear(option: ModelParameter) = mapOptions { _ - option }
	/**
	  * Clears the value of 0-n option-definitions, returning them to their default values
	  * @param options Options to clear
	  */
	def clearOptions(options: IterableOnce[ModelParameter]) = mapOptions { _ -- options }
	def clearOptions(option1: ModelParameter, option2: ModelParameter, moreOptions: ModelParameter*): Unit =
		clearOptions(Pair(option1, option2) ++ moreOptions)
	
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
	  * Updates the value of a model parameter
	  * @param param Modified parameter + assigned value
	  */
	def +=(param: (ModelParameter, Value)) = update(param._1, param._2)
	/**
	  * Registers a new tool for the LLM to utilize
	  * @param tool Tool made available for the LLM
	  */
	def +=(tool: Tool) = addTool(tool)
	
	/**
	  * Clears the value of a single option-definition, returning it to its default value
	  * @param param Option / parameter to clear
	  */
	def -=(param: ModelParameter) = clear(param)
	/**
	  * Removes a tool from the LLM's options
	  * @param tool Tool to remove
	  */
	def -=(tool: Tool) = removeTool(tool)
	
	/**
	  * Updates 0-n LLM parameters
	  * @param params New parameters to assign
	  */
	def ++=(params: IterableOnce[(ModelParameter, Value)]) = mapOptions { _ ++ params }
	/**
	  * Clears 0-n LLM parameters, so that they won't be specified in future requests
	  * @param params Parameters to clear
	  */
	def --=(params: IterableOnce[ModelParameter]) = clearOptions(params)
	
	/**
	  * @param f Mapping function applied to system messages
	  */
	def mapSystemMessages(f: Mutate[Seq[String]]) = systemMessagesPointer.update(f)
	/**
	  * @param f Mapping function applied to chat history
	  */
	def mapMessageHistory(f: Mutate[Seq[ChatMessage]]) = messageHistoryPointer.update(f)
	/**
	  * @param f Mapping function applied to LLM options
	  */
	def mapOptions(f: Mutate[Map[ModelParameter, Value]]) = optionsPointer.update(f)
	/**
	  * @param f Mapping function applied to LLM tools
	  */
	def mapTools(f: Mutate[Seq[Tool]]) = toolsPointer.update(f)
	
	// Assumes that messages is not empty
	// 'replyIncoming' is called when a streamed reply is acquired. Not called if the reply is buffered.
	// Final call will be either 'replyCompleted' (on success) or 'handleFailure' (on failure)
	private def _push(messages: Seq[ChatMessage], extraTools: Seq[Tool], allowStreaming: Boolean)
	                 (replyIncoming: ReplyMessage => Unit)(replyCompleted: BufferedReplyMessage => Unit)
	                 (handleFailure: Throwable => Unit): Future[Try[BufferedReplyMessage]] =
	{
		_queueSizePointer.update { _ + 1 }
		
		// Calculates the context size to prepare, unless user-defined
		val defaultOptions = options
		val optionsWithContextSize = {
			if (defaultOptions.contains(ContextTokens))
				defaultOptions
			else
				defaultOptions + (ContextTokens -> (contextSize(messages.mkString(". ")): Value))
		}
		
		// At this time, streaming is not supported with tools
		val defaultTools = this.tools
		val tools = defaultTools ++ extraTools.view.filterNot(defaultTools.contains)
		val systemMessage = NotEmpty(systemMessages).map { _.mkString("\n\n") }.map { System(_) }
		// Sends the chat request
		val replyFuture = ollama.push(
			ChatParams(messages.last, systemMessage.emptyOrSingle ++ messageHistory, tools, optionsWithContextSize)
				.toRequest(stream = allowStreaming && tools.isEmpty)).future
		
		// Updates the pointers once a response is received
		replyFuture.flatMap { result =>
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
							// Updates the minimum context size
							val contextSizeIncrease = _lastContextSizePointer.mutate { previouslyUsed =>
								val nowUsed = CertainNumber(statistics.contextTokenCount)
								(nowUsed - previouslyUsed) -> nowUsed
							}
							// Updates the max context size increase
							contextSizeIncrease.exact.foreach { increase =>
								_largestContextIncreasePointer.update {
									_.exact match {
										case Some(previously) => previously max increase
										case None => increase
									}
								}
							}
							reply.future
						}
						.flatMap {
							// Case: Reply fully parsed
							//       => Updates message history & finishes state (unless tools are called)
							case Success(reply) =>
								_messageHistoryPointer.update { _ ++ (messages :+ reply.message) }
								
								// Case: No tool-calls => Finishes
								if (reply.toolCalls.isEmpty) {
									replyCompleted(reply)
									TryFuture.success(reply)
								}
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
							case Failure(error) =>
								handleFailure(error)
								TryFuture.failure(error)
						}
				
				// Case: Failed to acquire a response => Fails
				case f: RequestFailure =>
					handleFailure(f.cause)
					TryFuture.failure(f.cause)
			}
		}
	}
	
	private def contextSize(nextMessage: => String) = {
		// Prepares for a specific increase in context size
		val expectedIncrease = _largestContextIncreasePointer.value.exact
			.getOrElse {
				val calculated = countTokensIn(nextMessage) + expectedReplySize
				_largestContextIncreasePointer.value.smallestPossibleValue match {
					case Some(smallest) => smallest max calculated
					case None => calculated
				}
			}
		// Attempts to acquire the size of the last interaction context
		val rawValue = _lastContextSizePointer.value.exact match {
			case Some(lastUsedContext) => lastUsedContext + expectedIncrease
			case None =>
				val systemSize = NotEmpty(systemMessages) match {
					case Some(messages) => messages.iterator.map(countTokensIn).sum
					case None => minContextSize
				}
				val historySize = messageHistory.iterator.map { m => countTokensIn(m.text) }.sum
				
				systemSize + historySize + expectedIncrease
		}
		// Limits to the allowed range
		if (rawValue >= maxContextSize)
			maxContextSize
		else if (rawValue <= minContextSize)
			minContextSize
		else
			rawValue
	}
	
	private def countTokensIn(message: String) =
		message.splitIterator(Regex.whiteSpace).map { word => if (word.length > 4) 2 else 1 }.sum
}
