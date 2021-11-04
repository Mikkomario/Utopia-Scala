package utopia.citadel.database.access.single.description

import utopia.citadel.database.access.many.description.DbDescriptionRoleDescriptions
import utopia.metropolis.model.combined.description.DescribedDescriptionRole
import utopia.metropolis.model.stored.description.DescriptionRole

/**
  * An access point to individual DescriptionRoles, based on their id
  * @since 2021-10-23
  */
case class DbSingleDescriptionRole(id: Int) 
	extends UniqueDescriptionRoleAccess 
		with SingleIdDescribedAccess[DescriptionRole, DescribedDescriptionRole]
{
	// IMPLEMENTED	--------------------
	
	override protected def describedFactory = DescribedDescriptionRole
	
	override protected def manyDescriptionsAccess = DbDescriptionRoleDescriptions
	
	override protected def singleDescriptionAccess = DbDescriptionRoleDescription
}

