package utopia.citadel.database.factory.description

import utopia.citadel.database.CitadelTables
import utopia.flow.collection.value.typeless.Model
import utopia.flow.datastructure.immutable.Model
import utopia.metropolis.model.partial.description.DescriptionRoleData
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading DescriptionRole data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DescriptionRoleFactory extends FromValidatedRowModelFactory[DescriptionRole]
{
	// IMPLEMENTED	--------------------
	
	override def table = CitadelTables.descriptionRole
	
	override def defaultOrdering = None
	
	override def fromValidatedModel(valid: Model) =
		DescriptionRole(valid("id").getInt, DescriptionRoleData(valid("jsonKeySingular").getString, 
			valid("jsonKeyPlural").getString, valid("created").getInstant))
}

