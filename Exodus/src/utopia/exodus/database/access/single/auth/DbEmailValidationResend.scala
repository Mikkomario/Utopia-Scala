package utopia.exodus.database.access.single.auth

import utopia.exodus.database.factory.auth.EmailValidationResendFactory
import utopia.exodus.database.model.auth.EmailValidationResendModel
import utopia.exodus.model.stored.auth.EmailValidationResend
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual EmailValidationResends
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
object DbEmailValidationResend 
	extends SingleRowModelAccess[EmailValidationResend] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = EmailValidationResendModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidationResendFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted EmailValidationResend instance
	  * @return An access point to that EmailValidationResend
	  */
	def apply(id: Int) = DbSingleEmailValidationResend(id)
}

