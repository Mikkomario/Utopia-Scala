package utopia.metropolis.model.enumeration

import utopia.flow.util.CollectionExtensions._
import utopia.metropolis.model.error.NoSuchTypeException

/**
  * An enumeration for various task types that users may or may not have access to
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
@deprecated("On Exodus side, replaced with StandardTask. Elsewhere use Exodus server result list.", "v1")
sealed trait TaskType
{
	/**
	  * @return DB row id associated with this task type
	  */
	def id: Int
}

@deprecated("On Exodus side, replaced with StandardTask. Elsewhere use Exodus server result list.", "v1")
object TaskType
{
	/**
	  * This task/right allows one to delete the whole organization
	  */
	@deprecated("On Exodus side, replaced with StandardTask. Elsewhere use Exodus server result list.", "v1")
	case object DeleteOrganization extends TaskType
	{
		override val id = 1
	}
	
	/**
	  * This task allows one to adjust roles for other users (but not promote past own status)
	  */
	@deprecated("On Exodus side, replaced with StandardTask. Elsewhere use Exodus server result list.", "v1")
	case object ChangeRoles extends TaskType
	{
		override val id = 2
	}
	
	/**
	  * This task allows one to invite new members to the organization
	  */
	@deprecated("On Exodus side, replaced with StandardTask. Elsewhere use Exodus server result list.", "v1")
	case object InviteMembers extends TaskType
	{
		override val id = 3
	}
	
	/**
	  * This task allows one to update organization description
	  */
	@deprecated("On Exodus side, replaced with StandardTask. Elsewhere use Exodus server result list.", "v1")
	case object DocumentOrganization extends TaskType
	{
		override val id = 4
	}
	
	/**
	  * This task allows one to remove members from an organization
	  */
	@deprecated("On Exodus side, replaced with StandardTask. Elsewhere use Exodus server result list.", "v1")
	case object RemoveMember extends TaskType
	{
		override val id = 5
	}
	
	/**
	  * This task allows one to cancel a pending organization deletion
	  */
	@deprecated("On Exodus side, replaced with StandardTask. Elsewhere use Exodus server result list.", "v1")
	case object CancelOrganizationDeletion extends TaskType
	{
		override def id = 6
	}
	
	/**
	  * All task values
	  */
	val values = Vector[TaskType](DeleteOrganization, ChangeRoles, InviteMembers, DocumentOrganization, RemoveMember)
	
	/**
	  * @param taskId A task id
	  * @return A task matching specified id. Failure if there wasn't a task for specified id
	  */
	def forId(taskId: Int) = values.find { _.id == taskId }.toTry { new NoSuchTypeException(
		s"There doesn't exist a task type with id $taskId") }
}
