package utopia.citadel.database.access.single.description

import utopia.citadel.database.factory.description.CitadelLinkedDescriptionFactory
import utopia.citadel.database.model.description.CitadelDescriptionLinkModel

object DbLanguageDescription extends LinkedDescriptionAccess
{
	// IMPLEMENTED	--------------------
	
	override def factory = CitadelLinkedDescriptionFactory.language
	
	override def linkModel = CitadelDescriptionLinkModel.language
}

