package utopia.exodus.model.partial.auth

import java.time.Instant
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now

/**
  * Used for linking scopes to tokens using many-to-many connections, 
	describing what actions each token enables
  * @param tokenId Id of the linked token
  * @param scopeId Id of the enabled scope
  * @param created Time when this token scope link was first created
  * @param isDirectlyAccessible Whether the linked scope is directly accessible using the linked token
  * @param grantsForward Whether this scope is granted to tokens that are created using this token
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class TokenScopeLinkData(tokenId: Int, scopeId: Int, created: Instant = Now, 
	isDirectlyAccessible: Boolean = false, grantsForward: Boolean = false) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("token_id" -> tokenId, "scope_id" -> scopeId, "created" -> created, 
			"is_directly_accessible" -> isDirectlyAccessible, "grants_forward" -> grantsForward))
}

