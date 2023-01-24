package utopia.paradigm.shape.shape2d

import utopia.flow.collection.immutable.caching.iterable.LazyVector
import utopia.paradigm.shape.shape3d.Matrix3D
import utopia.paradigm.transform.Transformable

/**
  * This trait is extended by 2D shapes that have 4 corners and where the opposite sides (left + right, top + bottom)
  * are always identical
  * @author Mikko Hilpinen
  * @since Genesis 14.4.2019
  */
trait Parallelogramic extends Polygonic with Transformable[Parallelogramic]
{
	// ABSTRACT	------------------
	
	/**
	  * @return The top-left corner of this shape
	  */
	def topLeftCorner: Point
	
	/**
	  * @return The edge from the top-left corner of this shape to the top-right corner of this shape
	  */
	def topEdge: Vector2D
	/**
	  * @return The edge from the top-right corner of this shape to the bottom-right corner of this shape
	  */
	def rightEdge: Vector2D
	
	
	// COMPUTED	------------------
	
	/**
	  * @return The edge from bottom-right corner to the bottom-left corner
	  */
	def bottomEdge = -topEdge
	/**
	  * @return The edge from the bottom-left corner of this shape to the top-left corner of this shape
	  */
	def leftEdge = -rightEdge
	
	/**
	  * @return The top-right corner of this shape
	  */
	def topRightCorner = topLeftCorner + topEdge
	/**
	  * @return The bottom-right corner of this shape
	  */
	def bottomRightCorner = topRightCorner + rightEdge
	/**
	  * @return The bottom-left corner of this shape
	  */
	def bottomLeftCorner = topLeftCorner - leftEdge
	
	/**
	  * @return The top side of this shape (left to right)
	  */
	def topSide = Line(topLeftCorner, topRightCorner)
	/**
	  * @return The right side of this shape (top to bottom)
	  */
	def rightSide = Line(topRightCorner, bottomRightCorner)
	/**
	  * @return The bottom side of this shape (right to left)
	  */
	def bottomSide = Line(bottomRightCorner, bottomLeftCorner)
	/**
	  * @return The left side of this shape (bottom to top)
	  */
	def leftSide = Line(bottomLeftCorner, topLeftCorner)
	
	/**
	  * @return The area of this shape
	  */
	def area = topEdge.length * rightEdge.length
	
	
	// IMPLEMENTED	--------------
	
	override def corners = LazyVector.fromFunctions(
		() => topLeftCorner,
		() => topRightCorner,
		() => bottomRightCorner,
		() => bottomLeftCorner
	)
	override def edges = Vector(topEdge, rightEdge, bottomEdge, leftEdge)
	
	override def collisionAxes = Vector(topEdge, rightEdge).map { _.normal2D }
	
	override def bounds = {
		val p1 = topLeftCorner
		val p2 = p1 + topEdge + rightEdge
		Bounds.between(p1, p2)
	}
	
	override def center = topLeftCorner + (topEdge / 2) + (rightEdge / 2)
	override def maxEdgeLength = topEdge.length max rightEdge.length
	override def minEdgeLength = topEdge.length min rightEdge.length
	
	override def transformedWith(transformation: Matrix3D) = map { _ * transformation }
	override def transformedWith(transformation: Matrix2D) = map { _ * transformation }
	
	
	// OTHER	----------------
	
	private def map(f: Point => Point) = {
		val topLeft2 = f(topLeftCorner)
		val topRight2 = f(topRightCorner).toVector
		val bottomRight2 = f(bottomRightCorner).toVector
		
		Parallelogram(topLeft2, topRight2 - topLeft2, bottomRight2 - topRight2)
	}
}
