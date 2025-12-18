package utopia.echo.controller.chat

import utopia.annex.model.request.ApiRequest
import utopia.echo.controller.client.OllamaClient
import utopia.echo.model.ChatMessage
import utopia.echo.model.llm.{LlmDesignator, ModelSettings}
import utopia.echo.model.request.ChatParams
import utopia.echo.model.request.tool.ToolFactory
import utopia.echo.model.response.ollama.{BufferedOllamaReply, OllamaReply}
import utopia.echo.model.tokenization.PartiallyEstimatedTokenCount
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.model.template.HasValues
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.result.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.template.eventful.Changing

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

// TODO: Copy / extend this parsing feature to other chats
object OllamaChat
{
	// OTHER    ------------------------------
	
	/**
	 * Parses a chat from a model, applying that model's state (system messages, message history, settings, etc.)
	 * Note: Non-critical failures are logged.
	 * @param model Model representing chat state
	 * @param toolFactory Factory used for defining tool functionality.
	 *                    Default = not implemented.
	 *                    Note: The default parameter throws if any tools have been defined!
	 * @param exc Implicit execution context
	 * @param log Implicit logging interface used
	 * @param jsonParser Json parser used in response-processing
	 * @param client Wrapped / utilized Ollama client interface
	 * @return Parsed chat interface. Failure if the specified model didn't specify the targeted LLM.
	 */
	def parseFrom(model: HasValues, toolFactory: ToolFactory = ToolFactory.notImplemented)
	             (implicit exc: ExecutionContext, log: Logger, jsonParser: JsonParser, client: OllamaClient) =
	{
		model("llm").string.toTry { new NoSuchElementException("Required parameter \"llm\" is missing") }.map { llm =>
			val chat = new OllamaChat(LlmDesignator(llm, thinks = model("llmThinks").getBoolean))
			
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
				chat.messageHistoryWithSizes = history.flatMap { v =>
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
  * @param initialLlm The initially targeted LLM
  * @param exc Implicit execution context used in asynchronous processing
  * @param log Implicit logging implementation for handling pointer-related failures and for
  *            recording chat request failures
  * @param jsonParser Json parser for interpreting Ollama's responses
 * @param client Wrapped / utilized Ollama client interface
  */
class OllamaChat(initialLlm: LlmDesignator)
                (implicit exc: ExecutionContext, log: Logger, jsonParser: JsonParser, client: OllamaClient)
	extends AbstractChat[OllamaReply, BufferedOllamaReply, OllamaChat](client, initialLlm,
		BufferedOllamaReply.empty, BufferedOllamaReply.empty) with Chat
{
	// IMPLEMENTED  ---------------------------
	
	override def self: OllamaChat = this
	
	override protected def copyWithoutState: OllamaChat = new OllamaChat(llm)
	
	override protected def makeRequest(params: ChatParams, allowStreaming: Boolean): ApiRequest[OllamaReply] =
		params.toRequest(allowStreaming && params.tools.isEmpty)
	
	override protected def streamedReplyFrom(textPointer: Changing[String], newTextPointer: Changing[String],
	                                         lastUpdatedPointer: Changing[Instant],
	                                         resultFuture: Future[Try[BufferedOllamaReply]]): OllamaReply =
		OllamaReply(textPointer, newTextPointer, lastUpdatedPointer).futureBuffered(resultFuture)
}
