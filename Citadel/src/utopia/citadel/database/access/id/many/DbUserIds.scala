package utopia.citadel.database.access.id.many

import utopia.citadel.database.model.user.UserSettingsModel
import utopia.vault.database.Connection
import utopia.vault.sql.{Select, Where}

/**
  * Used for searching for multiple user ids at a time
  * @author Mikko Hilpinen
  * @since 25.9.2021, v1.2
  */
object DbUserIds
{
	private def settingsModel = UserSettingsModel
	
	/**
	  * @param userName A user name
	  * @param connection Implicit DB connection
	  * @return All user ids that match that user name
	  */
	def forName(userName: String)(implicit connection: Connection) =
		connection(Select.index(settingsModel.table) +
			Where(settingsModel.withName(userName).toCondition && settingsModel.nonDeprecatedCondition)).rowIntValues
}
