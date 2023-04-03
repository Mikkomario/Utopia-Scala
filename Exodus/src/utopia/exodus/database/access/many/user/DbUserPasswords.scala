package utopia.exodus.database.access.many.user

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple UserPasswords at a time
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
object DbUserPasswords extends ManyUserPasswordsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted UserPasswords
	  * @return An access point to UserPasswords with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbUserPasswordsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbUserPasswordsSubset(targetIds: Set[Int]) extends ManyUserPasswordsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

