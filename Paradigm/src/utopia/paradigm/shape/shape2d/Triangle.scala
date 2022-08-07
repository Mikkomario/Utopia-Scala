package utopia.paradigm.shape.shape2d

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
case class Triangle(origin: Point, side1: Vector2D, side2: Vector2D) extends Polygonic with Transformable[Triangle]
{
	// IMPLEMENTED	----------------
	
	override def transformedWith(transformation: Matrix3D) = mapCorners { _ * transformation }
	
	override def transformedWith(transformation: Matrix2D) = mapCorners { _ * transformation }
	
	lazy val corners = Vector(origin, origin + side1, origin + side2)
	
	
	// OTHER    -------------------
	
	/**
	 * @param f A mapping function
	 * @return A copy of this triangle with mapped corners
	 */
	def mapCorners(f: Point => Point) =
	{
		val mapped = corners.map(f)
		Triangle.withCorners(mapped.head, mapped(1), mapped(2))
	}
}