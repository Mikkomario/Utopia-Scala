package utopia.citadel.database.access.many.user

import java.time.Instant
import utopia.citadel.database.factory.user.UserSettingsFactory
import utopia.citadel.database.model.user.UserSettingsModel
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.user.UserSettings
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition
import utopia.vault.sql.SqlExtensions._

object ManyUserSettingsAccess
{
	// NESTED	--------------------
	
	private class ManyUserSettingsSubView(override val parent: ManyRowModelAccess[UserSettings], 
		override val filterCondition: Condition) 
		extends ManyUserSettingsAccess with SubView
}

/**
  * A common trait for access points which target multiple UserSettings at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait ManyUserSettingsAccess extends ManyRowModelAccess[UserSettings] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * userIds of the accessible UserSettings
	  */
	def userIds(implicit connection: Connection) = pullColumn(model.userIdColumn)
		.flatMap { value => value.int }
	/**
	  * names of the accessible UserSettings
	  */
	def names(implicit connection: Connection) = pullColumn(model.nameColumn)
		.flatMap { value => value.string }
	/**
	  * emailAddresses of the accessible UserSettings
	  */
	def emailAddresses(implicit connection: Connection) = 
		pullColumn(model.emailColumn).flatMap { value => value.string }
	/**
	  * creationTimes of the accessible UserSettings
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	/**
	  * deprecationTimes of the accessible UserSettings
	  */
	def deprecationTimes(implicit connection: Connection) = 
		pullColumn(model.deprecatedAfterColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserSettingsModel
	
	/**
	  * @return An access point to settings that don't specify an email address
	  */
	def withoutEmailAddress = filter(model.emailColumn.isNull)
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserSettingsFactory
	
	override protected def defaultOrdering = Some(factory.defaultOrdering)
	
	override def filter(additionalCondition: Condition): ManyUserSettingsAccess = 
		new ManyUserSettingsAccess.ManyUserSettingsSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * @param userName A user name
	  * @return An access point to settings with that user name
	  */
	def withName(userName: String) = filter(model.withName(userName).toCondition)
	/**
	  * @param emailAddress An email address
	  * @return An access point to all user settings with that email address
	  */
	def withEmail(emailAddress: String) = filter(model.withEmail(emailAddress).toCondition)
	
	/**
	  * @param userIds Ids of the targeted users
	  * @return An access point to those user's settings
	  */
	def forAnyOfUsers(userIds: Iterable[Int]) = filter(model.userIdColumn in userIds)
	
	/**
	  * @param userName Searched user name
	  * @param connection Implicit DB Connection
	  * @return Whether these user settings refer to that user name at least once
	  */
	def containsName(userName: String)(implicit connection: Connection) =
		exists(model.withName(userName).toCondition)
	/**
	  * @param email Searched email address
	  * @param connection Implicit DB Connection
	  * @return Whether these settings refer to that email address at least once
	  */
	def containsEmail(email: String)(implicit connection: Connection) =
		exists(model.withEmail(email).toCondition)
	
	/**
	  * Updates the created of the targeted UserSettings instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any UserSettings instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	/**
	  * Updates the deprecatedAfter of the targeted UserSettings instance(s)
	  * @param newDeprecatedAfter A new deprecatedAfter to assign
	  * @return Whether any UserSettings instance was affected
	  */
	def deprecationTimes_=(newDeprecatedAfter: Instant)(implicit connection: Connection) = 
		putColumn(model.deprecatedAfterColumn, newDeprecatedAfter)
	/**
	  * Updates the email of the targeted UserSettings instance(s)
	  * @param newEmail A new email to assign
	  * @return Whether any UserSettings instance was affected
	  */
	def emailAddresses_=(newEmail: String)(implicit connection: Connection) = 
		putColumn(model.emailColumn, newEmail)
	/**
	  * Updates the name of the targeted UserSettings instance(s)
	  * @param newName A new name to assign
	  * @return Whether any UserSettings instance was affected
	  */
	def names_=(newName: String)(implicit connection: Connection) = putColumn(model.nameColumn, newName)
	/**
	  * Updates the userId of the targeted UserSettings instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any UserSettings instance was affected
	  */
	def userIds_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
}

