package utopia.paradigm.shape.shape2d.area.polygon

import utopia.paradigm.enumeration.RotationDirection
import utopia.paradigm.shape.shape2d.vector.point.Point

object Polygon
{
	/**
	  * @param p1 Point 1
	  * @param p2 Point 2
	  * @param p3 Point 3
	  * @param more Additional points
	  * @return A polygon with specified corners
	  */
	def apply(p1: Point, p2: Point, p3: Point, more: Point*) = new Polygon(Vector(p1, p2, p3) ++ more)
}

/**
  * Polygons are used for representing more complex 2D shapes
  * @author Mikko Hilpinen
  * @since Genesis 17.4.2019, v2+
  */
case class Polygon(corners: Seq[Point]) extends Polygonic
{
	// ATTRIBUTES	--------------------
	
	// Some more calculation extensive operations are cached in lazy variables
	
	override lazy val sides = super.sides
	
	override lazy val rotationDirection = super.rotationDirection
	
	override lazy val isConvex = super.isConvex
	
	override lazy val collisionAxes = super.collisionAxes
	
	override lazy val convexParts = super.convexParts
	
	
	// IMPLEMENTED  -------------------
	
	override def center = super[Polygonic].center
	
	
	// OTHER	------------------------
	
	/**
	  * Returns a copy of this polygon with the specified rotation direction
	  */
	def withRotationDirection(direction: RotationDirection) =
		if (rotationDirection == direction) this else Polygon(corners.reverse)
}
