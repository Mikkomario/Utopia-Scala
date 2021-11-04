package utopia.citadel.database.model.user

import java.time.Instant
import utopia.citadel.database.factory.user.UserSettingsFactory
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.user.UserSettingsData
import utopia.metropolis.model.stored.user.UserSettings
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter
import utopia.vault.nosql.storable.deprecation.DeprecatableAfter

/**
  * Used for constructing UserSettingsModel instances and for inserting UserSettingss to the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object UserSettingsModel 
	extends DataInserter[UserSettingsModel, UserSettings, UserSettingsData] 
		with DeprecatableAfter[UserSettingsModel]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains UserSettings userId
	  */
	val userIdAttName = "userId"
	
	/**
	  * Name of the property that contains UserSettings name
	  */
	val nameAttName = "name"
	
	/**
	  * Name of the property that contains UserSettings email
	  */
	val emailAttName = "email"
	
	/**
	  * Name of the property that contains UserSettings created
	  */
	val createdAttName = "created"
	
	/**
	  * Name of the property that contains UserSettings deprecatedAfter
	  */
	val deprecatedAfterAttName = "deprecatedAfter"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains UserSettings userId
	  */
	def userIdColumn = table(userIdAttName)
	
	/**
	  * Column that contains UserSettings name
	  */
	def nameColumn = table(nameAttName)
	
	/**
	  * Column that contains UserSettings email
	  */
	def emailColumn = table(emailAttName)
	
	/**
	  * Column that contains UserSettings created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * Column that contains UserSettings deprecatedAfter
	  */
	def deprecatedAfterColumn = table(deprecatedAfterAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = UserSettingsFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: UserSettingsData) = 
		apply(None, Some(data.userId), Some(data.name), data.email, Some(data.created), data.deprecatedAfter)
	
	override def complete(id: Value, data: UserSettingsData) = UserSettings(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this UserSettings was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param deprecatedAfter Time when these settings were replaced 
		with a more recent version (if applicable)
	  * @return A model containing only the specified deprecatedAfter
	  */
	def withDeprecatedAfter(deprecatedAfter: Instant) = apply(deprecatedAfter = Some(deprecatedAfter))
	
	/**
	  * @param email Email address of this user
	  * @return A model containing only the specified email
	  */
	def withEmail(email: String) = apply(email = Some(email))
	
	/**
	  * @param id A UserSettings id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param name Name used by this user
	  * @return A model containing only the specified name
	  */
	def withName(name: String) = apply(name = Some(name))
	
	/**
	  * @param userId Id of the described user
	  * @return A model containing only the specified userId
	  */
	def withUserId(userId: Int) = apply(userId = Some(userId))
}

/**
  * Used for interacting with UserSettings in the database
  * @param id UserSettings database id
  * @param userId Id of the described user
  * @param name Name used by this user
  * @param email Email address of this user
  * @param created Time when this UserSettings was first created
  * @param deprecatedAfter Time when these settings were replaced with a more recent version (if applicable)
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class UserSettingsModel(id: Option[Int] = None, userId: Option[Int] = None, name: Option[String] = None, 
	email: Option[String] = None, created: Option[Instant] = None, deprecatedAfter: Option[Instant] = None) 
	extends StorableWithFactory[UserSettings]
{
	// IMPLEMENTED	--------------------
	
	override def factory = UserSettingsModel.factory
	
	override def valueProperties = 
	{
		import UserSettingsModel._
		Vector("id" -> id, userIdAttName -> userId, nameAttName -> name, emailAttName -> email, 
			createdAttName -> created, deprecatedAfterAttName -> deprecatedAfter)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param deprecatedAfter A new deprecatedAfter
	  * @return A new copy of this model with the specified deprecatedAfter
	  */
	def withDeprecatedAfter(deprecatedAfter: Instant) = copy(deprecatedAfter = Some(deprecatedAfter))
	
	/**
	  * @param email A new email
	  * @return A new copy of this model with the specified email
	  */
	def withEmail(email: String) = copy(email = Some(email))
	
	/**
	  * @param name A new name
	  * @return A new copy of this model with the specified name
	  */
	def withName(name: String) = copy(name = Some(name))
	
	/**
	  * @param userId A new userId
	  * @return A new copy of this model with the specified userId
	  */
	def withUserId(userId: Int) = copy(userId = Some(userId))
}

