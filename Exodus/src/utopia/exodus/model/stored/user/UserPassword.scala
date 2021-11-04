package utopia.exodus.model.stored.user

import utopia.exodus.database.access.single.user.DbSingleUserPassword
import utopia.exodus.model.partial.user.UserPasswordData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a UserPassword that has already been stored in the database
  * @param id id of this UserPassword in the database
  * @param data Wrapped UserPassword data
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
case class UserPassword(id: Int, data: UserPasswordData) extends StoredModelConvertible[UserPasswordData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this UserPassword in the database
	  */
	def access = DbSingleUserPassword(id)
}

