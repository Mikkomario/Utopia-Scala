package utopia.exodus.database.access.single.auth

import utopia.exodus.database.factory.auth.EmailValidationPurposeFactory
import utopia.exodus.database.model.auth.EmailValidationPurposeModel
import utopia.exodus.model.stored.auth.EmailValidationPurpose
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual email validation purposes
  * @author Mikko Hilpinen
  * @since 25.10.2021, v4.0
  */
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
	  * @param id Database id of the targeted email validation purpose
	  * @return An access point to that email validation purpose
	  */
	def apply(id: Int) = DbSingleEmailValidationPurpose(id)
}

