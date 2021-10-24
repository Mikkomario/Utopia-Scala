package utopia.citadel.database.access.many.organization

import utopia.citadel.database.factory.organization.MemberRoleFactory
import utopia.metropolis.model.stored.organization.MemberRole
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyMemberRolesAccess
{
	// NESTED	--------------------
	
	private class ManyMemberRolesSubView(override val parent: ManyRowModelAccess[MemberRole], 
		override val filterCondition: Condition) 
		extends ManyMemberRolesAccess with SubView
}

/**
  * A common trait for access points which target multiple MemberRoles at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait ManyMemberRolesAccess
	extends ManyMemberRolesAccessLike[MemberRole, ManyMemberRolesAccess] with ManyRowModelAccess[MemberRole]
{
	// IMPLEMENTED	--------------------
	
	override def factory = MemberRoleFactory
	
	override protected def defaultOrdering = None
	
	override def _filter(additionalCondition: Condition): ManyMemberRolesAccess =
		new ManyMemberRolesAccess.ManyMemberRolesSubView(this, additionalCondition)
}

