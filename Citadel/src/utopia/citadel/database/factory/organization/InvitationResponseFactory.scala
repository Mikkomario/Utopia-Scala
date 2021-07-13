package utopia.citadel.database.factory.organization

import utopia.citadel.database.Tables
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.metropolis.model.partial.organization.InvitationResponseData
import utopia.metropolis.model.stored.organization.InvitationResponse
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading invitation responses from the database
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1.0
  */
object InvitationResponseFactory extends FromValidatedRowModelFactory[InvitationResponse]
{
	// IMPLEMENTED	--------------------------
	
	override protected def fromValidatedModel(model: Model[Constant]) = InvitationResponse(model("id").getInt,
		InvitationResponseData(model("invitationId").getInt, model("wasAccepted").getBoolean,
			model("wasBlocked").getBoolean, model("creatorId").getInt))
	
	override def table = Tables.invitationResponse
}
