package utopia.paradigm.shape.shape2d.vector.size

import utopia.paradigm.shape.template.vector.NumericVectorFactory

/**
  * A common trait for models / shapes that specify a size and may be copied
  * @author Mikko Hilpinen
  * @since 15.9.2022, v1.1
  */
trait Sized[+Repr] extends SizedLike[Double, Size, Repr] with HasSize
{
	override protected def sizeFactory: NumericVectorFactory[Double, Size] = Size
}