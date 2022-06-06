package utopia.citadel.database.access.many.description

import utopia.citadel.database.factory.description.CitadelLinkedDescriptionFactory
import utopia.citadel.database.model.description.CitadelDescriptionLinkModel

@deprecated("This class will be removed in a future release", "v2.1")
object DbClientDeviceDescriptions extends LinkedDescriptionsAccess
{
	// IMPLEMENTED	--------------------
	
	override def factory = CitadelLinkedDescriptionFactory.clientDevice
	
	override def linkModel = CitadelDescriptionLinkModel.clientDevice
}

