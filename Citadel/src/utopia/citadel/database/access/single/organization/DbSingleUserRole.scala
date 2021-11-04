package utopia.citadel.database.access.single.organization

import utopia.citadel.database.access.many.description.DbUserRoleDescriptions
import utopia.citadel.database.access.single.description.{DbUserRoleDescription, SingleIdDescribedAccess}
import utopia.metropolis.model.combined.organization.DescribedUserRole
import utopia.metropolis.model.stored.organization.UserRole

/**
  * An access point to individual UserRoles, based on their id
  * @since 2021-10-23
  */
case class DbSingleUserRole(id: Int) 
	extends UniqueUserRoleAccess with SingleIdDescribedAccess[UserRole, DescribedUserRole]
{
	// IMPLEMENTED	--------------------
	
	override protected def describedFactory = DescribedUserRole
	
	override protected def manyDescriptionsAccess = DbUserRoleDescriptions
	
	override protected def singleDescriptionAccess = DbUserRoleDescription
}

