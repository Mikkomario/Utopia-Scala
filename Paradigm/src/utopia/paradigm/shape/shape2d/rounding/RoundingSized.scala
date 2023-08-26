package utopia.paradigm.shape.shape2d.rounding

import utopia.paradigm.shape.shape1d.rounding.RoundingDouble
import utopia.paradigm.shape.shape2d.SizedLike

/**
  * A common trait for models / shapes that specify a size, use automatic rounding and may be copied
  * @author Mikko Hilpinen
  * @since 26.8.2023, v1.4
  */
trait RoundingSized[+Repr] extends SizedLike[RoundingDouble, RoundingSize, Repr] with HasRoundingSize
{
	override protected def sizeFactory = RoundingSize
}