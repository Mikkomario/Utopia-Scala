package utopia.citadel.database.factory.organization

import utopia.metropolis.model.combined.organization.UserRoleWithRights
import utopia.metropolis.model.stored.organization.{UserRole, UserRoleRight}
import utopia.vault.nosql.factory.multi.MultiCombiningFactory

/**
  * Used for reading user role data, including the allowed task ids
  * @author Mikko Hilpinen
  * @since 24.10.2021, v2.0
  */
object UserRoleWithRightsFactory extends MultiCombiningFactory[UserRoleWithRights, UserRole, UserRoleRight]
{
	override def parentFactory = UserRoleFactory
	override def childFactory = UserRoleRightFactory
	
	override def isAlwaysLinked = false
	
	override def apply(parent: UserRole, children: Seq[UserRoleRight]) =
		UserRoleWithRights(parent.id, children.map { _.taskId }.toSet)
}
