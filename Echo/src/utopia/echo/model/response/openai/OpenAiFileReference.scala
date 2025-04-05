package utopia.echo.model.response.openai

import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.{IntType, StringType}

object OpenAiFileReference
	extends OpenAiModelParser[OpenAiFileReference] with FromModelFactoryWithSchema[OpenAiFileReference]
{
	// ATTRIBUTES   ----------------------
	
	override lazy val typeIdentifiers = Set("file_citation", "file_path")
	override lazy val schema: ModelDeclaration = ModelDeclaration("file_id" -> StringType, "index" -> IntType)
	
	
	// IMPLEMENTED  ----------------------
	
	override protected def fromValidatedModel(model: Model): OpenAiFileReference =
		apply(model("file_id").getString, model("index").getInt)
}

/**
  * Used in responses to signify a reference to a file
  * @author Mikko Hilpinen
  * @since 04.04.2025, v1.3
  */
case class OpenAiFileReference(fileId: String, index: Int)
