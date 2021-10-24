package utopia.exodus.database.access.single.user

import utopia.exodus.database.factory.user.UserPasswordFactory
import utopia.exodus.database.model.user.UserPasswordModel
import utopia.exodus.model.stored.user.UserPassword
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual UserPasswords
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
object DbUserPassword extends SingleRowModelAccess[UserPassword] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserPasswordModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserPasswordFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted UserPassword instance
	  * @return An access point to that UserPassword
	  */
	def apply(id: Int) = DbSingleUserPassword(id)
}

