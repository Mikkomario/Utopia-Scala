package utopia.exodus.model.enumeration

import utopia.metropolis.model.enumeration.TaskIdWrapper

/**
  * An enumeration for different tasks handled by the standard Exodus server
  * @author Mikko Hilpinen
  * @since 25.7.2020, v1
  */
sealed trait ExodusTask extends TaskIdWrapper

object ExodusTask
{
	/**
	  * This task/right allows one to delete the whole organization
	  */
	case object DeleteOrganization extends ExodusTask
	{
		override val id = 1
	}
	
	/**
	  * This task allows one to adjust roles for other users (but not promote past own status)
	  */
	case object ChangeRoles extends ExodusTask
	{
		override val id = 2
	}
	
	/**
	  * This task allows one to invite new members to the organization
	  */
	case object InviteMembers extends ExodusTask
	{
		override val id = 3
	}
	
	/**
	  * This task allows one to update organization description
	  */
	case object DocumentOrganization extends ExodusTask
	{
		override val id = 4
	}
	
	/**
	  * This task allows one to remove members from an organization
	  */
	case object RemoveMember extends ExodusTask
	{
		override val id = 5
	}
	
	/**
	  * This task allows one to cancel a pending organization deletion
	  */
	case object CancelOrganizationDeletion extends ExodusTask
	{
		override def id = 6
	}
}
