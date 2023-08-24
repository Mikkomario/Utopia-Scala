package utopia.paradigm.shape.shape2d.rounding

import utopia.flow.operator.EqualsBy
import utopia.paradigm.shape.shape1d.rounding.RoundingDouble
import utopia.paradigm.shape.template.{Dimensions, HasDimensions, RoundingVector, RoundingVectorFactory}

object RoundingVector2D extends RoundingVectorFactory[RoundingVector2D]
{
	// ATTRIBUTES   ---------------------
	
	/**
	  * A zero length vector
	  */
	val zero = empty
	/**
	  * A unit (1) length vector along the X-axis
	  */
	val unit = apply(1.0)
	/**
	  * An identity vector (1,1) that preserves state when used in transformations
	  */
	val identity = apply(1.0, 1.0)
	
	
	// IMPLEMENTED  ---------------------
	
	override def apply(dimensions: Dimensions[RoundingDouble]): RoundingVector2D = new RoundingVector2D(dimensions.in2D)
	
	override def from(other: HasDimensions[RoundingDouble]): RoundingVector2D = other match {
		case v: RoundingVector2D => v
		case o => apply(o.dimensions)
	}
}

/**
  * A 2-dimensional double number vector class that uses rounding
  * @author Mikko Hilpinen
  * @since 24.8.2023, v1.4
  */
class RoundingVector2D private(override val dimensions: Dimensions[RoundingDouble])
	extends RoundingVectorLike2D[RoundingVector2D] with RoundingVector with EqualsBy
{
	override def self: RoundingVector2D = this
	override protected def factory: RoundingVectorFactory[RoundingVector2D] = RoundingVector2D
	
	override protected def equalsProperties: Iterable[Any] = dimensions
}
