package utopia.citadel.database.factory.language

import utopia.citadel.database.CitadelTables
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.metropolis.model.partial.language.LanguageFamiliarityData
import utopia.metropolis.model.stored.language.LanguageFamiliarity
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading LanguageFamiliarity data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object LanguageFamiliarityFactory extends FromValidatedRowModelFactory[LanguageFamiliarity]
{
	// IMPLEMENTED	--------------------
	
	override def table = CitadelTables.languageFamiliarity
	
	override def fromValidatedModel(valid: Model[Constant]) = 
		LanguageFamiliarity(valid("id").getInt, LanguageFamiliarityData(valid("orderIndex").getInt, 
			valid("created").getInstant))
}

