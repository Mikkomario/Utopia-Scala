package utopia.citadel.database.access.many.description

import utopia.citadel.database.factory.description.CitadelLinkedDescriptionFactory
import utopia.citadel.database.model.description.CitadelDescriptionLinkModel

object DbDescriptionRoleDescriptions extends LinkedDescriptionsAccess
{
	// IMPLEMENTED	--------------------
	
	override def factory = CitadelLinkedDescriptionFactory.descriptionRole
	
	override def linkModel = CitadelDescriptionLinkModel.descriptionRole
}

