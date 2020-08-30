package utopia.exodus.database.model.user

import java.time.Instant

import utopia.exodus.database.Tables
import utopia.exodus.database.factory.user.UserSettingsFactory
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.user.UserSettingsData
import utopia.metropolis.model.stored.user.UserSettings
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

object UserSettingsModel
{
	// ATTRIBUTES	----------------------------------
	
	/**
	  * Name of the attribute for user's id
	  */
	val userIdAttName = "userId"
	
	
	// COMPUTED	--------------------------------------
	
	/**
	  * @return Table used by this model
	  */
	def table = Tables.userSettings
	
	/**
	  * @return A model that has just been marked as deprecated
	  */
	def nowDeprecated = apply(deprecatedAfter = Some(Instant.now()))
	
	
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
  * @since 2.5.2020, v1
  */
case class UserSettingsModel(id: Option[Int] = None, userId: Option[Int] = None, name: Option[String] = None,
							 email: Option[String] = None, deprecatedAfter: Option[Instant] = None)
	extends StorableWithFactory[UserSettings]
{
	import UserSettingsModel._
	
	override def factory = UserSettingsFactory
	
	override def valueProperties = Vector("id" -> id, userIdAttName -> userId, "name" -> name, "email" -> email,
		"deprecatedAfter" -> deprecatedAfter)
}
