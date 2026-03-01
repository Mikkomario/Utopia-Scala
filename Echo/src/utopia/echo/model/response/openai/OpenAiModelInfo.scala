package utopia.echo.model.response.openai

import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.time.Now

import java.time.Instant
import scala.util.Try

object OpenAiModelInfo extends FromModelFactoryWithSchema[OpenAiModelInfo]
{
	// ATTRIBUTES   -----------------------
	
	override val schema: ModelDeclaration = ModelDeclaration("id" -> StringType)
	
	
	// IMPLEMENTED  -----------------------
	
	override protected def fromValidatedModel(model: Model): OpenAiModelInfo =
		apply(model("id").getString, model("owned_by").getString, model("created").long match {
			case Some(value) => Try { Instant.ofEpochSecond(value) }.getOrElse(Now)
			case None => Now
		})
}

/**
 * Contains information about an LLM
 * @param name Name/identifier of this model
 * @param owner Name of the organization that owns this model
 * @param created Time when this model was created
 * @author Mikko Hilpinen
 * @since 26.02.2026, v1.5
 */
case class OpenAiModelInfo(name: String, owner: String = "", created: Instant = Now)
{
	override def toString = name
}