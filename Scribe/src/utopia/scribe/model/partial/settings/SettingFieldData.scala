package utopia.scribe.model.partial.settings

import java.time.Instant
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now

/**
  * Represents a field that specifies some program functionality
  * @param category Name of the broader category where this field belongs
  * @param created Time when this field was introduced
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
case class SettingFieldData(category: String, name: String, description: Option[String] = None, 
	created: Instant = Now) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("category" -> category, "name" -> name, "description" -> description, 
			"created" -> created))
}

