package utopia.citadel.database.access.single.description

import utopia.citadel.database.factory.description.CitadelLinkedDescriptionFactory
import utopia.citadel.database.model.description.CitadelDescriptionLinkModel

object DbClientDeviceDescription extends LinkedDescriptionAccess
{
	// IMPLEMENTED	--------------------
	
	override def factory = CitadelLinkedDescriptionFactory.clientDevice
	
	override def linkModel = CitadelDescriptionLinkModel.clientDevice
}

