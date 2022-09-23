package utopia.metropolis.model.combined.description

import utopia.flow.collection.value.typeless
import utopia.flow.collection.value.typeless.Constant
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.Extender

/**
  * A common trait for extenders which include descriptions to items
  * @author Mikko Hilpinen
  * @since 20.5.2020, v1
  */
trait DescribedWrapper[+A <: ModelConvertible] extends Extender[A] with Described with ModelConvertible
{
	// IMPLEMENTED	-----------------------
	
	override def toModel = wrapped.toModel +
		typeless.Constant("descriptions", descriptions.map { _.toModel }.toVector)
}
