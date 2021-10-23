package utopia.citadel.database.access.single.user

import utopia.citadel.database.factory.user.UserFactory
import utopia.citadel.database.model.user.UserModel
import utopia.metropolis.model.stored.user.User
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual Users
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbUser extends SingleRowModelAccess[User] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted User instance
	  * @return An access point to that User
	  */
	def apply(id: Int) = DbSingleUser(id)
}

