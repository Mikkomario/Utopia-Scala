package utopia.metropolis.model.stored

/**
  * Represents a stored link between a user role and a task it can perform
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  * @param id     DB id of this link
  * @param roleId Id of the role associated with this link
  * @param taskId Id of the task associated with this link
  */
@deprecated("Replaced with UserRoleRight", "v2.0")
case class RoleRight(id: Int, roleId: Int, taskId: Int)
