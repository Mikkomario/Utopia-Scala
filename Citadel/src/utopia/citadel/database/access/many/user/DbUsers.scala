package utopia.citadel.database.access.many.user

import utopia.flow.generic.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple Users at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbUsers extends ManyUsersAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted Users
	  * @return An access point to Users with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbUsersSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbUsersSubset(targetIds: Set[Int]) extends ManyUsersAccess
	{
		// COMPUTED ------------------------
		
		/**
		  * @return An access point to settings concerning these users
		  */
		def settings = DbManyUserSettings.forAnyOfUsers(targetIds)
		
		
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

