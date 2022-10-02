package utopia.metropolis.model.stored

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Constant
import utopia.flow.generic.model.template.ModelConvertible

/**
  * A common trait for stored items that simply wrap model convertible data and add an id
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1
  */
trait StoredModelConvertible[+Data <: ModelConvertible] extends Stored[Data] with ModelConvertible
{
	override def toModel = Constant("id", id) +: data.toModel
}
