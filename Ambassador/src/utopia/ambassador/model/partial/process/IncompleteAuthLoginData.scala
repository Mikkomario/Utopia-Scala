package utopia.ambassador.model.partial.process

import java.time.Instant
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now

/**
  * Records cases where incomplete authentications are completed with the user logging in
  * @param authId Id of the incomplete authentication this login completes
  * @param userId Id of the user who logged in
  * @param created Time when this IncompleteAuthLogin was first created
  * @param wasSuccess Whether authentication tokens were successfully acquired from the 3rd party service
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class IncompleteAuthLoginData(authId: Int, userId: Int, created: Instant = Now, 
	wasSuccess: Boolean = false) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("auth_id" -> authId, "user_id" -> userId, "created" -> created, 
			"was_success" -> wasSuccess))
}

