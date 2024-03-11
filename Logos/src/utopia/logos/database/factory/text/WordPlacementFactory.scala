package utopia.logos.database.factory.text

import utopia.flow.generic.model.immutable.Model
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.logos.database.EmissaryTables
import utopia.logos.model.partial.text.WordPlacementData
import utopia.logos.model.stored.text.WordPlacement

/**
  * Used for reading word placement data from the DB
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
object WordPlacementFactory extends FromValidatedRowModelFactory[WordPlacement]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = EmissaryTables.wordPlacement
	
	override protected def fromValidatedModel(valid: Model) = 
		WordPlacement(valid("id").getInt, WordPlacementData(valid("statementId").getInt, 
			valid("wordId").getInt, valid("orderIndex").getInt))
}

