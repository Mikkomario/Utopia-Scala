package utopia.citadel.database.access.single.organization

import utopia.citadel.database.factory.organization.MemberRoleLinkFactory
import utopia.citadel.database.model.organization.MemberRoleLinkModel
import utopia.metropolis.model.stored.organization.MemberRoleLink
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.NonDeprecatedView

/**
  * Used for accessing individual MemberRoles
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbMemberRoleLink extends SingleRowModelAccess[MemberRoleLink] with NonDeprecatedView[MemberRoleLink] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = MemberRoleLinkModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = MemberRoleLinkFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted MemberRole instance
	  * @return An access point to that MemberRole
	  */
	def apply(id: Int) = DbSingleMemberRoleLink(id)
}

