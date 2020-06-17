package utopia.exodus.database.access.id

import utopia.exodus.database.factory.user.UserSettingsFactory
import utopia.exodus.database.model.user.UserSettingsModel
import utopia.vault.database.Connection
import utopia.vault.sql.{Select, Where}

/**
  * Used for accessing individual user ids
  * @author Mikko Hilpinen
  * @since 2.5.2020, v2
  */
object UserId
{
	/**
	  * @param userName User name
	  * @param connection DB Connection (implicit)
	  * @return User id matching specified user name
	  */
	def forName(userName: String)(implicit connection: Connection) =
		userIdFromSettings(UserSettingsModel.withName(userName))
	
	/**
	  * @param email User email address
	  * @param connection DB Connection (implicit)
	  * @return User id matching specified user email address
	  */
	def forEmail(email: String)(implicit connection: Connection) =
		userIdFromSettings(UserSettingsModel.withEmail(email))
	
	private def userIdFromSettings(searchModel: UserSettingsModel)(implicit connection: Connection) =
	{
		connection(Select(UserSettingsModel.table, UserSettingsModel.userIdAttName) +
			Where(searchModel.toCondition && UserSettingsFactory.nonDeprecatedCondition)).firstValue.int
	}
}
