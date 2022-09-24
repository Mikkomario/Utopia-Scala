package utopia.scribe.model.combined.settings

import utopia.flow.view.template.Extender
import utopia.scribe.model.partial.settings.SettingFieldData
import utopia.scribe.model.stored.settings.{SettingField, SettingValue}

/**
  * Combines field with assignments data
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
case class SettingWithValues(field: SettingField, assignments: Vector[SettingValue]) 
	extends Extender[SettingFieldData]
{
	// COMPUTED	--------------------
	
	/**
	  * Id of this field in the database
	  */
	def id = field.id
	
	
	// IMPLEMENTED	--------------------
	
	override def wrapped = field.data
}

