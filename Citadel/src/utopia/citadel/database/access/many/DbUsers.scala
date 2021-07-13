package utopia.citadel.database.access.many

import utopia.citadel.database.factory.user.{UserFactory, UserSettingsFactory}
import utopia.citadel.database.model.user.UserSettingsModel
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.user.User
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.sql.SqlExtensions._

/**
  * Used for accessing multiple user's data at once
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1.0
  */
object DbUsers extends ManyModelAccess[User]
{
	// IMPLEMENTED	--------------------------
	
	override def factory = UserFactory
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
	
	override protected def defaultOrdering = None
	
	
	// COMPUTED	------------------------------
	
	private def settingsModel = UserSettingsModel
	
	private def settingsFactory = UserSettingsFactory
	
	
	// OTHER	-------------------------------
	
	/**
	  * @param userIds Targeted user ids
	  * @return An access point to those users' data
	  */
	def apply(userIds: Set[Int]) = new DbUsersSubgroup(userIds)
	
	/**
	  * Checks whether a user name is currently in use
	  * @param userName Tested user name
	  * @param connection DB Connection (implicit)
	  * @return Whether specified user name is currently in use
	  */
	@deprecated("Searches based on user name are discouraged and no longer indexed", "v1.0")
	def existsUserWithName(userName: String)(implicit connection: Connection) =
	{
		settingsFactory.exists(settingsModel.withName(userName).toCondition && settingsFactory.nonDeprecatedCondition)
	}
	/**
	  * Checks whether a user email is currently in use
	  * @param email Tested user email
	  * @param connection DB Connection (implicit)
	  * @return Whether specified email address is currently in use
	  */
	def existsUserWithEmail(email: String)(implicit connection: Connection) =
	{
		settingsFactory.exists(settingsModel.withEmail(email).toCondition && settingsFactory.nonDeprecatedCondition)
	}
	
	
	// NESTED	--------------------------------
	
	class DbUsersSubgroup(userIds: Set[Int]) extends ManyModelAccess[User]
	{
		// IMPLEMENTED	------------------------
		
		override def factory = DbUsers.factory
		
		override def globalCondition =
			Some(DbUsers.mergeCondition(factory.table.primaryColumn.get.in(userIds)))
		
		override protected def defaultOrdering = None
		
		
		// COMPUTED	----------------------------
		
		/**
		  * @param connection Database connection (implicit)
		  * @return Current settings for users with these ids
		  */
		def settings(implicit connection: Connection) = settingsFactory.getMany(
			settingsFactory.nonDeprecatedCondition && settingsModel.userIdColumn.in(userIds))
	}
}
