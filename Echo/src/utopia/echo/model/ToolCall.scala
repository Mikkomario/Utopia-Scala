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
}

/**
  * Represents an LLM's request to utilize a tool (i.e. a custom function made available to an LLM)
  * @param name Name of the called function
  * @param args Arguments passed to the function (default = empty)
  * @author Mikko Hilpinen
  * @since 31.08.2024, v1.1
  */
case class ToolCall(name: String, args: Model = Model.empty) extends ModelConvertible
{
	override def toModel: Model = Model.from("function" -> Model.from(
		"name" -> name,
		"arguments" -> args
	))
	
	override def toString = s"$name(${ args.properties.view.map { c => s"${c.name} = ${c.value}" }.mkString(", ") })"
}
