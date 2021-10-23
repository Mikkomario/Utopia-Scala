package utopia.citadel.database.access.single.user

import utopia.citadel.database.factory.user.UserSettingsFactory
import utopia.citadel.database.model.user.UserSettingsModel
import utopia.metropolis.model.stored.user.UserSettings
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.NonDeprecatedView

/**
  * Used for accessing individual UserSettings
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbUserSettings 
	extends SingleRowModelAccess[UserSettings] with NonDeprecatedView[UserSettings] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserSettingsModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserSettingsFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted UserSettings instance
	  * @return An access point to that UserSettings
	  */
	def apply(id: Int) = DbSingleUserSettings(id)
}

