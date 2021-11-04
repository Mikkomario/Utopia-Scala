package utopia.citadel.database.access.single.description

import utopia.citadel.database.factory.description.CitadelLinkedDescriptionFactory
import utopia.citadel.database.model.description.CitadelDescriptionLinkModel

object DbLanguageFamiliarityDescription extends LinkedDescriptionAccess
{
	// IMPLEMENTED	--------------------
	
	override def factory = CitadelLinkedDescriptionFactory.languageFamiliarity
	
	override def linkModel = CitadelDescriptionLinkModel.languageFamiliarity
}

