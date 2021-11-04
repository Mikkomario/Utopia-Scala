package utopia.citadel.database.access.many.description

import utopia.citadel.database.factory.description.DescriptionLinkFactoryOld

/**
  * Used for accessing language familiarity descriptions in the DB
  * @author Mikko Hilpinen
  * @since 13.10.2021, v1.3
  */
object DbLanguageFamiliarityDescriptions extends DescriptionLinksAccessOld
{
	override def factory = DescriptionLinkFactoryOld.languageFamiliarity
}
