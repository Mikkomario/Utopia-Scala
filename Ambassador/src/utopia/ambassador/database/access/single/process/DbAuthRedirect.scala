package utopia.ambassador.database.access.single.process

import utopia.ambassador.database.factory.process.AuthRedirectFactory
import utopia.ambassador.database.model.process.AuthRedirectModel
import utopia.ambassador.model.stored.process.AuthRedirect
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.single.model.distinct.{SingleIdModelAccess, UniqueModelAccess}
import utopia.vault.nosql.view.{SubView, UnconditionalView}

/**
  * Used for accessing individual authentication user redirects in the DB
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
object DbAuthRedirect extends SingleRowModelAccess[AuthRedirect] with UnconditionalView
{
	// COMPUTED ------------------------------
	
	private def model = AuthRedirectModel
	
	/**
	  * @return An access point to redirects that are still valid
	  */
	def valid = DbValidRedirect
	
	
	// IMPLEMENTED  --------------------------
	
	override def factory = AuthRedirectFactory
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param redirectId Id of the targeted redirection
	  * @return An access point to that redirection's data
	  */
	def apply(redirectId: Int) = new DbSingleRedirect(redirectId)
	
	/**
	  * @param preparationId An authentication preparation id
	  * @return An access point to the possible user redirect based on that preparation
	  */
	def forPreparationWithId(preparationId: Int) = new DbRedirectForPreparation(preparationId)
	
	
	// NESTED   ------------------------------
	
	object DbValidRedirect extends SingleRowModelAccess[AuthRedirect] with SubView
	{
		// IMPLEMENTED  ----------------------
		
		override protected def parent = DbAuthRedirect
		override def factory = parent.factory
		
		override def filterCondition = model.nonDeprecatedCondition
		
		
		// OTHER    --------------------------
		
		/**
		  * @param token A redirect access token
		  * @param connection Implicit DB connection
		  * @return A redirect attempt for that access token
		  */
		def forToken(token: String)(implicit connection: Connection) =
			find(model.withToken(token).toCondition)
	}
	
	class DbSingleRedirect(val redirectId: Int)
		extends SingleIdModelAccess[AuthRedirect](redirectId, DbAuthRedirect.factory)
	{
		/**
		  * @return An access point to the result of this redirection
		  */
		def result = DbAuthRedirectResult.forRedirectWithId(redirectId)
		
		/**
		  * @param connection Implicit DB connection
		  * @return Whether this redirection has already been closed / responded to
		  */
		def isClosed(implicit connection: Connection) = result.nonEmpty
	}
	
	class DbRedirectForPreparation(val preparationId: Int) extends UniqueModelAccess[AuthRedirect]
	{
		override def factory = DbAuthRedirect.factory
		
		override def condition = model.withPreparationId(preparationId).toCondition
	}
}
