package utopia.citadel.database.access.many.user

import utopia.citadel.database.factory.user.UserSettingsFactory
import utopia.citadel.database.model.user.UserSettingsModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.stored.user.UserSettings
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, ViewFactory}
import utopia.vault.sql.Condition

import java.time.Instant

object ManyUserSettingsAccess extends ViewFactory[ManyUserSettingsAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyUserSettingsAccess = new _ManyUserSettingsAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyUserSettingsAccess(condition: Condition) extends ManyUserSettingsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple UserSettings at a time
  * @author Mikko Hilpinen
  * @since 23.10.2021
  */
trait ManyUserSettingsAccess 
	extends ManyRowModelAccess[UserSettings] with Indexed with FilterableView[ManyUserSettingsAccess]
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
	  * An access point to settings that don't specify an email address
	  */
	def withoutEmailAddress = filter(model.emailColumn.isNull)
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserSettingsModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserSettingsFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyUserSettingsAccess = ManyUserSettingsAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * @param email Searched email address
	  * @param connection Implicit DB Connection
	  * @return Whether these settings refer to that email address at least once
	  */
	def containsEmail(email: String)(implicit connection: Connection) = exists(model
		.withEmail(email).toCondition)
	
	/**
	  * @param userName Searched user name
	  * @param connection Implicit DB Connection
	  * @return Whether these user settings refer to that user name at least once
	  */
	def containsName(userName: String)(implicit connection: Connection) = 
		exists(model.withName(userName).toCondition)
	
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
	  * @param userIds Ids of the targeted users
	  * @return An access point to those user's settings
	  */
	def forAnyOfUsers(userIds: Iterable[Int]) = filter(model.userIdColumn in userIds)
	
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
	
	/**
	  * @param names A collection of user names
	  * @return User settings that use any of those names (may not cover all of them)
	  */
	def withAnyOfNames(names: Iterable[String]) = filter(model.nameColumn in names)
	
	/**
	  * @param emailAddress An email address
	  * @return An access point to all user settings with that email address
	  */
	def withEmail(emailAddress: String) = filter(model.withEmail(emailAddress).toCondition)
	
	/**
	  * @param userName A user name
	  * @return An access point to settings with that user name
	  */
	def withName(userName: String) = filter(model.withName(userName).toCondition)
}

