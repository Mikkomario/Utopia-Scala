package utopia.genesis.shape.shape2D

import utopia.genesis.shape.Vector3D

object Triangle
{
	/**
	  * Creates a new triangle with three corners provided
	  * @param p1 The first corner
	  * @param p2 The second corner
	  * @param p3 The third corner
	  * @return A new triangle
	  */
	def apply(p1: Point, p2: Point, p3: Point) = new Triangle(p1, (p2 - p1).toVector, (p3 - p1).toVector)
}

/**
  * This shape represents a triangle
  * @param origin The origin point of the triangle
  * @param side1 The first side of this triangle as a vector
  * @param side2 The second side of this triangle as a vector
  */
case class Triangle(origin: Point, side1: Vector3D, side2: Vector3D) extends Polygonic with TransformProjectable[Triangle]
{
	// IMPLEMENTED	----------------
	
	override def transformedWith(transformation: Transformation) =
	{
		val p1 = transformation(origin)
		val p2 = transformation(origin + side1)
		val p3 = transformation(origin + side2)
		
		Triangle(p1, p2, p3)
	}
	
	lazy val corners = Vector(origin, origin + side1, origin + side2)
}