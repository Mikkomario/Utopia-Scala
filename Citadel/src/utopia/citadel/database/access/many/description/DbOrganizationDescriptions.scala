package utopia.citadel.database.access.many.description

import utopia.citadel.database.factory.description.DescriptionLinkFactory

/**
  * Used for accessing organization descriptions
  * @author Mikko Hilpinen
  * @since 13.10.2021, v1.3
  */
object DbOrganizationDescriptions extends DescriptionLinksAccess
{
	override def factory = DescriptionLinkFactory.organization
}
