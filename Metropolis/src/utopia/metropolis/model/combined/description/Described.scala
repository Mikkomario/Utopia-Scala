package utopia.metropolis.model.combined.description

import utopia.flow.datastructure.immutable.Constant
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.Extender
import utopia.metropolis.model.stored.description.DescriptionLink

/**
  * A common trait for extenders which include descriptions to items
  * @author Mikko Hilpinen
  * @since 20.5.2020, v1
  */
trait Described[+A <: ModelConvertible] extends Extender[A] with ModelConvertible
{
	// ABSTRACT	---------------------------
	
	/**
	  * @return Descriptions linked with the wrapped item
	  */
	def descriptions: Set[DescriptionLink]
	
	
	// IMPLEMENTED	-----------------------
	
	override def toModel = wrapped.toModel +
		Constant("descriptions", descriptions.map { _.toModel }.toVector)
}
