package utopia.citadel.database.factory.user

import utopia.citadel.database.factory.language.LanguageFamiliarityFactory
import utopia.metropolis.model.combined.user.UserLanguageWithFamiliarity
import utopia.metropolis.model.stored.language.LanguageFamiliarity
import utopia.metropolis.model.stored.user.UserLanguage
import utopia.vault.nosql.factory.row.linked.CombiningFactory

/**
  * Used for reading UserLanguageWithFamiliaritys from the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object UserLanguageWithFamiliarityFactory 
	extends CombiningFactory[UserLanguageWithFamiliarity, UserLanguage, LanguageFamiliarity]
{
	// IMPLEMENTED	--------------------
	
	override def childFactory = LanguageFamiliarityFactory
	
	override def parentFactory = UserLanguageFactory
	
	override def apply(languageLink: UserLanguage, familiarity: LanguageFamiliarity) = 
		UserLanguageWithFamiliarity(languageLink, familiarity)
}

