package utopia.echo.model.request.chat.tool

import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.model.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.flow.util.NotEmpty
import utopia.flow.util.StringExtensions._

object ToolParameter
{
	// ATTRIBUTES   ------------------
	
	private lazy val schema = ModelDeclaration("type" -> StringType)
	
	
	// OTHER    ----------------------
	
	/**
	  * Parses a tool parameter from a model
	  * @param name Name of this parameter. Expected to be specified as property name.
	  * @param model Model representing this parameter.
	  * @param optional Whether this parameter should be optional (default = false)
	  * @return Parsed parameter. Failure if "type" was missing.
	  */
	def parseFrom(name: => String, model: AnyModel, optional: => Boolean = false) =
		schema.validate(model).map { model =>
			apply(name, model("type"), model("description"), model("enum").getVector.map { _.getString }, optional)
		}
}

/**
  * Describes an input parameter for a tool
  * @param name Name of this input parameter
  * @param dataType Accepted data type as a string. E.g. "string"
  * @param description A brief description of this parameter so that the LLM can understand its function.
  * @param enumValues If this parameter accepts an enumeration as input, it lists here all the valid options.
  *                   Default = empty.
  * @param optional Whether this is an optional parameter (default = false = this is a required parameter)
  * @author Mikko Hilpinen
  * @since 31.08.2024, v1.1
  */
case class ToolParameter(name: String, dataType: String, description: String = "", enumValues: Seq[String] = Empty,
                         optional: Boolean = false)
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * A constant which represents this parameter and can be included in a function description
	  */
	lazy val toConstant = {
		val builder = OptimizedIndexedSeq.newBuilder[Constant]
		builder += Constant("type", dataType)
		description.ifNotEmpty.foreach { desc => builder += Constant("description", desc) }
		NotEmpty(enumValues).foreach { enums => builder += Constant("enum", enums) }
		
		Constant(name, Model.withConstants(builder.result()))
	}
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return Whether this is a required parameter
	  */
	def required = !optional
}