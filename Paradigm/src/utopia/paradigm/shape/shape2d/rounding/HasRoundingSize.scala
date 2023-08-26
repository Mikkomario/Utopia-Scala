package utopia.paradigm.shape.shape2d.rounding

import utopia.paradigm.shape.shape1d.rounding.RoundingDouble
import utopia.paradigm.shape.shape2d.HasSizeLike

/**
  * A common trait for models / shapes that specify a size and use automatic rounding
  * @author Mikko Hilpinen
  * @since 26.8.2023, v1.4
  */
trait HasRoundingSize extends HasSizeLike[RoundingDouble, RoundingSize]
{
	override implicit def n: Fractional[RoundingDouble] = RoundingDouble.numeric
}