package utopia.scribe.core.model.stored

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model}
import utopia.flow.generic.model.template.ModelConvertible

/**
  * Common trait for items that are stored in the database (wrapping another item),
  * and which support model conversion
  * @author Mikko Hilpinen
  * @since 22.5.2023, v0.1
  */
trait StoredModelConvertible[+Data <: ModelConvertible] extends Stored[Data] with ModelConvertible
{
	override def toModel: Model = Constant("id", id) +: data.toModel
}
