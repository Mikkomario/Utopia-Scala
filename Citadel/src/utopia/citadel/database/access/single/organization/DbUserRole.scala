package utopia.citadel.database.access.single.organization

import utopia.citadel.database.factory.organization.UserRoleFactory
import utopia.citadel.database.model.organization.UserRoleModel
import utopia.metropolis.model.stored.organization.UserRole
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual UserRoles
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbUserRole extends SingleRowModelAccess[UserRole] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserRoleModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserRoleFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted UserRole instance
	  * @return An access point to that UserRole
	  */
	def apply(id: Int) = DbSingleUserRole(id)
}

