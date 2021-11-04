package utopia.citadel.database.access.single.description

import utopia.citadel.database.factory.description.DescriptionLinkFactoryOld

/**
  * Used for accessing individual task descriptions
  * @author Mikko Hilpinen
  * @since 13.10.2021, v1.3
  */
object DbTaskDescription extends DescriptionLinkAccessOld
{
	override def factory = DescriptionLinkFactoryOld.task
}
