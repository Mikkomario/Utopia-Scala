package utopia.exodus.database.access.many.auth

import utopia.exodus.model.stored.auth.EmailValidationAttemptOld
import utopia.flow.generic.ValueConversions._
import utopia.vault.nosql.view.NonDeprecatedView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple EmailValidationAttempts at a time
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Replaced with a new version", "v4.0")
object DbEmailValidationAttempts$Old
	extends ManyEmailValidationAttemptsAccessOld with NonDeprecatedView[EmailValidationAttemptOld]
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted EmailValidationAttempts
	  * @return An access point to EmailValidationAttempts with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbEmailValidationAttemptsSubsetOld(ids)
	
	
	// NESTED	--------------------
	
	class DbEmailValidationAttemptsSubsetOld(targetIds: Set[Int]) extends ManyEmailValidationAttemptsAccessOld
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

