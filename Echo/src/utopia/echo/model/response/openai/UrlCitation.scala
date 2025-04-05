package utopia.echo.model.response.openai

import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.collection.immutable.range.NumericSpan.IntSpan
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.{IntType, StringType}
import utopia.flow.generic.casting.ValueUnwraps._

object UrlCitation extends OpenAiModelParser[UrlCitation] with FromModelFactoryWithSchema[UrlCitation]
{
	// ATTRIBUTES   ---------------------
	
	override lazy val typeIdentifiers: Set[String] = Set("url_citation")
	override lazy val schema: ModelDeclaration = ModelDeclaration(
		"url" -> StringType, "start_index" -> IntType, "end_index" -> IntType)
	
	
	// IMPLEMENTED  ---------------------
	
	override protected def fromValidatedModel(model: Model): UrlCitation =
		apply(model("url"), NumericSpan(model("start_index").getInt, model("end_index").getInt), model("title"))
}

/**
  * Represents a citation from a website. Used in Open AI's responses.
  * @author Mikko Hilpinen
  * @since 04.04.2025, v1.3
  */
case class UrlCitation(url: String, charRange: IntSpan, title: String = "")
