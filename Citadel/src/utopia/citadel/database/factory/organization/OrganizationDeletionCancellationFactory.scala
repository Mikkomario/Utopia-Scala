package utopia.citadel.database.factory.organization

import utopia.citadel.database.CitadelTables
import utopia.flow.collection.value.typeless.Model
import utopia.flow.datastructure.immutable.Model
import utopia.metropolis.model.partial.organization.OrganizationDeletionCancellationData
import utopia.metropolis.model.stored.organization.OrganizationDeletionCancellation
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading OrganizationDeletionCancellation data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object OrganizationDeletionCancellationFactory 
	extends FromValidatedRowModelFactory[OrganizationDeletionCancellation] 
		with FromRowFactoryWithTimestamps[OrganizationDeletionCancellation]
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def table = CitadelTables.organizationDeletionCancellation
	
	override def fromValidatedModel(valid: Model) =
		OrganizationDeletionCancellation(valid("id").getInt, 
			OrganizationDeletionCancellationData(valid("deletionId").getInt, valid("creatorId").int, 
			valid("created").getInstant))
}

