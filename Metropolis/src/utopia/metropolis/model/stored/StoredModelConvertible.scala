package utopia.metropolis.model.stored

import utopia.flow.datastructure.immutable.Constant
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._

/**
  * A common trait for stored items that simply wrap model convertible data and add an id
  * @author Mikko Hilpinen
  * @since 16.5.2020, v2
  */
trait StoredModelConvertible[+Data <: ModelConvertible] extends Stored[Data] with ModelConvertible
{
	override def toModel = data.toModel + Constant("id", id)
}
