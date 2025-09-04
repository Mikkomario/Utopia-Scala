package utopia.echo.model

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.{ModelConvertible, ModelLike, Property}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory

import scala.util.Try

object ToolCall extends FromModelFactory[ToolCall]
{
	// IMPLEMENTED  ----------------------
	
	override def apply(model: ModelLike[Property]): Try[ToolCall] = {
		val functionModel = model("function").model.getOrElse(model)
		functionModel("name").string
			.toTry { new IllegalArgumentException(s"Tool call model $model did not specify a function name") }
			.map { name => apply(name, functionModel("arguments").getModel) }
	}
	
	
	// OTHER    -------------------------
	
	/**
	 * @param name Name of the called function
	 * @param args Arguments passed to the function (default = empty)
	 * @param callId Unique ID of this tool call. May be empty, if call IDs are not supported (default).
	 * @return A new tool call instance
	 */
	def apply(name: String, args: Model = Model.empty, callId: String = ""): ToolCall =
		_ToolCall(name, args, callId)
	
	
	// NESTED   -------------------------
	
	private case class _ToolCall(name: String, args: Model, callId: String) extends ToolCall
}

/**
  * Represents an LLM's request to utilize a tool (i.e. a custom function made available to an LLM)
  * @author Mikko Hilpinen
  * @since 31.08.2024, v1.1
  */
trait ToolCall extends ModelConvertible
{
	// ABSTRACT --------------------------
	
	/**
	 * @return Name of the called function
	 */
	def name: String
	/**
	 * @return Arguments passed to the function
	 */
	def args: Model
	/**
	 * @return Unique ID of this tool call. May be empty, if call IDs are not supported.
	 */
	def callId: String
	
	
	// IMPLEMENTED  ---------------------
	
	override def toModel: Model = Model.from("function" -> Model.from(
		"name" -> name,
		"arguments" -> args
	))
	
	override def toString = s"$name(${ args.properties.view.map { c => s"${c.name} = ${c.value}" }.mkString(", ") })"
}
