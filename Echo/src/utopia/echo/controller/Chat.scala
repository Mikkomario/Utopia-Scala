package utopia.echo.controller

import utopia.annex.model.manifest.SchrodingerState.{Alive, Dead, Flux}
import utopia.annex.model.response.{RequestFailure, Response}
import utopia.annex.schrodinger.PullSchrodinger
import utopia.annex.util.RequestResultExtensions._
import utopia.echo.model.enumeration.ModelParameter
import utopia.echo.model.request.chat.ChatParams
import utopia.echo.model.request.chat.tool.Tool
import utopia.echo.model.response.chat.StreamedReplyMessage
import utopia.echo.model.{ChatMessage, LlmDesignator}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.sign.Sign.Positive
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.EventfulPointer

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/**
  * An interface for interactive chat which supports conversation history
  * @author Mikko Hilpinen
  * @since 16.09.2024, v1.1
  */
// TODO: Handle tool calls
class Chat(ollama: OllamaClient)
          (implicit llm: LlmDesignator, exc: ExecutionContext, log: Logger, jsonParser: JsonParser)
{
	// ATTRIBUTES   ---------------------------
	
	private val messageHistoryPointer = EventfulPointer.emptySeq[ChatMessage]
	private val systemMessagesPointer = EventfulPointer.emptySeq[ChatMessage]
	
	private val queueSizePointer = Volatile.eventful(0)
	
	private val lastResultPointer = EventfulPointer[Try[StreamedReplyMessage]](Success(StreamedReplyMessage.empty()))
	private val lastReplyPointer = lastResultPointer.incrementalMap { _.get } { (previous, resultEvent) =>
		resultEvent.newValue.getOrElse(previous)
	}
	
	private lazy val statePointer = queueSizePointer.mergeWith(lastResultPointer) { (queued, lastResult) =>
		// Case: Messages have been queued => Flux state
		if (queued > 0)
			Flux(lastResult.isSuccess)
		else
			lastResult match {
				// Case: Reply at least partially received => Flux+ or Alive if completed
				case Success(reply) => if (reply.isBuffered) Alive else Flux(Positive)
				// Case: Response-reading failed => Dead
				case Failure(_) => Dead
			}
	}
	
	// TODO: Add context size management
	private val optionsPointer = EventfulPointer(Map[ModelParameter, Value]())
	private val toolsPointer = EventfulPointer.emptySeq[Tool]
	
	private val usedContextTokensPointer = EventfulPointer(0)
	
	
	// COMPUTED -------------------------------
	
	def systemMessages = systemMessagesPointer.value
	def messageHistory = messageHistoryPointer.value
	
	def usedContextTokens = usedContextTokensPointer.value
	private def usedContextTokens_=(newSize: Int) = usedContextTokensPointer.value = newSize
	
	def options = optionsPointer.value
	def tools = toolsPointer.value
	
	
	// OTHER    -------------------------------
	
	def push(message: String) = {
		// TODO: Implement
		???
	}
	
	private def _push(message: String, images: Seq[String] = Empty) = {
		// Sends the chat request
		queueSizePointer.update { _ + 1 }
		val outboundMessage = ChatMessage(message, encodedImages = images)
		val future = ollama.push(
			ChatParams(outboundMessage, systemMessages ++ messageHistory, tools, options).toRequest.streamed).future
		
		// Updates the pointers once a response is received
		future.foreachResult { result =>
			lastResultPointer.value = result.toTry
			queueSizePointer.update { _ - 1 }
			
			result match {
				case Response.Success(reply, _, _) =>
					// Handles the reply completion asynchronously
					reply.statisticsFuture.foreach {
						case Success(statistics) =>
							usedContextTokens = statistics.contextTokenCount
							
							// Updates message history
							reply.future.foreachSuccess { reply =>
								messageHistoryPointer.update { _ ++ Pair(outboundMessage, reply.message) }
							}
							
						case Failure(error) => lastResultPointer.value = Failure(error)
					}
				case f: RequestFailure => log(f.cause, "Messaging failed")
			}
		}
		
		// Returns a Schrödinger
		// TODO: Implement
	}
}
