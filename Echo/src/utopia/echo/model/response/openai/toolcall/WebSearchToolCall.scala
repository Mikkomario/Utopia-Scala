package utopia.echo.model.response.openai.toolcall

import utopia.annex.model.manifest.SchrodingerState
import utopia.echo.model.response.openai.{OpenAiModelParser, OpenAiOutputElementFromModelFactory}
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.StringType

object WebSearchToolCall extends OpenAiOutputElementFromModelFactory[WebSearchToolCall]
{
	// ATTRIBUTES   ----------------------
	
	override lazy val typeIdentifiers: Set[String] = Set("web_search_call")
	private lazy val schema: ModelDeclaration = ModelDeclaration("id" -> StringType)
	
	
	// IMPLEMENTED    --------------------
	
	/**
	  * @param index Index at which the model appears
	  * @return An interface for parsing a model into a web-search tool-call
	  */
	override def at(index: Int): OpenAiModelParser[WebSearchToolCall] = new CallAt(index)
		
	
	// NESTED   --------------------------
	
	private class CallAt(index: Int)
		extends OpenAiModelParser[WebSearchToolCall] with FromModelFactoryWithSchema[WebSearchToolCall]
	{
		override def typeIdentifiers: Set[String] = WebSearchToolCall.typeIdentifiers
		override def schema: ModelDeclaration = WebSearchToolCall.schema
		
		override protected def fromValidatedModel(model: Model): WebSearchToolCall =
			WebSearchToolCall(index, model("id").getString, parseStatusFrom(model))
	}
}

/**
  * Represents a call to Open AI's web search tool
  * @author Mikko Hilpinen
  * @since 04.04.2025, v1.3
  */
case class WebSearchToolCall(index: Int, id: String, state: SchrodingerState) extends OpenAiOutputElement
