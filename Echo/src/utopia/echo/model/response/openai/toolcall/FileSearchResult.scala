package utopia.echo.model.response.openai.toolcall

import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.{DoubleType, StringType}

object FileSearchResult extends FromModelFactoryWithSchema[FileSearchResult]
{
	// ATTRIBUTES   ----------------------
	
	override lazy val schema: ModelDeclaration = ModelDeclaration("file_id" -> StringType, "score" -> DoubleType)
	
	
	// IMPLEMENTED  ----------------------
	
	override protected def fromValidatedModel(model: Model): FileSearchResult =
		apply(model("file_id"), model("filename"), model("text"), model("score"), model("attributes").getModel)
}

/**
  * Used for modeling responses from Open AI concerning file search tool calls' results
  * @author Mikko Hilpinen
  * @since 04.04.2025, v1.3
  * @param fileId ID of the found file
  * @param fileName Name of the found file
  * @param text Text extracted from the file
  * @param score Relevance score of this file [0,1]
  * @param attributes User-defined attributes attached to this file
  */
case class FileSearchResult(fileId: String, fileName: String, text: String, score: Double,
                            attributes: Model = Model.empty)
