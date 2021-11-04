package utopia.citadel.database.access.many.description

import utopia.citadel.database.factory.description.DescriptionLinkFactoryOld

/**
  * Used for accessing descriptions concerning user roles
  * @author Mikko Hilpinen
  * @since 13.10.2021, v1.3
  */
object DbUserRoleDescriptions extends DescriptionLinksAccessOld
{
	override def factory = DescriptionLinkFactoryOld.userRole
}
