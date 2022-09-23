package utopia.vault.model.template

import utopia.flow.collection.value.typeless.Constant
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._

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
