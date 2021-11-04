package utopia.citadel.database.access.many.description

import utopia.citadel.database.factory.description.CitadelLinkedDescriptionFactory
import utopia.citadel.database.model.description.CitadelDescriptionLinkModel

object DbUserRoleDescriptions extends LinkedDescriptionsAccess
{
	// IMPLEMENTED	--------------------
	
	override def factory = CitadelLinkedDescriptionFactory.userRole
	
	override def linkModel = CitadelDescriptionLinkModel.userRole
}

