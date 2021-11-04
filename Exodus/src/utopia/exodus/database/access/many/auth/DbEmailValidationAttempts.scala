package utopia.exodus.database.access.many.auth

import utopia.exodus.model.stored.auth.EmailValidationAttempt
import utopia.flow.generic.ValueConversions._
import utopia.vault.nosql.view.NonDeprecatedView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple EmailValidationAttempts at a time
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
object DbEmailValidationAttempts 
	extends ManyEmailValidationAttemptsAccess with NonDeprecatedView[EmailValidationAttempt]
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted EmailValidationAttempts
	  * @return An access point to EmailValidationAttempts with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbEmailValidationAttemptsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbEmailValidationAttemptsSubset(targetIds: Set[Int]) extends ManyEmailValidationAttemptsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

