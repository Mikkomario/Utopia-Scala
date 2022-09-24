package utopia.ambassador.model.partial.token

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible

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

