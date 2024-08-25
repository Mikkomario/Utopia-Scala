package utopia.paradigm.shape.shape2d.area.polygon.c4

import utopia.flow.collection.immutable.caching.iterable.LazyVector
import utopia.flow.collection.immutable.{Pair, Single}
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.area.polygon.{Polygonic, Triangle}
import utopia.paradigm.shape.shape2d.line.Line
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
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
	
	
	// IMPLEMENTED	--------------
	
	override def identity = this
	
	override def area = {
		// A = ab*sin(o), where a and b are side lengths and o is the angle between two sides
		// Fore reference, see: // https://www.math.net/area-of-a-parallelogram
		topEdge.length * rightEdge.length * (topEdge.direction - rightEdge.direction).sine.abs
	}
	
	override def corners = LazyVector.fromFunctions(
		() => topLeftCorner,
		() => topRightCorner,
		() => bottomRightCorner,
		() => bottomLeftCorner
	)
	override def edges = Vector(topEdge, rightEdge, bottomEdge, leftEdge)
	
	override def collisionAxes: Seq[Vector2D] = Pair(topEdge, rightEdge).map { _.normal2D }
	
	override def center = topLeftCorner + (topEdge / 2) + (rightEdge / 2)
	override def maxEdgeLength = topEdge.length max rightEdge.length
	override def minEdgeLength = topEdge.length min rightEdge.length
	
	override def isConvex = true
	override def convexParts = Single(this)
	
	override def toTriangles =
		Pair(Triangle(topLeftCorner, topEdge, leftEdge), Triangle(bottomRightCorner, rightEdge, bottomEdge))
	
	override def transformedWith(transformation: Matrix3D) = _map { _ * transformation }
	override def transformedWith(transformation: Matrix2D) = _map { _ * transformation }
	
	
	// OTHER    -----------------------
	
	// NB: Assumes that the transformation would not change this to a non-parallelogram
	private def _map(f: Point => Point) = {
		val topLeft2 = f(topLeftCorner)
		val topRight2 = f(topRightCorner).toVector
		val bottomRight2 = f(bottomRightCorner).toVector
		
		Parallelogram(topLeft2, topRight2 - topLeft2, bottomRight2 - topRight2)
	}
}
