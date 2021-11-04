package utopia.metropolis.model.stored.organization

import utopia.metropolis.model.combined.organization.MemberRoleWithRights
import utopia.metropolis.model.partial.organization.MemberRoleLinkData
import utopia.metropolis.model.stored.{StoredFromModelFactory, StoredModelConvertible}

object MemberRoleLink extends StoredFromModelFactory[MemberRoleLink, MemberRoleLinkData]
{
	override def dataFactory = MemberRoleLinkData
}

/**
  * Represents a MemberRole that has already been stored in the database
  * @param id id of this MemberRole in the database
  * @param data Wrapped MemberRole data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class MemberRoleLink(id: Int, data: MemberRoleLinkData) extends StoredModelConvertible[MemberRoleLinkData]
{
	/**
	  * @param taskIds Ids of the tasks allowed by this member role
	  * @return Copy of this role with allowed tasks included
	  */
	def withAccessToTasksWithIds(taskIds: Set[Int]) = MemberRoleWithRights(this, taskIds)
}

