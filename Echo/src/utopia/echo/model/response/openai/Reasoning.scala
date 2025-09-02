package utopia.echo.model.response.openai

import utopia.annex.model.manifest.SchrodingerState
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.StringType

object Reasoning extends OpenAiModelParser[Reasoning] with FromModelFactoryWithSchema[Reasoning]
{
	// ATTRIBUTES   -----------------------
	
	override lazy val typeIdentifiers: Set[String] = Set("reasoning")
	override lazy val schema: ModelDeclaration = ModelDeclaration("id" -> StringType)
	
	
	// IMPLEMENTED  -----------------------
	
	override protected def fromValidatedModel(model: Model): Reasoning = {
		val summary = model("summary").getVector.iterator.map { _("text") }.mkString
		val text = model("content").getVector.iterator.map { _("text") }.mkString
		apply(model("id").getString, summary, text, model("encrypted_content").getString, parseStatusFrom(model))
	}
}

/**
  * Represents a reasoning summary acquired via Open AI API
  * @author Mikko Hilpinen
  * @since 04.04.2025, v1.3
  * @param id Id of this reasoning entry
  * @param text Reasoning text content
  */
case class Reasoning(id: String, summary: String, text: String, encrypted: String, state: SchrodingerState)
