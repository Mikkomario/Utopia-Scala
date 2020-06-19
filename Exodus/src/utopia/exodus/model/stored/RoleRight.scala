package utopia.exodus.model.stored

import utopia.metropolis.model.enumeration.{TaskType, UserRole}

/**
  * Represents a stored link between a user role and a task it can perform
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  * @param id DB id of this link
  * @param role Role associated with this link
  * @param task Task associated with this link
  */
case class RoleRight(id: Int, role: UserRole, task: TaskType)
