package utopia.citadel.database.factory.user

import utopia.citadel.database.factory.language.LanguageFamiliarityFactory
import utopia.metropolis.model.combined.user.UserLanguageLinkWithFamiliarity
import utopia.metropolis.model.stored.language.LanguageFamiliarity
import utopia.metropolis.model.stored.user.UserLanguageLink
import utopia.vault.nosql.factory.row.linked.CombiningFactory

/**
  * Used for reading UserLanguageWithFamiliaritys from the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object UserLanguageLinkWithFamiliarityFactory
	extends CombiningFactory[UserLanguageLinkWithFamiliarity, UserLanguageLink, LanguageFamiliarity]
{
	// COMPUTED ------------------------
	
	/**
	  * @return Default order used by this factory
	  */
	def defaultOrder = childFactory.defaultOrder
	
	
	// IMPLEMENTED	--------------------
	
	override def childFactory = LanguageFamiliarityFactory
	
	override def parentFactory = UserLanguageLinkFactory
	
	override def apply(languageLink: UserLanguageLink, familiarity: LanguageFamiliarity) =
		UserLanguageLinkWithFamiliarity(languageLink, familiarity)
}

