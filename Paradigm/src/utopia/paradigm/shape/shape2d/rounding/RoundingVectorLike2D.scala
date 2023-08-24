package utopia.paradigm.shape.shape2d.rounding

import utopia.flow.operator.HasLength
import utopia.paradigm.shape.shape1d.rounding.RoundingDouble
import utopia.paradigm.shape.template.{FromDimensionsFactory, HasDimensions, RoundingVectorLike}

/**
  * Common trait for 2+ dimensional vectors that use rounding
  * @author Mikko Hilpinen
  * @since 24.8.2023, v1.4
  */
trait RoundingVectorLike2D[+Repr <: HasDimensions[RoundingDouble] with HasLength] extends RoundingVectorLike[Repr, Repr]
{
	override protected def fromDoublesFactory: FromDimensionsFactory[Double, Repr] = factory.forDoubles
}
