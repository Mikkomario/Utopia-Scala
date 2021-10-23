package utopia.metropolis.model.partial.description

import java.time.Instant
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now

/**
  * An enumeration for different roles or purposes a description can serve
  * @param jsonKeySingular Key used in json documents for a singular value (string) of this description role
  * @param jsonKeyPlural Key used in json documents for multiple values (array) of this description role
  * @param created Time when this DescriptionRole was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class DescriptionRoleData(jsonKeySingular: String, jsonKeyPlural: String, created: Instant = Now) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("json_key_singular" -> jsonKeySingular, "json_key_plural" -> jsonKeyPlural, 
			"created" -> created))
}

