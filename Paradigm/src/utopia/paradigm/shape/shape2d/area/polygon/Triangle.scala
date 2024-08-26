package utopia.paradigm.shape.shape2d.area.polygon

import utopia.flow.collection.immutable.Single
import utopia.flow.util.Mutate
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape3d.Matrix3D
import utopia.paradigm.transform.Transformable

object Triangle
{
	/**
	  * Creates a new triangle with three corners provided
	  * @param p1 The first corner
	  * @param p2 The second corner
	  * @param p3 The third corner
	  * @return A new triangle
	  */
	def withCorners(p1: Point, p2: Point, p3: Point) = new Triangle(p1, (p2 - p1).toVector, (p3 - p1).toVector)
}

/**
  * This shape represents a triangle
  * @param origin The origin point of the triangle
  * @param side1 The first side of this triangle as a vector
  * @param side2 The second side of this triangle as a vector
  */
// TODO: Change into a trait and allow 2 formats: sides or corners
case class Triangle(origin: Point, side1: Vector2D, side2: Vector2D)
	extends Polygonic with Transformable[Triangle]
{
	// ATTRIBUTES   ----------------
	
	lazy val corners = Vector(origin, origin + side1, origin + side2)
	
	
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
	
	override def edges = Vector(side1, side2 - side1, side2)
	
	override def toTriangles = Single(this)
	override def convexParts = Single(this)
	override def collisionAxes = edges.map { _.normal2D }
	
	override def transformedWith(transformation: Matrix3D) = map { _ * transformation }
	override def transformedWith(transformation: Matrix2D) = map { _ * transformation }
	
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