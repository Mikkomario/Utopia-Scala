package utopia.echo.model.request

import utopia.echo.model.enumeration.JsonValueType
import utopia.echo.model.enumeration.JsonValueType.AnyType
import utopia.flow.collection.immutable.Pair

/**
  * Specifies to the LLM, what kind of property should be included in a response json object
  * @author Mikko Hilpinen
  * @since 11.07.2024, v0.1
  */
case class PropertySchema(name: String, expectation: String = "", dataType: JsonValueType = AnyType,
                          required: Boolean = false)
{
	override def toString =
		s"\"$name\": \"${ Pair(dataType.name, expectation).filter { _.nonEmpty }.mkString(" ") }${
			if (required) "" else " (optional)" }\""
}