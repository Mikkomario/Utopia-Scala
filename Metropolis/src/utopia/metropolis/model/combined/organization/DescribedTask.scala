package utopia.metropolis.model.combined.organization

import utopia.metropolis.model.combined.description.{DescribedFactory, DescribedWrapper, LinkedDescription, SimplyDescribed}
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.metropolis.model.stored.organization.Task

object DescribedTask extends DescribedFactory[Task, DescribedTask]

/**
  * Combines Task with the linked descriptions
  * @param task Task to wrap
  * @param descriptions Descriptions concerning the wrapped Task
  * @since 2021-10-23
  */
case class DescribedTask(task: Task, descriptions: Set[LinkedDescription])
	extends DescribedWrapper[Task] with SimplyDescribed
{
	// IMPLEMENTED	--------------------
	
	override def wrapped = task
	
	override protected def simpleBaseModel(roles: Iterable[DescriptionRole]) = wrapped.toModel
}

