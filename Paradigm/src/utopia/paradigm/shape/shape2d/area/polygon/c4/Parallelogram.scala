package utopia.paradigm.shape.shape2d.area.polygon.c4

import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point

/**
  * A 2D shape that consists of 4 points
  * @param topLeftCorner The top left point
  * @param topEdge Vector from the top left point to the top right point
  * @param rightEdge Vector from the top-right point to tbe bottom-right point
  */
case class Parallelogram(topLeftCorner: Point, topEdge: Vector2D, rightEdge: Vector2D) extends Parallelogramic
{
	override lazy val corners = super.corners
	override lazy val bounds = super.bounds
	
	override def toString = s"Origin: $topLeft, Top Edge: $topEdge, Right Edge: $rightEdge"
}