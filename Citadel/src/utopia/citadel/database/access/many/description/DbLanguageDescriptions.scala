package utopia.citadel.database.access.many.description

import utopia.citadel.database.factory.description.DescriptionLinkFactory

/**
  * Used for accessing language descriptions from the DB
  * @author Mikko Hilpinen
  * @since 13.10.2021, v1.3
  */
object DbLanguageDescriptions extends DescriptionLinksAccess
{
	override def factory = DescriptionLinkFactory.language
}
