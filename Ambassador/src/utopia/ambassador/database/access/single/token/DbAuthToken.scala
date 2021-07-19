package utopia.ambassador.database.access.single.token

import utopia.ambassador.database.factory.token.AuthTokenFactory
import utopia.ambassador.database.model.token.AuthTokenModel
import utopia.ambassador.model.stored.token.AuthToken
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.single.model.distinct.SingleIdModelAccess
import utopia.vault.nosql.view.NonDeprecatedView

/**
  * Used for accessing individual authentication tokens
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
object DbAuthToken extends SingleRowModelAccess[AuthToken] with NonDeprecatedView[AuthToken]
{
	// COMPUTED -----------------------------------
	
	private def model = AuthTokenModel
	
	
	// IMPLEMENTED  -------------------------------
	
	override def factory = AuthTokenFactory
	
	
	// OTHER    -----------------------------------
	
	/**
	  * @param tokenId A token id
	  * @return An access point to that token's data
	  */
	def apply(tokenId: Int) = new DbSingleAuthToken(tokenId)
	
	
	// NESTED   -----------------------------------
	
	class DbSingleAuthToken(tokenId: Int) extends SingleIdModelAccess[AuthToken](tokenId, DbAuthToken.factory)
	{
		/**
		  * Deprecates this authentication token
		  * @param connection Implicit DB Connection
		  * @return Whether a token was changed
		  */
		def deprecate()(implicit connection: Connection) =
			model.nowDeprecated.updateWhere(condition && model.deprecationColumn.isNull) > 0
	}
}
