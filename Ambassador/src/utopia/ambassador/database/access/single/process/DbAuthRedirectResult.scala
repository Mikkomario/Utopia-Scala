package utopia.ambassador.database.access.single.process

import utopia.ambassador.database.factory.process.AuthRedirectResultFactory
import utopia.ambassador.database.model.process.AuthRedirectResultModel
import utopia.ambassador.model.stored.process.AuthRedirectResult
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.single.model.distinct.UniqueModelAccess
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual authentication results
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
object DbAuthRedirectResult extends SingleRowModelAccess[AuthRedirectResult] with UnconditionalView
{
	// COMPUTED -------------------------------
	
	private def model = AuthRedirectResultModel
	
	
	// IMPLEMENTED  ---------------------------
	
	override def factory = AuthRedirectResultFactory
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param redirectId Id of the user redirection
	  * @return An access point to the result of that redirection
	  */
	def forRedirectWithId(redirectId: Int) = new DbResultForRedirect(redirectId)
	
	
	// NESTED   -------------------------------
	
	class DbResultForRedirect(redirectId: Int) extends UniqueModelAccess[AuthRedirectResult]
	{
		override def factory = DbAuthRedirectResult.factory
		
		override def condition = model.withRedirectId(redirectId).toCondition
	}
}
