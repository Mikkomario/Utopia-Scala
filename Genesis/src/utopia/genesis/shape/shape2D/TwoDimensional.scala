package utopia.genesis.shape.shape2D

import utopia.genesis.shape.Axis.{X, Y}
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.template.Dimensional

/**
  * Common trait for items that have two or more dimensional components
  * @author Mikko Hilpinen
  * @since 14.7.2020, v2.3
  */
trait TwoDimensional[+A] extends Dimensional[A]
{
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
	  * @return The first two dimensions of this instance
	  */
	def dimensions2D = dimensions.take(2)
	
	/**
	  * @return A map that contains this item's first two dimensions (if available), tied to X and Y axes.
	  */
	def toMap2D: Map[Axis2D with Product, A] =
	{
		val dims = dimensions2D
		if (dims.size >= 2)
			Map(X -> dims.head, Y -> dims(1))
		else if (dims.nonEmpty)
			Map(X -> dims.head)
		else
			Map()
	}
}
