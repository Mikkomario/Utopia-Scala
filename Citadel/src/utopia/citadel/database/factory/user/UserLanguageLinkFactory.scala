package utopia.citadel.database.factory.user

import utopia.citadel.database.CitadelTables
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.metropolis.model.partial.user.UserLanguageLinkData
import utopia.metropolis.model.stored.user.UserLanguageLink
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading UserLanguage data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object UserLanguageLinkFactory extends FromValidatedRowModelFactory[UserLanguageLink]
{
	// IMPLEMENTED	--------------------
	
	override def table = CitadelTables.userLanguageLink
	
	override def fromValidatedModel(valid: Model) =
		UserLanguageLink(valid("id").getInt, UserLanguageLinkData(valid("userId").getInt, valid("languageId").getInt,
			valid("familiarityId").getInt, valid("created").getInstant))
}

