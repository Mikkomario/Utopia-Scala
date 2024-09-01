package utopia.echo.model.request.chat.tool

import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model}

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
	lazy val toConstant = Constant(name,
		Model.from(
			"type" -> dataType,
			"description" -> description,
			"enum" -> enumValues.notEmpty
		).withoutEmptyValues)
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return Whether this is a required parameter
	  */
	def required = !optional
}