package utopia.metropolis.model.combined.organization

import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.description.DescriptionLink

/**
  * Combines task type with some or all of its descriptions
  * @author Mikko Hilpinen
  * @since 6.5.2020, v1
  * @param taskId Wrapped task's id
  * @param descriptions Various descriptions for this task
  */
case class DescribedTask(taskId: Int, descriptions: Set[DescriptionLink]) extends ModelConvertible
{
	override def toModel = Model(Vector("id" -> taskId,
		"descriptions" -> descriptions.map { _.toModel }.toVector))
}
