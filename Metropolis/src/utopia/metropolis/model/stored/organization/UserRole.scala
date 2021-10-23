package utopia.metropolis.model.stored.organization

import utopia.metropolis.model.partial.organization.UserRoleData
import utopia.metropolis.model.stored.StoredModelConvertible

/**
  * Represents a UserRole that has already been stored in the database
  * @param id id of this UserRole in the database
  * @param data Wrapped UserRole data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class UserRole(id: Int, data: UserRoleData) extends StoredModelConvertible[UserRoleData]

