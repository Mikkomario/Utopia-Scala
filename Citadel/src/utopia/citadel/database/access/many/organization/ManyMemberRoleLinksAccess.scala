package utopia.citadel.database.access.many.organization

import utopia.citadel.database.factory.organization.MemberRoleLinkFactory
import utopia.metropolis.model.stored.organization.MemberRoleLink
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyMemberRoleLinksAccess
{
	// NESTED	--------------------
	
	private class ManyMemberRoleLinksSubView(override val parent: ManyRowModelAccess[MemberRoleLink],
	                                         override val filterCondition: Condition)
		extends ManyMemberRoleLinksAccess with SubView
}

/**
  * A common trait for access points which target multiple MemberRoles at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait ManyMemberRoleLinksAccess
	extends ManyMemberRoleLinksAccessLike[MemberRoleLink, ManyMemberRoleLinksAccess] with ManyRowModelAccess[MemberRoleLink]
{
	// COMPUTED ------------------------
	
	/**
	  * @return An access point to member role links including allowed task ids
	  */
	def withRights = globalCondition match
	{
		case Some(condition) => DbMemberRolesWithRights.filter(condition)
		case None => DbMemberRolesWithRights
	}
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = MemberRoleLinkFactory
	
	override def _filter(additionalCondition: Condition): ManyMemberRoleLinksAccess =
		new ManyMemberRoleLinksAccess.ManyMemberRoleLinksSubView(this, additionalCondition)
}

