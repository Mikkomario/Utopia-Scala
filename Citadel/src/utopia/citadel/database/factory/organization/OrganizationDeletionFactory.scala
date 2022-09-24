package utopia.citadel.database.factory.organization

import utopia.citadel.database.CitadelTables
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.model.immutable.Model
import utopia.metropolis.model.partial.organization.OrganizationDeletionData
import utopia.metropolis.model.stored.organization.OrganizationDeletion
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading OrganizationDeletion data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object OrganizationDeletionFactory 
	extends FromValidatedRowModelFactory[OrganizationDeletion] 
		with FromRowFactoryWithTimestamps[OrganizationDeletion]
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def table = CitadelTables.organizationDeletion
	
	override def fromValidatedModel(valid: Model) =
		OrganizationDeletion(valid("id").getInt, OrganizationDeletionData(valid("organizationId").getInt, 
			valid("actualization").getInstant, valid("creatorId").getInt, valid("created").getInstant))
}

