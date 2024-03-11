package utopia.logos.database.factory.text

import utopia.flow.generic.model.immutable.Model
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.logos.database.EmissaryTables
import utopia.logos.model.partial.text.DelimiterData
import utopia.logos.model.stored.text.Delimiter

/**
  * Used for reading delimiter data from the DB
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
object DelimiterFactory extends FromValidatedRowModelFactory[Delimiter]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = EmissaryTables.delimiter
	
	override protected def fromValidatedModel(valid: Model) = 
		Delimiter(valid("id").getInt, DelimiterData(valid("text").getString, valid("created").getInstant))
}

