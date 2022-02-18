package utopia.exodus.database.access.single.auth

import utopia.exodus.database.factory.auth.TokenFactory
import utopia.exodus.database.model.auth.TokenModel
import utopia.exodus.model.stored.auth.Token
import utopia.flow.util.Sha256Hasher
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{NonDeprecatedView, SubView}

/**
  * Used for accessing individual tokens
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object DbToken extends SingleRowModelAccess[Token] with NonDeprecatedView[Token] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = TokenModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = TokenFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted token
	  * @return An access point to that token
	  */
	def apply(id: Int) = DbSingleToken(id)
	
	/**
	  * @param token A token string (not hashed)
	  * @return An access point to a matching valid token
	  */
	def matching(token: String) = new DbTokenMatch(token)
	
	
	// NESTED   --------------------
	
	class DbTokenMatch(tokenString: String) extends UniqueTokenAccess with SubView
	{
		// ATTRIBUTES   ------------
		
		// Tokens are hashed using SHA256 algorithm
		override lazy val filterCondition = model.withHash(Sha256Hasher(tokenString)).toCondition
		
		
		// IMPLEMENTED  ------------
		
		override protected def parent = DbToken
	}
}

