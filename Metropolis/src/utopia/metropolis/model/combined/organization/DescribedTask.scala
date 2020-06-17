package utopia.metropolis.model.combined.organization

import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.enumeration.TaskType
import utopia.metropolis.model.stored.description.DescriptionLink

/**
  * Combines task type with some or all of its descriptions
  * @author Mikko Hilpinen
  * @since 6.5.2020, v2
  * @param task Wrapped task
  * @param descriptions Various descriptions for this task
  */
case class DescribedTask(task: TaskType, descriptions: Set[DescriptionLink]) extends ModelConvertible
{
	override def toModel = Model(Vector("id" -> task.id,
		"descriptions" -> descriptions.map { _.toModel }.toVector))
}
