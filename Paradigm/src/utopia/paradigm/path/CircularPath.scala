package utopia.paradigm.path

import utopia.flow.operator.LinearMeasurable
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.enumeration.RotationDirection
import utopia.paradigm.enumeration.RotationDirection.Clockwise
import utopia.paradigm.shape.shape2d.{Circle, Point}

/**
  * This path traverses along circle edge
  * @author Mikko Hilpinen
  * @since Genesis 21.6.2019, v2.1+
  * @param circle The circle that determines this path
  * @param startAngle The starting angle (default = 270 degrees = up)
  * @param endAngle The ending angle (default = 270 degrees = up). If same as starting angle,
  *                 will rotate 360 degress, not 0
  * @param direction The traversing direction along the circle
  */
case class CircularPath(circle: Circle, startAngle: Angle = Angle.up, endAngle: Angle = Angle.up,
                        direction: RotationDirection = Clockwise) extends Path[Point] with LinearMeasurable
{
	// ATTRIBUTES	--------------------
	
	val maxRotation = Rotation.between(startAngle, endAngle, direction)
	
	
	// IMPLEMENTED	--------------------
	
	override def start = circle(startAngle)
	
	override def end = circle(endAngle)
	
	// Arc length = 2Pi*r for whole circle
	override def length = 2 * Math.PI * circle.radius * maxRotation.radians / (2 * Math.PI)
	
	override def apply(t: Double) = circle(startAngle + maxRotation * t)
}
