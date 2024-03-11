package utopia.logos.database.factory.url

import utopia.flow.generic.model.immutable.Model
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.logos.database.EmissaryTables
import utopia.logos.model.partial.url.LinkPlacementData
import utopia.logos.model.stored.url.LinkPlacement

/**
  * Used for reading link placement data from the DB
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
object LinkPlacementFactory extends FromValidatedRowModelFactory[LinkPlacement]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = EmissaryTables.linkPlacement
	
	override protected def fromValidatedModel(valid: Model) = 
		LinkPlacement(valid("id").getInt, LinkPlacementData(valid("statementId").getInt, 
			valid("linkId").getInt, valid("orderIndex").getInt))
}

