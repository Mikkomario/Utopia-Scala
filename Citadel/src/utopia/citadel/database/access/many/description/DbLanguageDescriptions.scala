package utopia.citadel.database.access.many.description

import utopia.citadel.database.factory.description.CitadelLinkedDescriptionFactory
import utopia.citadel.database.model.description.CitadelDescriptionLinkModel
import utopia.metropolis.model.cached.LanguageIds

object DbLanguageDescriptions extends LinkedDescriptionsAccess
{
	// COMPUTED ------------------------
	
	/**
	 * @param languageIds Preferred language ids
	 * @return An access point to descriptions concerning those languages
	 */
	def forPreferredLanguages(implicit languageIds: LanguageIds) = apply(languageIds.toSet)
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = CitadelLinkedDescriptionFactory.language
	
	override def linkModel = CitadelDescriptionLinkModel.language
}

