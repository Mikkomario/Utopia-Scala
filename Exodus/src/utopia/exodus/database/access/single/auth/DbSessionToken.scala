package utopia.exodus.database.access.single.auth

import utopia.exodus.database.factory.auth.SessionTokenFactory
import utopia.exodus.database.model.auth.SessionTokenModel
import utopia.exodus.model.stored.auth.SessionToken
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.NonDeprecatedView

/**
  * Used for accessing individual SessionTokens
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
object DbSessionToken 
	extends SingleRowModelAccess[SessionToken] with NonDeprecatedView[SessionToken] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = SessionTokenModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = SessionTokenFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted SessionToken instance
	  * @return An access point to that SessionToken
	  */
	def apply(id: Int) = DbSingleSessionToken(id)
}

