package utopia.paradigm.shape.shape3d

import utopia.flow.collection.immutable.Pair
import utopia.paradigm.enumeration.Axis.Z
import utopia.paradigm.shape.shape2d.MultiDimensional

/**
  * A common trait for items that contain exactly three dimensions
  * @author Mikko Hilpinen
  * @since Genesis 14.7.2020, v2.3
  */
trait ThreeDimensional[+A] extends MultiDimensional[A]
{
	// COMPUTED	------------------------
	
	/**
	  * @return This instance's z-component
	  */
	def z = along(Z)
	
	/**
	  * @return The first three dimensions of this dimensional instance
	  */
	@deprecated("This is exactly the same as dimensions (in this context)", "v2.6")
	def dimensions3D = dimensions.take(3)
	
	
	// IMPLEMENTED  ----------------------
	
	override def dimensions2D = Pair(x, y)
}
