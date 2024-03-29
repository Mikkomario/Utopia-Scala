package utopia.citadel.database.factory.organization

import utopia.citadel.database.CitadelTables
import utopia.flow.generic.model.immutable.Model
import utopia.metropolis.model.partial.organization.InvitationResponseData
import utopia.metropolis.model.stored.organization.InvitationResponse
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading InvitationResponse data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object InvitationResponseFactory 
	extends FromValidatedRowModelFactory[InvitationResponse] 
		with FromRowFactoryWithTimestamps[InvitationResponse]
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def table = CitadelTables.invitationResponse
	
	override def fromValidatedModel(valid: Model) =
		InvitationResponse(valid("id").getInt, InvitationResponseData(valid("invitationId").getInt, 
			valid("message").string, valid("creatorId").int, valid("created").getInstant, 
			valid("accepted").getBoolean, valid("blocked").getBoolean))
}

