package utopia.citadel.database.factory.user

import utopia.citadel.database.CitadelTables
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.metropolis.model.partial.user.UserLanguageData
import utopia.metropolis.model.stored.user.UserLanguage
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading UserLanguage data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object UserLanguageFactory extends FromValidatedRowModelFactory[UserLanguage]
{
	// IMPLEMENTED	--------------------
	
	override def table = CitadelTables.userLanguage
	
	override def fromValidatedModel(valid: Model[Constant]) = 
		UserLanguage(valid("id").getInt, UserLanguageData(valid("userId").getInt, valid("languageId").getInt, 
			valid("familiarityId").getInt, valid("created").getInstant))
}

