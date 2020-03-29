package utopia.reflection.shape

import utopia.genesis.shape.Axis._
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape2D.Size

import scala.collection.immutable.HashMap

object StackLengthLimit
{
	/**
	  * A stack length limit that doesn't actually limit stack lengths
	  */
	val noLimit = StackLengthLimit()
	
	/**
	  * Creates a set of stack length limits from 2D sizes
	  * @param min Minimum size (default = zero size)
	  * @param minOptimal Minimum optimal size (default = None)
	  * @param maxOptimal Maximum optimal size (default = None)
	  * @param max Maximum size (default = None)
	  * @return A map that contains limits for both X and Y axes
	  */
	def sizeLimit(min: Size = Size.zero, minOptimal: Option[Size] = None, maxOptimal: Option[Size] = None,
				  max: Option[Size] = None) = HashMap[Axis2D, StackLengthLimit](
		X -> limitsFromSizes(X, min, minOptimal, maxOptimal, max),
		Y -> limitsFromSizes(Y, min, minOptimal, maxOptimal, max))
	
	private def limitsFromSizes(axis: Axis2D, min: Size, minOptimal: Option[Size], maxOptimal: Option[Size], max: Option[Size]) =
		StackLengthLimit(min.along(axis), minOptimal.map { _.along(axis) },
			maxOptimal.map { _.along(axis) }, max.map { _.along(axis) })
}

/**
  * These classes are used for limiting stack lengths to certain bounds
  * @author Mikko Hilpinen
  * @since 15.5.2019, v1+
  * @param min The absolute minimum length (default = 0)
  * @param minOptimal The minimum value allowed for optimal length. None if not limited. (default = None)
  * @param maxOptimal The maximum value allowed for optimal length. None if not limited. (default = None)
  * @param max The absolute maximum length. None if not limited. (default = None)
  */
case class StackLengthLimit(min: Double = 0, minOptimal: Option[Double] = None, maxOptimal: Option[Double] = None,
							max: Option[Double] = None)
