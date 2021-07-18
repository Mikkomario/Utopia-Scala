package utopia.ambassador.database.access.single.process

import utopia.ambassador.database.factory.process.AuthRedirectFactory
import utopia.ambassador.database.model.process.AuthRedirectModel
import utopia.ambassador.model.stored.process.AuthRedirect
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.single.model.distinct.UniqueModelAccess
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual authentication user redirects in the DB
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
object DbAuthUserRedirect extends SingleRowModelAccess[AuthRedirect] with UnconditionalView
{
	// COMPUTED ------------------------------
	
	private def model = AuthRedirectModel
	
	
	// IMPLEMENTED  --------------------------
	
	override def factory = AuthRedirectFactory
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param preparationId An authentication preparation id
	  * @return An access point to the possible user redirect based on that preparation
	  */
	def forPreparationWithId(preparationId: Int) = new DbRedirectForPreparation(preparationId)
	
	
	// NESTED   ------------------------------
	
	class DbRedirectForPreparation(val preparationId: Int) extends UniqueModelAccess[AuthRedirect]
	{
		override def factory = DbAuthUserRedirect.factory
		
		override def condition = model.withPreparationId(preparationId).toCondition
	}
}
