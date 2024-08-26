package utopia.paradigm.shape.shape2d.area.polygon

import utopia.flow.collection.immutable.{Pair, Single}
import utopia.flow.operator.combine.Combinable
import utopia.flow.util.Mutate
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape3d.Matrix3D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.transform.Transformable

object Triangle
{
	// OTHER    ------------------------------
	
	/**
	  * Creates a new triangle with three corners provided
	  * @param p1 The first corner
	  * @param p2 The second corner
	  * @param p3 The third corner
	  * @return A new triangle
	  */
	def withCorners(p1: Point, p2: Point, p3: Point): Triangle = TriangleWithCorners(Vector(p1, p2, p3))
	
	/**
	  * @param origin Primary corner of this triangle
	  * @param sides Two sides that leave from this corner, as vectors
	  * @return A new triangle
	  */
	def apply(origin: Point, sides: Pair[Vector2D]): Triangle = TriangleWithSides(origin, sides)
	/**
	  * @param origin Primary corner of this triangle
	  * @param side1 The first side that leaves from this corner, as a vector
	  * @param side2 The second side that leaves from this corner, as a vector
	  * @return A new triangle
	  */
	def apply(origin: Point, side1: Vector2D, side2: Vector2D): Triangle = apply(origin, Pair(side1, side2))
	
	
	// NESTED   -------------------------------
	
	private case class TriangleWithSides(origin: Point, primarySides: Pair[Vector2D]) extends Triangle
	{
		// ATTRIBUTES   ----------------
		
		lazy val corners = origin +: primarySides.map { origin + _ }
		
		
		// IMPLEMENTED  ----------------
		
		override def edges =
			Vector(primarySides.first, primarySides.reverseReduce { _ - _ }, primarySides.second)
		
		override def +(other: HasDoubleDimensions) = copy(origin = origin + other)
	}
	
	private case class TriangleWithCorners(corners: Seq[Point]) extends Triangle
}

/**
  * Common trait for polygons with 3 corners.
  * @author Mikko Hilpinen
  * @since ???, < v1.7
  */
trait Triangle extends Polygon with Transformable[Triangle] with Combinable[HasDoubleDimensions, Triangle]
{
	// IMPLEMENTED	----------------
	
	override def identity = this
	
	override def isConvex = true
	
	override def center = Point.average(corners)
	override def area = {
		// Calculates the area of a triangle by utilizing the Heron's formula:
		//      A = sqrt(s(s-a)(s-b)(s-c))
		//          Where s = (a + b + c) / 2
		//          and a, b and c are the lengths of the triangle's edges
		// For reference, see: https://byjus.com/maths/area-of-a-triangle/
		val sideLengths = edges.map { _.length }
		val s = sideLengths.sum / 2
		math.sqrt(s * sideLengths.view.map { s - _ }.product)
	}
	
	override def toTriangles = Single(this)
	override def convexParts = Single(this)
	override def collisionAxes = edges.map { _.normal2D }
	
	override def transformedWith(transformation: Matrix3D) = map { _ * transformation }
	override def transformedWith(transformation: Matrix2D) = map { _ * transformation }
	
	override def +(other: HasDoubleDimensions) = map { _ + other }
	
	override def map(f: Mutate[Point]) = {
		val mapped = corners.map(f)
		Triangle.withCorners(mapped.head, mapped(1), mapped(2))
	}
	
	
	// OTHER    -------------------
	
	/**
	  * @param f A mapping function
	  * @return A copy of this triangle with mapped corners
	  */
	@deprecated("Renamed to .map(...)", "v1.7")
	def mapCorners(f: Point => Point) = map(f)
}