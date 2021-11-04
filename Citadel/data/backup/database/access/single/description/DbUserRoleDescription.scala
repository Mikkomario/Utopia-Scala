package utopia.citadel.database.access.single.description

import utopia.citadel.database.factory.description.DescriptionLinkFactoryOld

/**
  * Used for accessing individual user role descriptions
  * @author Mikko Hilpinen
  * @since 13.10.2021, v1.3
  */
object DbUserRoleDescription extends DescriptionLinkAccessOld
{
	override def factory = DescriptionLinkFactoryOld.userRole
}
