package utopia.citadel.database.access.single.organization

import utopia.citadel.database.factory.organization.MemberRoleFactory
import utopia.citadel.database.model.organization.MemberRoleModel
import utopia.metropolis.model.stored.organization.MemberRole
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.NonDeprecatedView

/**
  * Used for accessing individual MemberRoles
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbMemberRole extends SingleRowModelAccess[MemberRole] with NonDeprecatedView[MemberRole] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = MemberRoleModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = MemberRoleFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted MemberRole instance
	  * @return An access point to that MemberRole
	  */
	def apply(id: Int) = DbSingleMemberRole(id)
}

