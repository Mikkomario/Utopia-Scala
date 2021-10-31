package utopia.metropolis.model.stored.user

import utopia.metropolis.model.partial.user.UserData
import utopia.metropolis.model.stored.{StoredFromModelFactory, StoredModelConvertible}

object User extends StoredFromModelFactory[User, UserData]
{
	override def dataFactory = UserData
}

/**
  * Represents a User that has already been stored in the database
  * @param id id of this User in the database
  * @param data Wrapped User data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class User(id: Int, data: UserData) extends StoredModelConvertible[UserData]