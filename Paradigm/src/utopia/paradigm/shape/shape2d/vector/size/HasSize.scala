package utopia.paradigm.shape.shape2d.vector.size

import scala.math.Numeric.DoubleIsFractional

/**
  * A common trait for models / shapes that specify a size
  * @author Mikko Hilpinen
  * @since 15.9.2022, v1.1
  */
trait HasSize extends HasSizeLike[Double, Size]
{
	override implicit def n: Numeric[Double] = DoubleIsFractional
}