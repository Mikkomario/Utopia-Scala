package utopia.metropolis.model.stored.organization

import utopia.metropolis.model.partial.organization.MemberRoleData
import utopia.metropolis.model.stored.StoredModelConvertible

/**
  * Represents a MemberRole that has already been stored in the database
  * @param id id of this MemberRole in the database
  * @param data Wrapped MemberRole data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class MemberRole(id: Int, data: MemberRoleData) extends StoredModelConvertible[MemberRoleData]

