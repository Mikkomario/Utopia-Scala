package utopia.metropolis.model.combined.organization

import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.combined.description.SimplyDescribed
import utopia.metropolis.model.stored.description.{DescriptionLinkOld, DescriptionRole}

/**
  * Combines task type with some or all of its descriptions
  * @author Mikko Hilpinen
  * @since 6.5.2020, v1
  * @param taskId Wrapped task's id
  * @param descriptions Various descriptions for this task
  */
case class DescribedTask(taskId: Int, override val descriptions: Set[DescriptionLinkOld])
	extends ModelConvertible with SimplyDescribed
{
	// IMPLEMENTED  -----------------------------
	
	override def toModel = Model(Vector("id" -> taskId,
		"descriptions" -> descriptions.map { _.toModel }.toVector))
	
	override protected def simpleBaseModel(roles: Iterable[DescriptionRole]) = Model("id", taskId)
}
