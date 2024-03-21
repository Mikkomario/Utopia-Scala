package utopia.logos.database.factory.url

import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.LogosTables
import utopia.logos.model.partial.url.LinkPlacementData
import utopia.logos.model.stored.url.LinkPlacement
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading link placement data from the DB
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
object LinkPlacementDbFactory extends FromValidatedRowModelFactory[LinkPlacement]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = LogosTables.linkPlacement
	
	override protected def fromValidatedModel(valid: Model) = 
		LinkPlacement(valid("id").getInt, LinkPlacementData(valid("statementId").getInt, 
			valid("linkId").getInt, valid("orderIndex").getInt))
}

