package utopia.citadel.database.access.many.description

import utopia.citadel.database.factory.description.DescriptionLinkFactoryOld
import utopia.metropolis.model.cached.LanguageIds

/**
  * Used for accessing language descriptions from the DB
  * @author Mikko Hilpinen
  * @since 13.10.2021, v1.3
  */
object DbLanguageDescriptions extends DescriptionLinksAccessOld
{
	// COMPUTED -------------------------------
	
	/**
	 * @param languageIds Language id list
	 * @return An access point to descriptions concerning those languages
	 */
	def forPreferredLanguages(implicit languageIds: LanguageIds) = apply(languageIds.toSet)
	
	
	// IMPLEMENTED  ---------------------------
	
	override def factory = DescriptionLinkFactoryOld.language
}
