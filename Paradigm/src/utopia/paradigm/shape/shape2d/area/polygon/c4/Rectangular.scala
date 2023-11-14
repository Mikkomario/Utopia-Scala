package utopia.paradigm.shape.shape2d.area.polygon.c4

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
	
	
	// IMPLEMENTED	-------------
	
	override def rightEdge = topEdge.normal2D.withLength(rightEdgeLength)
	
	override def maxEdgeLength = rightEdgeLength max topEdge.length
	override def minEdgeLength = rightEdgeLength min topEdge.length
	
	override def collisionAxes = Vector(topEdge, rightEdge)
}
