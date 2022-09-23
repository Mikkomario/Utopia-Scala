package utopia.citadel.database.factory.organization

import utopia.citadel.database.CitadelTables
import utopia.citadel.database.model.organization.InvitationModel
import utopia.flow.collection.value.typeless.Model
import utopia.flow.datastructure.immutable.Model
import utopia.metropolis.model.partial.organization.InvitationData
import utopia.metropolis.model.stored.organization.Invitation
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading Invitation data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object InvitationFactory 
	extends FromValidatedRowModelFactory[Invitation] with FromRowFactoryWithTimestamps[Invitation] 
		with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def nonDeprecatedCondition = InvitationModel.nonDeprecatedCondition
	
	override def table = CitadelTables.invitation
	
	override def fromValidatedModel(valid: Model) =
		Invitation(valid("id").getInt, InvitationData(valid("organizationId").getInt, 
			valid("startingRoleId").getInt, valid("expires").getInstant, valid("recipientId").int, 
			valid("recipientEmail").string, valid("message").string, valid("senderId").int, 
			valid("created").getInstant))
}

