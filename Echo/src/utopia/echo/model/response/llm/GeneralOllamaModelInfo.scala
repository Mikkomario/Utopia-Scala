package utopia.echo.model.response.llm

import utopia.echo.model.LlmDesignator
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.{InstantType, LongType, StringType}
import utopia.flow.time.Now
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.model.template.ModelConvertible

import java.time.Instant

object GeneralOllamaModelInfo extends FromModelFactoryWithSchema[GeneralOllamaModelInfo]
{
	// ATTRIBUTES   ------------------------
	
	override lazy val schema: ModelDeclaration = ModelDeclaration(
		"name" -> StringType, "modified_at" -> InstantType, "size" -> LongType, "digest" -> StringType)
	
	
	// IMPLEMENTED  -----------------------
	
	override protected def fromValidatedModel(model: Model): GeneralOllamaModelInfo = GeneralOllamaModelInfo(
		model("name"), model("size"), model("digest"), OllamaModelDetails.parseFrom(model("details").getModel),
		model("modified_at"))
}

/**
 * Contains information returned by Ollama's list models -request (concerning an individual model)
 * @param name Full name of this model. E.g. "codellama:13b" or "llama3:latest"
 * @param sizeBytes Size of this model in bytes
 * @param digest SHA digest of this model
 * @param details Details about this model
 * @param lastModified Last time this model was modified
 * @author Mikko Hilpinen
 * @since 03.09.2024, v1.1
 */
case class GeneralOllamaModelInfo(name: String, sizeBytes: Long, digest: String, details: OllamaModelDetails,
                                  lastModified: Instant = Now)
	extends ModelConvertible
{
	// COMPUTED --------------------------
	
	/**
	 * @return An LLM designator matching this model
	 */
	def designator = LlmDesignator(name)
	
	
	// IMPLEMENTED  ----------------------
	
	override def toModel: Model = Model.from("name" -> name, "size" -> sizeBytes, "digest" -> digest,
		"modified_at" -> lastModified, "details" -> details)
}
