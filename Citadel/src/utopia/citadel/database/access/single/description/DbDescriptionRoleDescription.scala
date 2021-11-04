package utopia.citadel.database.access.single.description

import utopia.citadel.database.factory.description.CitadelLinkedDescriptionFactory
import utopia.citadel.database.model.description.CitadelDescriptionLinkModel
import utopia.metropolis.model.enumeration.DescriptionRoleIdWrapper

object DbDescriptionRoleDescription extends LinkedDescriptionAccess
{
	// IMPLEMENTED	--------------------
	
	override def factory = CitadelLinkedDescriptionFactory.descriptionRole
	override def linkModel = CitadelDescriptionLinkModel.descriptionRole
	
	
	// OTHER    -----------------------------------
	
	/**
	  * @param role A description role
	  * @return An access point to individual descriptions of that description role
	  */
	def apply(role: DescriptionRoleIdWrapper): SingleTargetDescription = apply(role.id)
}

