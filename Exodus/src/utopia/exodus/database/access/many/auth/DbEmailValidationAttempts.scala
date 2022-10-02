package utopia.exodus.database.access.many.auth

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple email validation attempts at a time
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object DbEmailValidationAttempts extends ManyEmailValidationAttemptsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted email validation attempts
	  * @return An access point to email validation attempts with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbEmailValidationAttemptsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbEmailValidationAttemptsSubset(targetIds: Set[Int]) extends ManyEmailValidationAttemptsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

