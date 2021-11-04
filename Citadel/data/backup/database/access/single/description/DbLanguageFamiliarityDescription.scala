package utopia.citadel.database.access.single.description

import utopia.citadel.database.factory.description.DescriptionLinkFactoryOld

/**
  * Used for accessing individual language familiarity descriptions
  * @author Mikko Hilpinen
  * @since 13.10.2021, v1.3
  */
object DbLanguageFamiliarityDescription extends DescriptionLinkAccessOld
{
	override def factory = DescriptionLinkFactoryOld.languageFamiliarity
}
