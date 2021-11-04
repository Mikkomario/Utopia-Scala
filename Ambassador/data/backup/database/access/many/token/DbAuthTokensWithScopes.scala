package utopia.ambassador.database.access.many.token

import utopia.ambassador.database.factory.token.AuthTokenWithScopesFactory
import utopia.ambassador.database.model.scope.ScopeModel
import utopia.ambassador.database.model.token.AuthTokenModel
import utopia.ambassador.model.combined.token.AuthTokenWithScopes
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.view.{NonDeprecatedView, SubView}
import utopia.vault.sql.SqlExtensions._

/**
  * Used for accessing multiple authentication tokens at a time, including their scopes
  * @author Mikko Hilpinen
  * @since 19.7.2021, v1.0
  */
object DbAuthTokensWithScopes extends ManyModelAccess[AuthTokenWithScopes]
	with NonDeprecatedView[AuthTokenWithScopes]
{
	// COMPUTED -----------------------------------
	
	private def tokenModel = AuthTokenModel
	private def scopeModel = ScopeModel
	
	
	// IMPLEMENTED  -------------------------------
	
	override def factory = AuthTokenWithScopesFactory
	
	override protected def defaultOrdering = Some(factory.parentFactory.defaultOrdering)
	
	
	// OTHER    -----------------------------------
	
	/**
	  * @param userId Id of the targeted user
	  * @return An access point to that user's tokens
	  */
	def forUserWithId(userId: Int) = new DbAuthTokensForUser(userId)
	
	
	// NESTED   -----------------------------------
	
	class DbAuthTokensForUser(userId: Int) extends ManyModelAccess[AuthTokenWithScopes] with SubView
	{
		// COMPUTED -------------------------------
		
		/**
		 * @param connection Implicit DB Connection
		 * @return Ids of the services these scopes are associated with
		 */
		def linkedServiceIds(implicit connection: Connection) =
			pullColumn(scopeModel.serviceIdColumn).flatMap { _.int }.toSet
		
		
		// IMPLEMENTED  ---------------------------
		
		override protected def parent = DbAuthTokensWithScopes
		override protected def defaultOrdering = parent.defaultOrdering
		override def factory = parent.factory
		
		override def filterCondition = tokenModel.withUserId(userId).toCondition
		
		
		// OTHER    -------------------------------
		
		/**
		  * @param serviceId Id of the target service
		  * @param connection Implicit DB connection
		  * @return This user's active tokens to that service
		  */
		def forServiceWithId(serviceId: Int)(implicit connection: Connection) =
			find(scopeModel.withServiceId(serviceId).toCondition)
		
		/**
		  * @param serviceIds Ids of the targeted services
		  * @param connection Implicit DB Connection
		  * @return This user's tokens that apply to the specified services
		  */
		def forServicesWithIds(serviceIds: Iterable[Int])(implicit connection: Connection) =
			find(scopeModel.serviceIdColumn.in(serviceIds))
	}
}
