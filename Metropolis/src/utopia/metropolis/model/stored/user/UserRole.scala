package utopia.metropolis.model.stored.user

/**
  * Represents a user role registered to DB
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  * @param id Id of this role in the DB
  * @param allowedTaskIds Ids of the tasks that user having this role has access to
  */
@deprecated("Replaced with UserRole in organization package and RoleWithRights", "v1")
case class UserRole(id: Int, allowedTaskIds: Set[Int])
