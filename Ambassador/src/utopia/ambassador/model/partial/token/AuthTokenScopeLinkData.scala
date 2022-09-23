package utopia.ambassador.model.partial.token

import utopia.flow.collection.value.typeless.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._

/**
  * Used for listing, which scopes are available based on which authentication token
  * @param tokenId Id of the token that provides access to the linked scope
  * @param scopeId Id of the scope that is accessible by using the linked token
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthTokenScopeLinkData(tokenId: Int, scopeId: Int) extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("token_id" -> tokenId, "scope_id" -> scopeId))
}

