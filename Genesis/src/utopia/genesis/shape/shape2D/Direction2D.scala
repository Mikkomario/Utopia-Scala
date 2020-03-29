package utopia.genesis.shape.shape2D

import utopia.genesis.shape.Axis._
import utopia.genesis.shape.Axis2D

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
	 * @return Whether this side resides at the positive (true) or the negative (false) side of the axis
	 */
	def isPositiveDirection: Boolean
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
}

object Direction2D
{
	/**
	 * Direction upwards (y-axis, negative direction)
	 */
	case object Up extends Direction2D
	{
		override def axis = Y
		
		override def isPositiveDirection = false
		
		override def opposite = Down
	}
	
	/**
	 * Direction downwards (y-axis, positive direction)
	 */
	case object Down extends Direction2D
	{
		override def axis = Y
		
		override def isPositiveDirection = true
		
		override def opposite = Up
	}
	
	/**
	 * Direction right (x-axis, positive direction)
	 */
	case object Right extends Direction2D
	{
		override def axis = X
		
		override def isPositiveDirection = true
		
		override def opposite = Left
	}
	
	/**
	 * Direction left (x-axis, negative direction)
	 */
	case object Left extends Direction2D
	{
		override def axis = X
		
		override def isPositiveDirection = false
		
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
	def apply(axis: Axis2D, isPositive: Boolean): Direction2D = axis match
	{
		case X => if (isPositive) Right else Left
		case Y => if (isPositive) Down else Up
	}
}