package utopia.citadel.database.access.single.description

import utopia.citadel.database.factory.description.DescriptionLinkFactory

/**
  * Used for accessing individual task descriptions
  * @author Mikko Hilpinen
  * @since 13.10.2021, v1.3
  */
object DbTaskDescription extends DescriptionLinkAccess
{
	override def factory = DescriptionLinkFactory.task
}
