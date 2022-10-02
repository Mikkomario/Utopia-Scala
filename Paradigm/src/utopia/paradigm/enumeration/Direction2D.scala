package utopia.paradigm.enumeration

import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.flow.operator.{Reversible, Scalable, Sign}
import utopia.paradigm.angular.Angle
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.RotationDirection.{Clockwise, Counterclockwise}
import utopia.paradigm.shape.shape1d.Vector1D

sealed trait Direction2DLike[+Parallel, +Perpendicular] extends Reversible[Parallel] with Scalable[Double, Vector1D]
{
	// ABSTRACT	--------------------------
	
	/**
	  * @return Axis used for determining this side
	  */
	def axis: Axis2D
	/**
	  * @return 1D sign of this direction (positive or negative)
	  */
	def sign: Sign
	/**
	  * @return Direction opposite to this one
	  */
	def opposite: Parallel
	
	/**
	  * @param direction Target direction
	  * @return Direction which is 90 degrees towards the specified rotation direction from this direction
	  */
	def rotatedQuarterTowards(direction: RotationDirection): Perpendicular
	
	/**
	  * @return An angle based on this direction
	  */
	def toAngle: Angle
	
	/**
	  * @param axis Target axis
	  * @return This direction, if it is parallel to the specified axis. None otherwise.
	  */
	def along(axis: Axis2D): Option[Parallel]
	
	
	// COMPUTED	--------------------------
	
	/**
	  * @return Whether this direction is horizontal
	  */
	def isHorizontal = axis == X
	/**
	  * @return Whether this direction is vertical
	  */
	def isVertical = axis == Y
	
	/**
	  * @return A direction that is 90 degree clockwise of this direction
	  */
	def rotatedQuarterClockwise = rotatedQuarterTowards(Clockwise)
	/**
	  * @return A direction that is 90 degree counter-clockwise of this direction
	  */
	def rotatedQuarterCounterClockwise = rotatedQuarterTowards(Counterclockwise)
	
	/**
	  * @return Directions perpendicular to this direction. First the direction to counterclockwise direction,
	  *         then the direction to clockwise direction
	  */
	def perpendicular = Vector(rotatedQuarterCounterClockwise, rotatedQuarterClockwise)
	
	/**
	  * @return A unit vector pointing towards this direction
	  */
	def toUnitVector = axis(sign.modifier)
	
	
	// IMPLEMENTED  ----------------------------
	
	override def unary_- = opposite
	
	override def *(mod: Double) = axis(mod * sign.modifier)
	
	
	// OTHER	--------------------------------
	
	/**
	  * @param length A vector length
	  * @return A vector with specified length and this direction
	  */
	def apply(length: Double) = this * length
}

/**
 * Represents a single side of a 2D rectangle, Eg. top
 * @author Mikko Hilpinen
 * @since Genesis 3.11.2019, v2.1+
 */
sealed trait Direction2D extends Direction2DLike[Direction2D, Direction2D]
{
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
	  * @return Whether this side resides at the positive (true) or the negative (false) side of the axis
	  */
	@deprecated("Please use sign instead", "v2.3")
	def isPositiveDirection = sign.isPositive
}

/**
  * Common trait for horizontal 2D directions (Left & Right)
  */
sealed trait HorizontalDirection extends Direction2D with Direction2DLike[HorizontalDirection, VerticalDirection]
{
	override def repr = this
	
	override def axis = X
	
	override def along(axis: Axis2D) = if (axis == X) Some(this) else None
}

/**
  * Common trait for vertical 2D directions (Up & Down)
  */
sealed trait VerticalDirection extends Direction2D with Direction2DLike[VerticalDirection, HorizontalDirection]
{
	override def repr = this
	
	override def axis = Y
	
	override def along(axis: Axis2D) = if (axis == Y) Some(this) else None
}

object Direction2D
{
	// ATTRIBUTES	---------------------------
	
	/**
	  * All possible directions
	  */
	val values = Vector[Direction2D](Up, Down, Left, Right)
	
	
	// COMPUTED	-------------------------------
	
	/**
	  * @return All horizontal directions
	  */
	def horizontal = HorizontalDirection.values
	
	/**
	  * @return All vertical directions
	  */
	def vertical = VerticalDirection.values
	
	
	// OTHER	-------------------------------
	
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
	@deprecated("Please use apply(Axis2D, Sign) instead", "v2.3")
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
	def apply(axis: Axis2D, sign: Sign): Direction2D = axis match
	{
		case X => if (sign.isPositive) Right else Left
		case Y => if (sign.isPositive) Down else Up
	}
	
	
	// NESTED	-------------------------------
	
	/**
	 * Direction upwards (y-axis, negative direction)
	 */
	case object Up extends VerticalDirection
	{
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
	case object Down extends VerticalDirection
	{
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
	case object Right extends HorizontalDirection
	{
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
	case object Left extends HorizontalDirection
	{
		override def sign = Negative
		
		override def opposite = Right
		
		override def toAngle = Angle.left
		
		override def rotatedQuarterTowards(direction: RotationDirection) = direction match
		{
			case Clockwise => Up
			case Counterclockwise => Down
		}
	}
}

object HorizontalDirection
{
	// ATTRIBUTES	------------------------
	
	/**
	  * All horizontal direction options (left & right)
	  */
	val values = Vector[HorizontalDirection](Direction2D.Left, Direction2D.Right)
	
	
	// OTHER	----------------------------
	
	/**
	  * @param sign Direction sign
	  * @return Horizontal direction with that sign
	  */
	def apply(sign: Sign) = sign match
	{
		case Positive => Direction2D.Right
		case Negative => Direction2D.Left
	}
}

object VerticalDirection
{
	// ATTRIBUTES	------------------------
	
	/**
	  * All vertical direction options (up & down)
	  */
	val values = Vector[VerticalDirection](Direction2D.Up, Direction2D.Down)
	
	
	// OTHER	----------------------------
	
	/**
	  * @param sign Direction sign
	  * @return Vertical direction with that sign
	  */
	def apply(sign: Sign) = sign match
	{
		case Positive => Direction2D.Down
		case Negative => Direction2D.Up
	}
}