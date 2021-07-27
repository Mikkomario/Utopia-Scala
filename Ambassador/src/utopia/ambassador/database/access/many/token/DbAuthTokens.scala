package utopia.ambassador.database.access.many.token

import utopia.ambassador.database.factory.token.AuthTokenFactory
import utopia.ambassador.database.model.token.{AuthTokenModel, TokenScopeLinkModel}
import utopia.ambassador.model.stored.token.AuthToken
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.{NonDeprecatedView, SubView}
import utopia.vault.sql.{Select, Where}

/**
  * Used for accessing multiple authentication tokens at a time
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
object DbAuthTokens extends ManyRowModelAccess[AuthToken] with NonDeprecatedView[AuthToken]
{
	// COMPUTED -----------------------------------
	
	private def model = AuthTokenModel
	private def scopeLinkModel = TokenScopeLinkModel
	
	
	// IMPLEMENTED  -------------------------------
	
	override def factory = AuthTokenFactory
	override protected def defaultOrdering = Some(factory.defaultOrdering)
	
	
	// OTHER    ------------------------------------
	
	/**
	  * @param userId A user's id
	  * @return An access point to that user's individual authentication tokens
	  */
	def forUserWithId(userId: Int) = new DbUserAuthTokens(userId)
	
	
	// NESTED   ------------------------------------
	
	class DbUserAuthTokens(val userId: Int) extends ManyRowModelAccess[AuthToken] with SubView
	{
		// COMPUTED --------------------------------
		
		/**
		  * @return An access point to these tokens, including the scopes linked with those tokens
		  */
		def withScopes = DbAuthTokensWithScopes.forUserWithId(userId)
		
		/**
		  * @param connection Implicit DB Connection
		  * @return The ids of the scopes available to this user with their current valid
		  *         access and/or refresh tokens
		  */
		def scopeIds(implicit connection: Connection) =
			connection(Select(target join scopeLinkModel.table, scopeLinkModel.scopeIdColumn) + Where(condition))
				.rowIntValues
		
		/**
		 * @param connection Implicit DB Connection
		 * @return Ids of the services these tokens are usable in
		 */
		def linkedServiceIds(implicit connection: Connection) = withScopes.linkedServiceIds
		
		
		// IMPLEMENTED  ----------------------------
		
		override protected def parent = DbAuthTokens
		
		override def factory = parent.factory
		
		override def filterCondition = model.withUserId(userId).toCondition
		
		override protected def defaultOrdering = DbAuthTokens.defaultOrdering
	}
}
