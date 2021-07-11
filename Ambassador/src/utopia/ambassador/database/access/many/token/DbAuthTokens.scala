package utopia.ambassador.database.access.many.token

import utopia.ambassador.database.factory.token.AuthTokenFactory
import utopia.ambassador.database.model.token.{AuthTokenModel, TokenScopeLinkModel}
import utopia.ambassador.model.stored.token.AuthToken
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.sql.{Select, Where}

/**
  * Used for accessing multiple authentication tokens at a time
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
object DbAuthTokens extends ManyRowModelAccess[AuthToken]
{
	// COMPUTED -----------------------------------
	
	private def model = AuthTokenModel
	private def scopeLinkModel = TokenScopeLinkModel
	
	
	// IMPLEMENTED  -------------------------------
	
	override def factory = AuthTokenFactory
	override protected def defaultOrdering = Some(factory.defaultOrdering)
	override def globalCondition = Some(factory.nonDeprecatedCondition)
	
	
	// OTHER    ------------------------------------
	
	/**
	  * @param userId A user's id
	  * @return An access point to that user's individual authentication tokens
	  */
	def forUserWithId(userId: Int) = new DbUserAuthTokens(userId)
	
	
	// NESTED   ------------------------------------
	
	class DbUserAuthTokens(val userId: Int) extends ManyRowModelAccess[AuthToken]
	{
		// COMPUTED --------------------------------
		
		private def condition = DbAuthTokens.mergeCondition(model.withUserId(userId))
		
		/**
		  * @param connection Implicit DB Connection
		  * @return The ids of the scopes available to this user with their current valid
		  *         access and/or refresh tokens
		  */
		def scopeIds(implicit connection: Connection) =
			connection(Select(target join scopeLinkModel.table, scopeLinkModel.scopeIdColumn) + Where(condition))
				.rowIntValues
		
		
		// IMPLEMENTED  ----------------------------
		
		override def factory = DbAuthTokens.factory
		
		override protected def defaultOrdering = DbAuthTokens.defaultOrdering
		override def globalCondition = Some(DbAuthTokens.mergeCondition(model.withUserId(userId)))
	}
}
