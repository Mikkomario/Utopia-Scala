package utopia.citadel.database.factory.organization

import utopia.citadel.database.CitadelTables
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.metropolis.model.partial.organization.OrganizationData
import utopia.metropolis.model.stored.organization.Organization
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading Organization data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object OrganizationFactory extends FromValidatedRowModelFactory[Organization]
{
	// IMPLEMENTED	--------------------
	
	override def table = CitadelTables.organization
	
	override def fromValidatedModel(valid: Model[Constant]) = 
		Organization(valid("id").getInt, OrganizationData(valid("creatorId").int, 
			valid("created").getInstant))
}

