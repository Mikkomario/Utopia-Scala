package utopia.exodus.database.access.single.auth

import utopia.exodus.database.factory.auth.EmailValidationAttemptFactory
import utopia.exodus.database.model.auth.EmailValidationAttemptModel
import utopia.exodus.model.stored.auth.EmailValidationAttempt
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual email validation attempts
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object DbEmailValidationAttempt 
	extends SingleRowModelAccess[EmailValidationAttempt] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = EmailValidationAttemptModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidationAttemptFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted email validation attempt
	  * @return An access point to that email validation attempt
	  */
	def apply(id: Int) = DbSingleEmailValidationAttempt(id)
}

