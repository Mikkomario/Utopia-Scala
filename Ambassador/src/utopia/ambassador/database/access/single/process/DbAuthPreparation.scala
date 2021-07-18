package utopia.ambassador.database.access.single.process

import utopia.ambassador.database.access.many.process.DbAuthCompletionRedirectTargets
import utopia.ambassador.database.factory.process.AuthPreparationFactory
import utopia.ambassador.database.factory.scope.ScopeFactory
import utopia.ambassador.database.model.process.AuthPreparationModel
import utopia.ambassador.database.model.scope.{AuthPreparationScopeLinkModel, ScopeModel}
import utopia.ambassador.model.stored.process.AuthPreparation
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.single.model.distinct.SingleIdModelAccess
import utopia.vault.nosql.view.NonDeprecatedView
import utopia.vault.sql.{Select, Where}

/**
  * Used for accessing information concerning individual authentication preparations
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
object DbAuthPreparation extends SingleRowModelAccess[AuthPreparation] with NonDeprecatedView[AuthPreparation]
{
	// COMPUTED ---------------------------------
	
	private def model = AuthPreparationModel
	private def scopeLinkModel = AuthPreparationScopeLinkModel
	
	
	// IMPLEMENTED  -----------------------------
	
	override def factory = AuthPreparationFactory
	
	
	// OTHER    ---------------------------------
	
	/**
	  * @param preparationId An authentication preparation id
	  * @return An access point to that preparation's data
	  */
	def apply(preparationId: Int) = new DbSinglePreparation(preparationId)
	
	/**
	  * @param token An authentication token
	  * @param connection Implicit DB Connection
	  * @return A non-expired (but possibly closed) preparation matching that token
	  */
	def forToken(token: String)(implicit connection: Connection) =
		find(model.withToken(token).toCondition)
	
	
	// NESTED   ----------------------------------
	
	class DbSinglePreparation(val preparationId: Int)
		extends SingleIdModelAccess[AuthPreparation](preparationId, DbAuthPreparation.factory)
	{
		// COMPUTED ------------------------------
		
		private def scopeFactory = ScopeFactory
		private def scopeModel = ScopeModel
		
		/**
		  * @return Redirect targets specified for this authentication attempt
		  */
		def redirectTargets = DbAuthCompletionRedirectTargets.forPreparationWithId(preparationId)
		
		/**
		  * @param connection Implicit DB Connection
		  * @return Whether this preparation has already been consumed with a user redirect
		  */
		def isClosed(implicit connection: Connection) =
			DbAuthRedirect.forPreparationWithId(preparationId).nonEmpty
		
		
		// OTHER    ------------------------------
		
		/**
		  * @param serviceId A service id
		  * @param connection Implicit DB Connection
		  * @return Scopes requested during this authentication preparation which are tied to the specified service
		  */
		def requestedScopesForServiceWithId(serviceId: Int)(implicit connection: Connection) =
		{
			val target = scopeFactory.target join scopeLinkModel.table
			val preparationCondition = scopeLinkModel.withPreparationId(preparationId).toCondition
			val serviceCondition = scopeModel.withServiceId(serviceId).toCondition
			
			scopeFactory(connection(
				Select(target, scopeFactory.table) + Where(preparationCondition && serviceCondition)))
		}
	}
}
