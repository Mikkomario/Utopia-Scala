package utopia.logos.database.factory.word

import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.LogosTables
import utopia.logos.model.enumeration.DisplayStyle
import utopia.logos.model.partial.word.WordPlacementData
import utopia.logos.model.stored.word.WordPlacement
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading word placement data from the DB
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
@deprecated("Replaced with a new version", "v0.3")
object WordPlacementDbFactory extends FromValidatedRowModelFactory[WordPlacement]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = LogosTables.wordPlacement
	
	override protected def fromValidatedModel(valid: Model) = 
		WordPlacement(valid("id").getInt, WordPlacementData(valid("statementId").getInt, 
			valid("wordId").getInt, valid("orderIndex").getInt, DisplayStyle.fromValue(valid("styleId"))))
}

