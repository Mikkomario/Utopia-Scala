package utopia.scribe.model.partial.settings

import utopia.flow.collection.value.typeless.Value

import java.time.Instant
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now

/**
  * Represents a single setting value assignment
  * @param fieldId Id of the field this value is for
  * @param value Value assigned for this field
  * @param created Time when this value was specified
  * @param deprecatedAfter Time when this value was replaced with another
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
case class SettingValueData(fieldId: Int, value: Value = Value.empty, created: Instant = Now, 
	deprecatedAfter: Option[Instant] = None) 
	extends ModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * Whether this SettingValue has already been deprecated
	  */
	def isDeprecated = deprecatedAfter.isDefined
	
	/**
	  * Whether this SettingValue is still valid (not deprecated)
	  */
	def isValid = !isDeprecated
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("field_id" -> fieldId, "value" -> value, "created" -> created, 
			"deprecated_after" -> deprecatedAfter))
}

