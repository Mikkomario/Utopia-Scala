package utopia.genesis.shape.shape2D

import utopia.genesis.shape.Axis.{X, Y}
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
}
