package utopia.echo.model.response.openai.toolcall

import utopia.annex.model.manifest.SchrodingerState
import utopia.echo.model.response.openai.{OpenAiModelParser, OpenAiOutputElementFromModelFactory}
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.{ModelLike, Property}

import scala.util.Try

object OpenAiFunctionToolCall extends OpenAiOutputElementFromModelFactory[OpenAiFunctionToolCall]
{
	// ATTRIBUTES   -------------------------
	
	override lazy val typeIdentifiers: Set[String] = Set("function_call")
	private lazy val schema = ModelDeclaration("id" -> StringType, "call_id" -> StringType, "name" -> StringType)
	
	
	// IMPLEMENTED    -----------------------
	
	/**
	  * @param index Index at which the model appears
	  * @return An interface for parsing a function call model at that index
	  */
	override def at(index: Int): OpenAiModelParser[OpenAiFunctionToolCall] = new FunctionAt(index)
		
	
	// NESTED   -----------------------------
	
	private class FunctionAt(index: Int) extends OpenAiModelParser[OpenAiFunctionToolCall]
	{
		override def typeIdentifiers: Set[String] = OpenAiFunctionToolCall.typeIdentifiers
		
		override def apply(model: ModelLike[Property]): Try[OpenAiFunctionToolCall] =
			schema.validate(model).flatMap { model =>
				model("arguments").tryModel.map { args =>
					OpenAiFunctionToolCall(index, model("id"), model("call_id"), model("name"), args,
						parseStatusFrom(model))
				}
			}
	}
}

/**
  * Represents a call to a custom function tool. Used in Open AI's response models.
  * @author Mikko Hilpinen
  * @since 04.04.2025, v1.3
  * @param index Index of this function call in the output sequence
  * @param id Unique id of this function call
  * @param callId Id of this function call in the model
  * @param name Name of the called function
  * @param arguments Arguments passed to the function
  * @param state State of this tool call
  */
case class OpenAiFunctionToolCall(index: Int, id: String, callId: String, name: String, arguments: Model = Model.empty,
                                  state: SchrodingerState)
	extends OpenAiOutputElement
