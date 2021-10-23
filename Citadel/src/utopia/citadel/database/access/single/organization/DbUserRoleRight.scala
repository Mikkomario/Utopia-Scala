package utopia.citadel.database.access.single.organization

import utopia.citadel.database.factory.organization.UserRoleRightFactory
import utopia.citadel.database.model.organization.UserRoleRightModel
import utopia.metropolis.model.stored.organization.UserRoleRight
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual UserRoleRights
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbUserRoleRight extends SingleRowModelAccess[UserRoleRight] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserRoleRightModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserRoleRightFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted UserRoleRight instance
	  * @return An access point to that UserRoleRight
	  */
	def apply(id: Int) = DbSingleUserRoleRight(id)
}

