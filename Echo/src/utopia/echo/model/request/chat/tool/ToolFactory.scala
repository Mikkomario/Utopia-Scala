package utopia.echo.model.request.chat.tool

import utopia.echo.model.request.chat.tool.ToolFactory.schema
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.{ModelLike, Property}

import scala.util.{Success, Try}

object ToolFactory
{
	// ATTRIBUTES   ---------------------------
	
	/**
	  * A tool factory which always throws.
	  * May be used as a placeholder during development.
	  */
	lazy val notImplemented =
		apply { _ => _ => throw new NotImplementedError("Tools have not been implemented") }
	
	private lazy val schema = ModelDeclaration.empty.withChild("function", ModelDeclaration("name" -> StringType))
	
	
	// OTHER    --------------------------------
	
	/**
	  * @param functionForName A function which specifies tool functionality.
	  *                        Accepts the function name.
	  *                        Yields a function that converts a set of parameters to a string
	  *                        representing a return value.
	  * @return A new factory which uses the specified function to generate tool functionality
	  */
	def apply(functionForName: String => Model => String): ToolFactory = new _ToolFactory(functionForName)
	
	
	// NESTED   --------------------------------
	
	private class _ToolFactory(functionalityForName: String => Model => String) extends ToolFactory
	{
		override def apply(name: String, description: String, parameters: Seq[ToolParameter]): Tool =
			Tool(name, description)(parameters: _*)(functionalityForName(name))
	}
}

/**
  * A factory used for constructing tools. Used in model parsing.
  * @author Mikko Hilpinen
  * @since 15.10.2024, v1.2
  */
trait ToolFactory extends FromModelFactory[Tool]
{
	// ABSTRACT -------------------------------
	
	/**
	  * Creates a new functional tool
	  * @param name Name of this tool
	  * @param description Description of this tool
	  * @param parameters Accepted parameters
	  * @return A new tool.
	  */
	def apply(name: String, description: String, parameters: Seq[ToolParameter]): Tool
	
	
	// IMPLEMENTED  ---------------------------
	
	override def apply(model: ModelLike[Property]): Try[Tool] = schema.validate(model).flatMap { model =>
		val function = model("function").getModel
		val params = function("parameters").model match {
			case Some(params) =>
				val requiredParamNames = params("required").getVector.view.map { _.getString }.toSet
				params("properties").getModel.properties.tryMap { p =>
					ToolParameter.parseFrom(p.name, p.value.getModel, optional = !requiredParamNames.contains(p.name))
				}
				
			case None => Success(Empty)
		}
		params.map { params => apply(function("name").getString, function("description").getString, params) }
	}
}
