package utopia.ambassador.database.access.single.token

import utopia.ambassador.database.factory.token.AuthTokenFactory
import utopia.ambassador.model.stored.token.AuthToken
import utopia.vault.nosql.access.SingleRowModelAccess

/**
  * Used for accessing individual authentication tokens
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
object DbAuthToken extends SingleRowModelAccess[AuthToken]
{
	// IMPLEMENTED  -------------------------------
	
	override def factory = AuthTokenFactory
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
}
