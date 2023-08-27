package utopia.paradigm.shape.template.vector

import utopia.paradigm.shape.shape1d.RoundingDouble
import utopia.paradigm.shape.template.{Dimensions, HasDimensions}

object RoundingVector extends RoundingVectorFactory[RoundingVector]
{
	// IMPLEMENTED  -----------------------
	
	override def apply(dimensions: Dimensions[RoundingDouble]): RoundingVector = _RoundingVector(dimensions)
	
	override def from(other: HasDimensions[RoundingDouble]): RoundingVector = other match {
		case r: RoundingVector => r
		case o => apply(o.dimensions)
	}
	
	
	// NESTED   ---------------------------
	
	private case class _RoundingVector(dimensions: Dimensions[RoundingDouble]) extends RoundingVector
	{
		override def self: RoundingVector = this
		override protected def factory: RoundingVectorFactory[RoundingVector] = RoundingVector
	}
}

/**
  * Common trait for double number vectors that apply rounding.
  * Implementing classes are encouraged to implement [[RoundingVectorLike]] as well in order to specify a custom
  * 'Repr' type. This trait is mainly to ease the use of RoundingVectorLike by specifying a default 'Repr' type.
  * @author Mikko Hilpinen
  * @since 24.8.2023, v1.4
  */
trait RoundingVector extends RoundingVectorLike[RoundingVector]