package utopia.citadel.database.access.single.description

import utopia.citadel.database.factory.description.DescriptionLinkFactory

/**
  * Used for accessing individual language descriptions
  * @author Mikko Hilpinen
  * @since 13.10.2021, v1.3
  */
object DbLanguageDescription extends DescriptionLinkAccess
{
	override def factory = DescriptionLinkFactory.language
}
