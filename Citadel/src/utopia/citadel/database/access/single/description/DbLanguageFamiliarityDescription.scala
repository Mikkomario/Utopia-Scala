package utopia.citadel.database.access.single.description

import utopia.citadel.database.factory.description.DescriptionLinkFactory

/**
  * Used for accessing individual language familiarity descriptions
  * @author Mikko Hilpinen
  * @since 13.10.2021, v1.3
  */
object DbLanguageFamiliarityDescription extends DescriptionLinkAccess
{
	override def factory = DescriptionLinkFactory.languageFamiliarity
}
