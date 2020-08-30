package utopia.genesis.shape.shape3D

import utopia.genesis.shape.Axis.Z
import utopia.genesis.shape.shape2D.TwoDimensional

/**
  * A common trait for items that contain at least three separate dimensions
  * @author Mikko Hilpinen
  * @since 14.7.2020, v2.3
  */
trait ThreeDimensional[+A] extends TwoDimensional[A]
{
	// COMPUTED	------------------------
	
	/**
	  * @return This instance's z-component
	  */
	def z = along(Z)
	
	/**
	  * @return The first three dimensions of this dimensional instance
	  */
	def dimensions3D = dimensions.take(3)
}
