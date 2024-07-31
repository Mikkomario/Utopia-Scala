package utopia.exodus.database.access.many.auth

import utopia.exodus.database.factory.auth.EmailValidationAttemptFactory
import utopia.exodus.model.stored.auth.EmailValidationAttempt
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object ManyEmailValidationAttemptsAccess extends ViewFactory[ManyEmailValidationAttemptsAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyEmailValidationAttemptsAccess = 
		new _ManyEmailValidationAttemptsAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyEmailValidationAttemptsAccess(condition: Condition) 
		extends ManyEmailValidationAttemptsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple email validation attempts at a time
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
trait ManyEmailValidationAttemptsAccess 
	extends ManyEmailValidationAttemptsAccessLike[EmailValidationAttempt, ManyEmailValidationAttemptsAccess] 
		with ManyRowModelAccess[EmailValidationAttempt]
{
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidationAttemptFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyEmailValidationAttemptsAccess = 
		ManyEmailValidationAttemptsAccess(condition)
}

