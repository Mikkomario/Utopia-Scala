package utopia.citadel.database.access.single.organization

import utopia.citadel.database.access.many.description.DbTaskDescriptions
import utopia.citadel.database.access.single.description.{DbTaskDescription, SingleIdDescribedAccess}
import utopia.metropolis.model.combined.organization.DescribedTask
import utopia.metropolis.model.stored.organization.Task

/**
  * An access point to individual Tasks, based on their id
  * @since 2021-10-23
  */
case class DbSingleTask(id: Int) extends UniqueTaskAccess with SingleIdDescribedAccess[Task, DescribedTask]
{
	// IMPLEMENTED	--------------------
	
	override protected def describedFactory = DescribedTask
	
	override protected def manyDescriptionsAccess = DbTaskDescriptions
	
	override protected def singleDescriptionAccess = DbTaskDescription
}

