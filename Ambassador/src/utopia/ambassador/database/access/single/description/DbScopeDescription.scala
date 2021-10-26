package utopia.ambassador.database.access.single.description

import utopia.ambassador.database.factory.description.AmbassadorLinkedDescriptionFactory
import utopia.ambassador.database.model.description.AmbassadorDescriptionLinkModel
import utopia.citadel.database.access.single.description.LinkedDescriptionAccess

object DbScopeDescription extends LinkedDescriptionAccess
{
	// IMPLEMENTED	--------------------
	
	override def factory = AmbassadorLinkedDescriptionFactory.scope
	
	override def linkModel = AmbassadorDescriptionLinkModel.scope
}

