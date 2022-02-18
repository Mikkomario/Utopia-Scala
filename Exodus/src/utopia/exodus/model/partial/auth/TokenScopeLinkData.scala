package utopia.exodus.model.partial.auth

import java.time.Instant
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now

/**
  * Used for linking scopes to tokens using many-to-many connections, 
	describing what actions each token enables
  * @param tokenId Id of the linked token
  * @param scopeId Id of the enabled scope
  * @param created Time when this token scope link was first created
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class TokenScopeLinkData(tokenId: Int, scopeId: Int, created: Instant = Now) extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("token_id" -> tokenId, "scope_id" -> scopeId, "created" -> created))
}

