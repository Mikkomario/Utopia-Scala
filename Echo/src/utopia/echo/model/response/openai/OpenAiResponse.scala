package utopia.echo.model.response.openai

import utopia.annex.model.manifest.SchrodingerState
import utopia.annex.model.manifest.SchrodingerState.Alive
import utopia.echo.model.ChatMessage
import utopia.echo.model.enumeration.ChatRole.Assistant
import utopia.echo.model.response.{BufferedReply, BufferedReplyLike}
import utopia.echo.model.response.openai.toolcall.{FileSearchToolCall, OpenAiFunctionToolCall, WebSearchToolCall}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.time.Now

import java.time.Instant
import scala.util.Try

object OpenAiResponse extends FromModelFactory[OpenAiResponse]
{
	// ATTRIBUTES   -------------------
	
	private val schema = ModelDeclaration("id" -> StringType)
	
	/**
	 * An empty Open AI response
	 */
	lazy val empty = apply("", OpenAiTokenUsageStatistics.zero)
	
	
	// IMPLEMENTED  -------------------
	
	override def apply(model: HasProperties): Try[OpenAiResponse] = schema.validate(model).flatMap { model =>
		model("usage").tryModel.flatMap(OpenAiTokenUsageStatistics.apply).flatMap { tokenUsage =>
			model("output").getVector.tryMapAll { _.tryModel }.flatMap { outputModels =>
				val outputByType: Map[String, Seq[(Model, Int)]] = outputModels.zipWithIndex
					.groupBy { _._1("type").getString }.withDefaultValue(Empty)
				OpenAiMessage(outputByType).flatMap { messages =>
					outputByType("reasoning").tryMapAll { case (model, _) => Reasoning(model) }.flatMap { reasoning =>
						OpenAiFunctionToolCall(outputByType).flatMap { functionCalls =>
							WebSearchToolCall(outputByType).flatMap { webSearchCalls =>
								FileSearchToolCall(outputByType).map { fileSearchCalls =>
									apply(model("id").getString, tokenUsage, messages, reasoning,
										functionCalls, webSearchCalls, fileSearchCalls,
										model("metadata").getModel, OpenAiModelParser.parseStatusFrom(model),
										model("incomplete_details")("reason").getString,
										model("error").model.map(OpenAiError.parseFrom),
										model("created_at").getInstant)
								}
							}
						}
					}
				}
			}
		}
	}
}

/**
  * Represents a complete response received from the Open AI API
  * @param id Unique identifier for this Response.
 * @param tokenUsage Information about the token-usage throughout this response
 * @param messages Generated (assistant) messages
 * @param functionCalls Function calls requested by the LLM
 * @param webSearchCalls Calls made to the Open AI web search function
 * @param fileSearchCalls Open AI file-search tool calls made by the LLM
 * @param metadata Open form metadata included from other requests
 * @param state State of this response, where:
 *                  - Flux = In progress
 *                  - Alive = Completed successfully
 *                  - Dead = Left incomplete (i.e. rejected) or failed
 * @param whyIncomplete Reason given by the API, why this response was left incomplete, if applicable.
 *                      May be empty (even if incomplete).
 * @param error Error linked to this failure response, if available / applicable
 * @param created Time when this response was created (server-side)
 * @author Mikko Hilpinen
  * @since 04.04.2025, v1.3
  */
// TODO: Add support for "computer use" tool-calls (once other tool calls are better supported)
// TODO: Add details about the request
case class OpenAiResponse(id: String, tokenUsage: OpenAiTokenUsageStatistics, messages: Seq[OpenAiMessage] = Empty,
                          reasoning: Seq[Reasoning] = Empty,
                          functionCalls: Seq[OpenAiFunctionToolCall] = Empty,
                          webSearchCalls: Seq[WebSearchToolCall] = Empty,
                          fileSearchCalls: Seq[FileSearchToolCall] = Empty, metadata: Model = Model.empty,
                          override val state: SchrodingerState = Alive,
                          whyIncomplete: String = "", error: Option[OpenAiError] = None, created: Instant = Now)
	extends BufferedReply with BufferedReplyLike[OpenAiResponse]
{
	// ATTRIBUTES   ------------------------
	
	override lazy val text: String =
		messages.reverseIterator.find { _.senderRole == Assistant }.orElse(messages.lastOption) match {
			case Some(message) => message.text
			case None => ""
		}
	override lazy val thoughts: String = reasoning.iterator.map { _.text }.mkString
	
	override lazy val message: ChatMessage = ChatMessage(text, thoughts,
		senderRole = {
			if (messages.isEmpty || messages.exists { _.senderRole == Assistant })
				Assistant
			else
				messages.last.senderRole
		},
		toolCalls = functionCalls)
	
	
	// IMPLEMENTED  -----------------------
	
	override def self = this
	
	override def lastUpdated: Instant = created
}