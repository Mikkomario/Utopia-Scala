package utopia.citadel.database.factory.language

import utopia.citadel.database.CitadelTables
import utopia.flow.collection.value.typeless.Model
import utopia.metropolis.model.partial.language.LanguageData
import utopia.metropolis.model.stored.language.Language
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading Language data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object LanguageFactory extends FromValidatedRowModelFactory[Language]
{
	// IMPLEMENTED	--------------------
	
	override def table = CitadelTables.language
	
	override def defaultOrdering = None
	
	override def fromValidatedModel(valid: Model) =
		Language(valid("id").getInt, LanguageData(valid("isoCode").getString, valid("created").getInstant))
}

