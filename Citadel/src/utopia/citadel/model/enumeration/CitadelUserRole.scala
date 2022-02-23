package utopia.citadel.model.enumeration

import utopia.metropolis.model.enumeration.UserRoleIdWrapper

/**
  * An enumeration for the standard user roles used in the Citadel project.
  * @author Mikko Hilpinen
  * @since 25.7.2020, v1.0
  */
sealed trait CitadelUserRole extends UserRoleIdWrapper

object CitadelUserRole
{
	/**
	  * Owners have read/write access to all data in an organization
	  */
	case object Owner extends CitadelUserRole
	{
		override val id = 1
	}
	
	/**
	  * Admins have most of the same rights as the owners, except that they can't change ownership or delete
	  * an organization
	  */
	case object Admin extends CitadelUserRole
	{
		override val id = 2
	}
}