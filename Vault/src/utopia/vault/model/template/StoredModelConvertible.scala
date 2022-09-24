package utopia.vault.model.template

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Constant
import utopia.flow.generic.model.template.ModelConvertible

/**
  * A common trait for model classes which contain a data portion with an integer database row id and which want
  * to implement the ModelConvertible -trait.
  * @author Mikko Hilpinen
  * @since 9.9.2021, v1.9.1
  */
trait StoredModelConvertible[+Data <: ModelConvertible] extends Stored[Data, Int] with ModelConvertible
{
	override def toModel = Constant("id", id) +: data.toModel
}
