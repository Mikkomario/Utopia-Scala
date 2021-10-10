package utopia.citadel.database.access.many.user

import utopia.citadel.database.factory.user.UserSettingsFactory
import utopia.citadel.database.model.user.UserSettingsModel
import utopia.metropolis.model.stored.user.UserSettings
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.NonDeprecatedView

/**
  * Used for accessing multiple active user settings instances at a time
  * @author Mikko Hilpinen
  * @since 10.10.2021, v1.3
  */
object DbManyUserSettings extends ManyRowModelAccess[UserSettings] with NonDeprecatedView[UserSettings]
{
	// COMPUTED ---------------------------------
	
	private def model = UserSettingsModel
	
	
	// IMPLEMENTED  -----------------------------
	
	override def factory = UserSettingsFactory
	
	override protected def defaultOrdering = None
	
	
	// OTHER    --------------------------------
	
	/**
	  * Finds user settings that have the specified name
	  * @param name Searched username
	  * @param connection Implicit db connection
	  * @return Active user settings with that name
	  */
	def withName(name: String)(implicit connection: Connection) =
		find(model.withName(name).toCondition)
}
