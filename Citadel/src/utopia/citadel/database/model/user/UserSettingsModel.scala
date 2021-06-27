package utopia.citadel.database.model.user

import java.time.Instant
import utopia.citadel.database.Tables
import utopia.citadel.database.factory.user.UserSettingsFactory
import utopia.citadel.database.model.DeprecatableAfter
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.user.UserSettingsData
import utopia.metropolis.model.stored.user.UserSettings
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

object UserSettingsModel extends DeprecatableAfter[UserSettingsModel]
{
	// ATTRIBUTES	----------------------------------
	
	/**
	  * Name of the attribute for user's id
	  */
	val userIdAttName = "userId"
	
	
	// COMPUTED	--------------------------------------
	
	/**
	  * @return Column that contains user id information
	  */
	def userIdColumn = table(userIdAttName)
	
	
	// IMPLEMENTED  ----------------------------------
	
	override def table = Tables.userSettings
	
	override def withDeprecatedAfter(deprecation: Instant) =
		apply(deprecatedAfter = Some(deprecation))
	
	
	// OTHER	--------------------------------------
	
	/**
	  * @param userId Id of the described user
	  * @return A model with only user id set
	  */
	def withUserId(userId: Int) = apply(userId = Some(userId))
	
	/**
	  * @param userName User name
	  * @return a model with only user name set
	  */
	def withName(userName: String) = apply(name = Some(userName))
	
	/**
	  * @param email Email
	  * @return A model with only email set
	  */
	def withEmail(email: String) = apply(email = Some(email))
	
	/**
	  * Inserts a new set of user settings to the DB (please deprecate old version first)
	  * @param userId Id of the described user
	  * @param data New user settings data
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted data
	  */
	def insert(userId: Int, data: UserSettingsData)(implicit connection: Connection) =
	{
		val newId = apply(None, Some(userId), Some(data.name), Some(data.email)).insert().getInt
		UserSettings(newId, userId, data)
	}
}

/**
  * Used for interacting with user settings in DB
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1.0
  */
case class UserSettingsModel(id: Option[Int] = None, userId: Option[Int] = None, name: Option[String] = None,
							 email: Option[String] = None, deprecatedAfter: Option[Instant] = None)
	extends StorableWithFactory[UserSettings]
{
	import UserSettingsModel._
	
	override def factory = UserSettingsFactory
	
	override def valueProperties = Vector("id" -> id, userIdAttName -> userId, "name" -> name, "email" -> email,
		deprecationAttName -> deprecatedAfter)
}
