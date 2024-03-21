package utopia.logos.database.factory.word

import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.LogosTables
import utopia.logos.model.partial.word.DelimiterData
import utopia.logos.model.stored.word.Delimiter
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading delimiter data from the DB
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
object DelimiterDbFactory extends FromValidatedRowModelFactory[Delimiter]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = LogosTables.delimiter
	
	override protected def fromValidatedModel(valid: Model) = 
		Delimiter(valid("id").getInt, DelimiterData(valid("text").getString, valid("created").getInstant))
}

