package utopia.echo.model.response.openai

import utopia.annex.model.manifest.SchrodingerState
import utopia.annex.model.manifest.SchrodingerState.Alive
import utopia.echo.model.response.openai.toolcall.{FileSearchToolCall, OpenAiFunctionToolCall, WebSearchToolCall}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.{ModelLike, Property}
import utopia.flow.time.Now

import java.time.Instant
import scala.util.Try

object OpenAiResponse extends FromModelFactory[OpenAiResponse]
{
	// ATTRIBUTES   -------------------
	
	private lazy val schema = ModelDeclaration("id" -> StringType)
	
	
	// IMPLEMENTED  -------------------
	
	override def apply(model: ModelLike[Property]): Try[OpenAiResponse] = schema.validate(model).flatMap { model =>
		model("usage").tryModel.flatMap(OpenAITokenUsageStatistics.apply).flatMap { tokenUsage =>
			model("output").getVector.tryMap { _.tryModel }.flatMap { outputModels =>
				val outputByType = outputModels.zipWithIndex.groupBy { _._1("type").getString }.withDefaultValue(Empty)
				OpenAiMessage(outputByType).flatMap { messages =>
					OpenAiFunctionToolCall(outputByType).flatMap { functionCalls =>
						WebSearchToolCall(outputByType).flatMap { webSearchCalls =>
							FileSearchToolCall(outputByType).map { fileSearchCalls =>
								apply(model("id").getString, messages, functionCalls, webSearchCalls, fileSearchCalls,
									model("metadata").getModel, OpenAiModelParser.parseStatusFrom(model),
									model("incomplete_details")("reason").getString,
									model("error").model.map(OpenAiError.parseFrom), tokenUsage,
									model("created_at").getInstant)
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
  * @author Mikko Hilpinen
  * @since 04.04.2025, v1.3
  */
// TODO: Document
// TODO: Add support for "computer use" tool-calls
// TODO: Add details about the request
case class OpenAiResponse(id: String, messages: Seq[OpenAiMessage] = Empty,
                          functionCalls: Seq[OpenAiFunctionToolCall] = Empty,
                          webSearchCalls: Seq[WebSearchToolCall] = Empty,
                          fileSearchCalls: Seq[FileSearchToolCall] = Empty, metadata: Model = Model.empty,
                          state: SchrodingerState = Alive,
                          whyIncomplete: String = "", error: Option[OpenAiError] = None,
                          tokenUsage: OpenAITokenUsageStatistics, created: Instant = Now)
