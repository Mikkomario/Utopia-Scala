package utopia.exodus.database.access.many.auth

import utopia.exodus.database.factory.auth.EmailValidationAttemptFactory
import utopia.exodus.database.model.auth.EmailValidationAttemptModel
import utopia.exodus.model.stored.auth.EmailValidationAttempt
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyEmailValidationAttemptsAccess
{
	// NESTED	--------------------
	
	private class ManyEmailValidationAttemptsSubView(override val parent: ManyRowModelAccess[EmailValidationAttempt], 
		override val filterCondition: Condition) 
		extends ManyEmailValidationAttemptsAccess with SubView
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
	
	override def filter(additionalCondition: Condition): ManyEmailValidationAttemptsAccess = 
		new ManyEmailValidationAttemptsAccess.ManyEmailValidationAttemptsSubView(this, additionalCondition)
}

