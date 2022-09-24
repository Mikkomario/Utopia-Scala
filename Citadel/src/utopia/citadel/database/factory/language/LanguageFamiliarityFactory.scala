package utopia.citadel.database.factory.language

import utopia.citadel.database.CitadelTables
import utopia.flow.generic.model.immutable.Model
import utopia.metropolis.model.partial.language.LanguageFamiliarityData
import utopia.metropolis.model.stored.language.LanguageFamiliarity
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.sql.OrderBy

/**
  * Used for reading LanguageFamiliarity data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object LanguageFamiliarityFactory extends FromValidatedRowModelFactory[LanguageFamiliarity]
{
	// ATTRIBUTES ----------------------
	
	/**
	  * Default ordering used by this factory (based on order index)
	  */
	lazy val defaultOrder = OrderBy.ascending(table("orderIndex"))
	
	
	// IMPLEMENTED	--------------------
	
	override def table = CitadelTables.languageFamiliarity
	
	override def defaultOrdering = Some(defaultOrder)
	
	override def fromValidatedModel(valid: Model) =
		LanguageFamiliarity(valid("id").getInt, LanguageFamiliarityData(valid("orderIndex").getInt, 
			valid("created").getInstant))
}

