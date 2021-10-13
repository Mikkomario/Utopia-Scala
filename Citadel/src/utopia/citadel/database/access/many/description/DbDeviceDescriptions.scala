package utopia.citadel.database.access.many.description

import utopia.citadel.database.factory.description.DescriptionLinkFactory

/**
  * Used for accessing device descriptions
  * @author Mikko Hilpinen
  * @since 13.10.2021, v1.3
  */
object DbDeviceDescriptions extends DescriptionLinksAccess
{
	override def factory = DescriptionLinkFactory.device
}
