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
	def rightEdgeLength: Double
	
	
	// COMPUTED	-----------------
	
	/**
	  * @return The width or the height of this rectangle, whichever is larger
	  */
	@deprecated("Please use maxEdgeLength instead", "v1.1")
	def maxDimension = maxEdgeLength
	/**
	  * @return The width or the height of this rectangle, whichever is smaller
	  */
	@deprecated("Please use minEdgeLength instead", "v1.1")
	def minDimension = minEdgeLength
	
	
	// IMPLEMENTED	-------------
	
	override def rightEdge = topEdge.normal2D.withLength(rightEdgeLength)
	
	override def maxEdgeLength = rightEdgeLength max topEdge.length
	override def minEdgeLength = rightEdgeLength min topEdge.length
}
