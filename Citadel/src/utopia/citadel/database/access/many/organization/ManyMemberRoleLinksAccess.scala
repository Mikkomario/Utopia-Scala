package utopia.citadel.database.access.many.organization

import utopia.citadel.database.factory.organization.MemberRoleLinkFactory
import utopia.metropolis.model.stored.organization.MemberRoleLink
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.sql.Condition

object ManyMemberRoleLinksAccess
{
	// OTHER	--------------------
	
	def apply(condition: Condition): ManyMemberRoleLinksAccess = SubAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class SubAccess(accessCondition: Option[Condition]) extends ManyMemberRoleLinksAccess
}

/**
  * A common trait for access points which target multiple MemberRoles at a time
  * @author Mikko Hilpinen
  * @since 23.10.2021
  */
trait ManyMemberRoleLinksAccess 
	extends ManyMemberRoleLinksAccessLike[MemberRoleLink, ManyMemberRoleLinksAccess] 
		with ManyRowModelAccess[MemberRoleLink]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to member role links including allowed task ids
	  */
	def withRights = {
		accessCondition match 
		{
			case Some(condition) => DbMemberRolesWithRights.filter(condition)
			case None => DbMemberRolesWithRights
		}
	}
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = MemberRoleLinkFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyMemberRoleLinksAccess = ManyMemberRoleLinksAccess(condition)
}

