package utopia.ambassador.database.access.many.description

import utopia.ambassador.database.factory.description.AmbassadorLinkedDescriptionFactory
import utopia.ambassador.database.model.description.AmbassadorDescriptionLinkModel
import utopia.citadel.database.access.many.description.LinkedDescriptionsAccess

object DbScopeDescriptions extends LinkedDescriptionsAccess
{
	// IMPLEMENTED	--------------------
	
	override def factory = AmbassadorLinkedDescriptionFactory.scope
	
	override def linkModel = AmbassadorDescriptionLinkModel.scope
}

