package utopia.echo.model.response.openai.toolcall

import utopia.annex.model.manifest.SchrodingerState
import utopia.annex.model.manifest.SchrodingerState.{Alive, PositiveFlux}
import utopia.echo.model.response.openai.{OpenAiModelParser, OpenAiOutputElementFromModelFactory}
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.model.immutable.ModelDeclaration
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.{ModelLike, Property}

import scala.util.Try

object FileSearchToolCall extends OpenAiOutputElementFromModelFactory[FileSearchToolCall]
{
	// ATTRIBUTES   -------------------
	
	override lazy val typeIdentifiers: Set[String] = Set("file_search_call")
	private lazy val schema = ModelDeclaration("id" -> StringType)
	
	
	// IMPLEMENTED    -------------------
	
	/**
	  * @param index Index at which the model appears
	  * @return An interface for parsing a file search tool call from a model
	  */
	override def at(index: Int): OpenAiModelParser[FileSearchToolCall] = new CallAt(index)
	
	
	// NESTED   ------------------------
	
	private class CallAt(index: Int) extends OpenAiModelParser[FileSearchToolCall]
	{
		override def typeIdentifiers: Set[String] = FileSearchToolCall.typeIdentifiers
		
		override def apply(model: ModelLike[Property]): Try[FileSearchToolCall] =
			schema.validate(model).flatMap { model =>
				model("results").tryVectorWith { _.tryModel.flatMap(FileSearchResult.apply) }.map { results =>
					val status: SchrodingerState = model("status").getString match {
						case "searching" => PositiveFlux
						case status => interpretStatus(status)
					}
					FileSearchToolCall(index, model("id").getString, model("queries").getVector.map { _.getString },
						status, results)
				}
			}
	}
}

/**
  * Represents a file-search tool call, as modeled by Open AI
  * @author Mikko Hilpinen
  * @since 04.04.2025, v1.3
  * @param index Index at which this tool call appears in the output sequence
  * @param id ID of this file search tool call
  * @param queries Search queries performed
  * @param state Current state of this tool call.
  *                 - Alive if completed successfully
  *                 - Dead if failed or left incomplete
  *                 - Positive flux if searching
  *                 - Flux if in progress
  * @param results Acquired results
  */
case class FileSearchToolCall(index: Int, id: String, queries: Seq[String] = Empty, state: SchrodingerState = Alive,
                              results: Seq[FileSearchResult] = Empty)
	extends OpenAiOutputElement
