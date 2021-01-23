package utopia.genesis.shape.shape2D

import utopia.genesis.shape.Axis._
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape1D.{Angle, Direction1D, RotationDirection}
import utopia.genesis.shape.shape1D.Direction1D.{Negative, Positive}
import utopia.genesis.shape.shape1D.RotationDirection.{Clockwise, Counterclockwise}
import utopia.genesis.util.Scalable

/**
 * Represents a single side of a 2D rectangle, Eg. top
 * @author Mikko Hilpinen
 * @since 3.11.2019, v2.1+
 */
sealed trait Direction2D extends Scalable[Vector2D]
{
	// ABSTRACT	-------------------------------
	
	/**
	 * @return Axis used for determining this side
	 */
	def axis: Axis2D
	/**
	  * @return 1D sign of this direction (positive or negative)
	  */
	def sign: Direction1D
	/**
	 * @return Direction opposite to this one
	 */
	def opposite: Direction2D
	
	/**
	 * @return Whether this direction is horizontal
	 */
	def isHorizontal = axis == X
	
	/**
	 * @return Whether this direction is vertical
	 */
	def isVertical = axis == Y
	
	/**
	 * @return An angle based on this direction
	 */
	def toAngle: Angle
	
	
	// COMPUTED	--------------------------------
	
	/**
	  * @return This direction if it is horizontal. None otherwise.
	  */
	def horizontal = along(X)
	
	/**
	  * @return This direction if it is vertical. None otherwise.
	  */
	def vertical = along(Y)
	
	/**
	 * @return A direction that is 90 degree clockwise of this direction
	 */
	def rotatedQuarterClockwise = rotatedQuarterTowards(Clockwise)
	
	/**
	 * @return A direction that is 90 degree counter-clockwise of this direction
	 */
	def rotatedQuarterCounterClockwise = rotatedQuarterTowards(Counterclockwise)
	
	/**
	  * @return Whether this side resides at the positive (true) or the negative (false) side of the axis
	  */
	@deprecated("Please use sign instead", "v2.3")
	def isPositiveDirection = sign.isPositive
	
	
	// IMPLEMENTED  ----------------------------
	
	override def repr = axis(sign.modifier)
	
	override def *(mod: Double) = axis(mod * sign.modifier)
	
	
	// OTHER	--------------------------------
	
	/**
	 * @param length A vector length
	 * @return A vector with specified length and this direction
	 */
	def apply(length: Double) = this * length
	
	/**
	  * @param axis Target axis
	  * @return This direction, if it is parallel to the specified axis. None otherwise.
	  */
	def along(axis: Axis2D) = if (axis == this.axis) Some(this) else None
	
	/**
	 * @param direction Target direction
	 * @return Direction which is 90 degrees towards the specified rotation direction from this direction
	 */
	def rotatedQuarterTowards(direction: RotationDirection): Direction2D
}

object Direction2D
{
	/**
	 * Direction upwards (y-axis, negative direction)
	 */
	case object Up extends Direction2D
	{
		override def axis = Y
		
		override def sign = Negative
		
		override def opposite = Down
		
		override def toAngle = Angle.up
		
		override def rotatedQuarterTowards(direction: RotationDirection) = direction match
		{
			case Clockwise => Right
			case Counterclockwise => Left
		}
	}
	
	/**
	 * Direction downwards (y-axis, positive direction)
	 */
	case object Down extends Direction2D
	{
		override def axis = Y
		
		override def sign = Positive
		
		override def opposite = Up
		
		override def toAngle = Angle.down
		
		override def rotatedQuarterTowards(direction: RotationDirection) = direction match
		{
			case Clockwise => Left
			case Counterclockwise => Right
		}
	}
	
	/**
	 * Direction right (x-axis, positive direction)
	 */
	case object Right extends Direction2D
	{
		override def axis = X
		
		override def sign = Positive
		
		override def opposite = Left
		
		override def toAngle = Angle.right
		
		override def rotatedQuarterTowards(direction: RotationDirection) = direction match
		{
			case Clockwise => Down
			case Counterclockwise => Up
		}
	}
	
	/**
	 * Direction left (x-axis, negative direction)
	 */
	case object Left extends Direction2D
	{
		override def axis = X
		
		override def sign = Negative
		
		override def opposite = Right
		
		override def toAngle = Angle.left
		
		override def rotatedQuarterTowards(direction: RotationDirection) = direction match
		{
			case Clockwise => Up
			case Counterclockwise => Down
		}
	}
	
	/**
	 * All possible directions
	 */
	val values = Vector[Direction2D](Up, Down, Left, Right)
	
	/**
	 * @return All horizontal directions
	 */
	def horizontal = Vector[Direction2D](Left, Right)
	
	/**
	 * @return All vertical directions
	 */
	def vertical = Vector[Direction2D](Up, Down)
	
	/**
	  * @param axis Target axis
	  * @return Directions along that axis
	  */
	def along(axis: Axis2D) = axis match
	{
		case X => horizontal
		case Y => vertical
	}
	
	/**
	 * @param axis Target axis
	 * @param isPositive Whether direction should be positive (true) or negative (false)
	 * @return A direction
	 */
	@deprecated("Please use apply(Axis2D, Direction1D) instead", "v2.3")
	def apply(axis: Axis2D, isPositive: Boolean): Direction2D = axis match
	{
		case X => if (isPositive) Right else Left
		case Y => if (isPositive) Down else Up
	}
	
	/**
	  * @param axis Target axis
	  * @param sign Direction along that axis (positive or negative)
	  * @return A 2D direction
	  */
	def apply(axis: Axis2D, sign: Direction1D): Direction2D = axis match
	{
		case X => if (sign.isPositive) Right else Left
		case Y => if (sign.isPositive) Down else Up
	}
}