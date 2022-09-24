package utopia.paradigm.shape.shape2d

import utopia.flow.collection.immutable.Pair
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.template.Dimensional

/**
  * Common trait for items that have two or more dimensional components
  * @author Mikko Hilpinen
  * @since Genesis 14.7.2020, v2.3
  */
trait MultiDimensional[+A] extends Dimensional[A]
{
	// ABSTRACT -----------------------
	
	/**
	  * @return The two dimensions (x, then y) of this item
	  */
	def dimensions2D: Pair[A]
	
	
	// COMPUTED	-----------------------
	
	/**
	  * @return This instance's x-component
	  */
	def x = along(X)
	/**
	  * @return This instance's y-component
	  */
	def y = along(Y)
	
	/**
	  * @return A map that contains this item's first two dimensions, tied to X and Y axes.
	  */
	def toMap2D: Map[Axis2D, A] =
	{
		val (x, y) = dimensions2D.toTuple
		Map(X -> x, Y -> y)
	}
}
