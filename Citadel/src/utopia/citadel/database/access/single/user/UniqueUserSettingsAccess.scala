package utopia.citadel.database.access.single.user

import java.time.Instant
import utopia.citadel.database.factory.user.UserSettingsFactory
import utopia.citadel.database.model.user.UserSettingsModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.time.Now
import utopia.metropolis.model.stored.user.UserSettings
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct UserSettings.
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait UniqueUserSettingsAccess 
	extends SingleRowModelAccess[UserSettings] 
		with DistinctModelAccess[UserSettings, Option[UserSettings], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the described user. None if no instance (or value) was found.
	  */
	def userId(implicit connection: Connection) = pullColumn(model.userIdColumn).int
	/**
	  * Name used by this user. None if no instance (or value) was found.
	  */
	def name(implicit connection: Connection) = pullColumn(model.nameColumn).string
	/**
	  * Email address of this user. None if no instance (or value) was found.
	  */
	def email(implicit connection: Connection) = pullColumn(model.emailColumn).string
	/**
	  * Time when this UserSettings was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	/**
	  * Time when these settings were replaced 
		with a more recent version (if applicable). None if no instance (or value) was found.
	  */
	def deprecatedAfter(implicit connection: Connection) = pullColumn(model.deprecatedAfterColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserSettingsModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserSettingsFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Deprecates these user settings
	  * @param connection Implicit DB Connection
	  * @return Whether any settings were affected
	  */
	def deprecate()(implicit connection: Connection) = deprecatedAfter = Now
	
	/**
	  * Updates the created of the targeted UserSettings instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any UserSettings instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	/**
	  * Updates the deprecatedAfter of the targeted UserSettings instance(s)
	  * @param newDeprecatedAfter A new deprecatedAfter to assign
	  * @return Whether any UserSettings instance was affected
	  */
	def deprecatedAfter_=(newDeprecatedAfter: Instant)(implicit connection: Connection) = 
		putColumn(model.deprecatedAfterColumn, newDeprecatedAfter)
	/**
	  * Updates the email of the targeted UserSettings instance(s)
	  * @param newEmail A new email to assign
	  * @return Whether any UserSettings instance was affected
	  */
	def email_=(newEmail: String)(implicit connection: Connection) = putColumn(model.emailColumn, newEmail)
	/**
	  * Updates the name of the targeted UserSettings instance(s)
	  * @param newName A new name to assign
	  * @return Whether any UserSettings instance was affected
	  */
	def name_=(newName: String)(implicit connection: Connection) = putColumn(model.nameColumn, newName)
	/**
	  * Updates the userId of the targeted UserSettings instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any UserSettings instance was affected
	  */
	def userId_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
}

