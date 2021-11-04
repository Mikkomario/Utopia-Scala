package utopia.citadel.database.access.single.description

import utopia.citadel.database.factory.description.DescriptionLinkFactoryOld
import utopia.metropolis.model.enumeration.DescriptionRoleIdWrapper

/**
  * Used for accessing individual description role descriptions
  * @author Mikko Hilpinen
  * @since 13.10.2021, v1.3
  */
object DbDescriptionRoleDescription extends DescriptionLinkAccessOld
{
	// IMPLEMENTED  -------------------------------
	
	override def factory = DescriptionLinkFactoryOld.descriptionRole
	
	
	// OTHER    -----------------------------------
	
	/**
	  * @param role A description role
	  * @return An access point to individual descriptions of that description role
	  */
	def apply(role: DescriptionRoleIdWrapper): SingleTargetDescription = apply(role.id)
}
