package utopia.genesis.shape.shape2D

import utopia.genesis.shape.Axis._
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape1D.Direction1D
import utopia.genesis.shape.shape1D.Direction1D.{Negative, Positive}

/**
 * Represents a single side of a 2D rectangle, Eg. top
 * @author Mikko Hilpinen
 * @since 3.11.2019, v2.1+
 */
sealed trait Direction2D
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
	
	
	// COMPUTED	--------------------------------
	
	/**
	  * @return Whether this side resides at the positive (true) or the negative (false) side of the axis
	  */
	@deprecated("Please use sign instead", "v2.3")
	def isPositiveDirection = sign.isPositive
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
	}
	
	/**
	 * Direction downwards (y-axis, positive direction)
	 */
	case object Down extends Direction2D
	{
		override def axis = Y
		
		override def sign = Positive
		
		override def opposite = Up
	}
	
	/**
	 * Direction right (x-axis, positive direction)
	 */
	case object Right extends Direction2D
	{
		override def axis = X
		
		override def sign = Positive
		
		override def opposite = Left
	}
	
	/**
	 * Direction left (x-axis, negative direction)
	 */
	case object Left extends Direction2D
	{
		override def axis = X
		
		override def sign = Negative
		
		override def opposite = Right
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