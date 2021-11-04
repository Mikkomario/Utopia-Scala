package utopia.ambassador.database.access.single.token

import utopia.ambassador.database.factory.token.AuthTokenFactory
import utopia.ambassador.database.model.token.AuthTokenModel
import utopia.ambassador.model.stored.token.AuthToken
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.NonDeprecatedView

/**
  * Used for accessing individual AuthTokens
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbAuthToken extends SingleRowModelAccess[AuthToken] with NonDeprecatedView[AuthToken] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthTokenModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthTokenFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted AuthToken instance
	  * @return An access point to that AuthToken
	  */
	def apply(id: Int) = DbSingleAuthToken(id)
}

