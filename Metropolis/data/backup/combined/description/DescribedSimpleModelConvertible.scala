package utopia.metropolis.model.combined.description

import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.metropolis.model.stored.description.DescriptionRole

/**
 * A common trait for items that can be converted to simple models when description role data is available
 * (because they contain descriptions of some sort)
 * @author Mikko Hilpinen
 * @since 30.6.2021, v1.1
 */
trait DescribedSimpleModelConvertible
{
	/**
	 * Converts this instance to a simple model
	 * @param descriptionRoles Roles of the descriptions to include
	 * @return A model
	 */
	def toSimpleModelUsing(descriptionRoles: Iterable[DescriptionRole]): Model
}
