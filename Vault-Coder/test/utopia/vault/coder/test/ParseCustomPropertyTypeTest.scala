package utopia.vault.coder.test

import utopia.bunnymunch.jawn.JsonBunny
import utopia.vault.coder.model.datatype.CustomPropertyType

/**
  * Tests custom property type json-parsing
  * @author Mikko Hilpinen
  * @since 01/03/2024, v1.10.2
  */
object ParseCustomPropertyTypeTest extends App
{
	private val json = "{\n      \"type\": \"aac.scala.fuel.core.model.cached.FlightId\",\n      \"sql\": \"VARCHAR(16)\",\n      \"from_value\": {\n        \"code\": \"FlightId($v.getString)\",\n        \"references\": [\"aac.scala.fuel.core.model.cached.FlightId\"]\n      },\n      \"from_value_can_fail\": false,\n      \"option_from_value\": {\n        \"code\": \"$v.string.map(FlightId.apply)\",\n        \"references\": [\"aac.scala.fuel.core.model.cached.FlightId\"]\n      },\n      \"to_value\": {\n        \"code\": \"$v.toString\",\n        \"references\": [\"utopia.flow.generic.casting.ValueConversions._\"]\n      },\n      \"option_to_value\": {\n        \"code\": \"$v.map { _.toString }\",\n        \"references\": [\"utopia.flow.generic.casting.ValueConversions._\"]\n      },\n      \"prop_name\": \"flightIdentifier\"\n    }"
	private val model = JsonBunny(json).get.getModel
	println(model)
	assert(model.containsNonEmpty("sql"))
	println(CustomPropertyType(model).get)
}
