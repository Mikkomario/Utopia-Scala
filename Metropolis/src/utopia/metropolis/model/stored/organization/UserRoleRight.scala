package utopia.metropolis.model.stored.organization

import utopia.metropolis.model.partial.organization.UserRoleRightData
import utopia.metropolis.model.stored.StoredModelConvertible

/**
  * Represents a UserRoleRight that has already been stored in the database
  * @param id id of this UserRoleRight in the database
  * @param data Wrapped UserRoleRight data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class UserRoleRight(id: Int, data: UserRoleRightData) extends StoredModelConvertible[UserRoleRightData]

