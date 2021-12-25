package utopia.ambassador.database.access.single.process

import utopia.ambassador.database.factory.process.AuthRedirectResultFactory
import utopia.ambassador.database.model.process.AuthRedirectResultModel
import utopia.ambassador.model.stored.process.AuthRedirectResult
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{SubView, UnconditionalView}

/**
  * Used for accessing individual AuthRedirectResults
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbAuthRedirectResult 
	extends SingleRowModelAccess[AuthRedirectResult] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthRedirectResultModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthRedirectResultFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted AuthRedirectResult instance
	  * @return An access point to that AuthRedirectResult
	  */
	def apply(id: Int) = DbSingleAuthRedirectResult(id)
	
	/**
	  * @param redirectId Id of the targeted authentication redirection
	  * @return An access point to that redirection's result
	  */
	def forRedirectWithId(redirectId: Int) = new DbResultForRedirect(redirectId)
	
	
	// NESTED   ---------------------
	
	class DbResultForRedirect(redirectId: Int) extends UniqueAuthRedirectResultAccess with SubView
	{
		override protected def parent = DbAuthRedirectResult
		
		override def filterCondition = model.withRedirectId(redirectId).toCondition
	}
}

