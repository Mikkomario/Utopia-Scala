package utopia.scribe.model.combined.settings

import utopia.flow.util.Extender
import utopia.scribe.model.partial.settings.SettingFieldData
import utopia.scribe.model.stored.settings.{SettingField, SettingValue}

/**
  * Combines field with assignment data
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
case class AssignedSetting(field: SettingField, assignment: SettingValue) extends Extender[SettingFieldData]
{
	// COMPUTED	--------------------
	
	/**
	  * Id of this field in the database
	  */
	def id = field.id
	
	
	// IMPLEMENTED	--------------------
	
	override def wrapped = field.data
}

