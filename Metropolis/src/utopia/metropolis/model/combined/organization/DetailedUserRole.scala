package utopia.metropolis.model.combined.organization

import utopia.flow.collection.value.typeless.Constant
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.Extender
import utopia.metropolis.model.combined.description.DescribedSimpleModelConvertible
import utopia.metropolis.model.stored.description.DescriptionRole

/**
  * Combines described user role with allowed tasks
  * @author Mikko Hilpinen
  * @since 25.10.2021, v2.0
  */
case class DetailedUserRole(wrapped: DescribedUserRole, taskIds: Set[Int])
	extends Extender[DescribedUserRole] with DescribedSimpleModelConvertible with ModelConvertible
{
	override def toSimpleModelUsing(descriptionRoles: Iterable[DescriptionRole]) =
		wrapped.toSimpleModelUsing(descriptionRoles) + Constant("task_ids", taskIds.toVector.sorted)
	
	override def toModel = wrapped.toModel + Constant("task_ids", taskIds.toVector.sorted)
}
