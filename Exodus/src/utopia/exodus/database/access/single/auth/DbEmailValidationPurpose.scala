package utopia.exodus.database.access.single.auth

import utopia.exodus.database.factory.auth.EmailValidationPurposeFactory
import utopia.exodus.database.model.auth.EmailValidationPurposeModel
import utopia.exodus.model.stored.auth.EmailValidationPurpose
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual EmailValidationPurposes
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
object DbEmailValidationPurpose 
	extends SingleRowModelAccess[EmailValidationPurpose] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = EmailValidationPurposeModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidationPurposeFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted EmailValidationPurpose instance
	  * @return An access point to that EmailValidationPurpose
	  */
	def apply(id: Int) = DbSingleEmailValidationPurpose(id)
}

