package utopia.genesis.shape.shape2D

/**
  * Rectangular shapes have 4 90 degree corners
  */
trait Rectangular extends Parallelogramic
{
	// ABSTRACT	-----------------
	
	/**
	  * @return The lenght of the left / right (top to bottom) edge of this rectangular shape
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
	
	
	// IMPLEMENTED	-------------
	
	override def left = top.normal2D.withLength(leftLength)
}
