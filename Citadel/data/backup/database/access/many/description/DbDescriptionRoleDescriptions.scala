package utopia.citadel.database.access.many.description

import utopia.citadel.database.factory.description.DescriptionLinkFactoryOld

/**
  * Used for accessing description role descriptions
  * @author Mikko Hilpinen
  * @since 13.10.2021, v1.3
  */
object DbDescriptionRoleDescriptions extends DescriptionLinksAccessOld
{
	override def factory = DescriptionLinkFactoryOld.descriptionRole
}
