package utopia.paradigm.shape.shape2d

/**
  * Rectangular shapes have 4 90 degree corners
  */
trait Rectangular extends Parallelogramic
{
	// ABSTRACT	-----------------
	
	/**
	  * @return The length of the left / right (top to bottom) edge of this rectangular shape
	  */
	def leftLength: Double
	
	
	// COMPUTED	-----------------
	
	/**
	  * @return The width of this rectangle (not necessarily X-wise)
	  */
	def width = top.length
	/**
	  * @return The height of this rectangle (not necessarily Y-wise)
	  */
	def height = left.length
	
	/**
	  * @return The width or the height of this rectangle, whichever is larger
	  */
	def maxDimension = width max height
	/**
	  * @return The width or the height of this rectangle, whichever is smaller
	  */
	def minDimension = width min height
	
	
	// IMPLEMENTED	-------------
	
	override def left = top.normal2D.withLength(leftLength)
}
