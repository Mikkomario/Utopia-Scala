package utopia.genesis.shape

import utopia.flow.util.RichComparable
import utopia.genesis.util.Extensions._
import utopia.genesis.util.{ApproximatelyEquatable, Arithmetic}
import utopia.genesis.shape.RotationDirection.{Clockwise, Counterclockwise}

object Rotation
{
	// ATTRIBUTES	-----------------------
	
    /**
     * A zero rotation
     */
    val zero = Rotation(0)
    
	
	// OTHER	--------------------------
	
    /**
     * Converts a radian amount to a rotation
     */
    def ofRadians(rads: Double, direction: RotationDirection = Clockwise) =
	{
		if (rads >= 0)
			Rotation(rads, direction)
		else
			Rotation(-rads, direction.opposite)
	}
    
    /**
     * Converts a degree amount to a rotation
     */
    def ofDegrees(degrees: Double, direction: RotationDirection = Clockwise) =
		ofRadians(degrees.toRadians, direction)
	
	/**
	  * @param circles The number of full circles (360 degrees or 2Pi radians) rotated
	  * @param direction Rotation direction (default = Clockwise)
	  * @return A new rotation
	  */
	def ofCircles(circles: Double, direction: RotationDirection = Clockwise) = ofRadians(circles * 2 * math.Pi)
	
	/**
	  * Calculates the rotation between two angles
	  * @param start The start angle
	  * @param end The end angle
	  * @param direction The rotation direction used
	  * @return A rotation from start to end with specified direction
	  */
	def between(start: Angle, end: Angle, direction: RotationDirection) =
	{
		if (start ~== end)
			ofRadians(2 * math.Pi, direction)
		else
		{
			val rotationAmount =
			{
				if (direction == Clockwise)
				{
					if (end > start) end.radians - start.radians else end.radians + math.Pi * 2 - start.radians
				}
				else
					if (start > end) start.radians - end.radians else start.radians + math.Pi * 2 - end.radians
			}
			ofRadians(rotationAmount, direction)
		}
	}
}

/**
* This class represents a rotation around a certain axis
* @author Mikko Hilpinen
* @since 21.11.2018
  * @param radians The amount of radians to rotate (always positive or zero)
  * @param direction The rotation direction (default = clockwise)
**/
case class Rotation private(radians: Double, direction: RotationDirection = Clockwise)
	extends ApproximatelyEquatable[Rotation] with Arithmetic[Rotation, Rotation] with RichComparable[Rotation]
{
	// PROPS    --------------------------
	
	/**
	  * @return Whether this rotation is exactly zero
	  */
	def isZero = radians == 0
	
	/**
	  * This rotation in degrees (>= 0)
	  */
	def degrees = radians.toDegrees
	
	/**
	 * A radian double value of this rotation
	 */
	@deprecated("Please use clockwiseRadians instead", "v2.3")
	def toDouble = clockwiseRadians
	
	/**
	  * @return A radian double value of this rotation to clockwise direction. Negative if this rotation is counter
	  *         clockwise
	  */
	def clockwiseRadians = radians * direction.modifier
	
	/**
	  * @return A degree double value of this rotation to clockwise direction. Negative if this rotation is counter
	  *         clockwise
	  */
	def clockwiseDegrees = degrees * direction.modifier
	
	/**
	  * @return A radian double value of this rotation to counter clockwise direction. Negative if this direction is
	  *         clockwise.
	  */
	def counterClockwiseRadians = -clockwiseRadians
	
	/**
	  * @return A degree double value of this rotation to counter clockwise direction. Negative if this direction is
	  *         counter clockwise.
	  */
	def counterClockwiseDegrees = -clockwiseDegrees
	
	/**
	 * This rotation as an angle from origin (right)
	 */
	def toAngle = Angle.ofRadians(clockwiseRadians)
	
	/**
	  * @return Whether this rotation is towards the clockwise direction
	  */
	def isClockwise = direction == Clockwise
	
	/**
	  * @return Whether this rotation is towards the counter clockwise direction
	  */
	def isCounterClockwise = direction == Counterclockwise
	
	
	// IMPLEMENTED    --------------------
	
	override def compareTo(o: Rotation) = clockwiseRadians.compareTo(o.clockwiseRadians)
	
	override def toString = f"$degrees%1.2f degrees $direction"
	
	def ~==(other: Rotation) = radians * direction.modifier ~== other.radians * direction.modifier
	
	override def repr = this
	
	/**
	  * Returns a multiplied version of this rotation
	  */
	def *(modifier: Double) =
	{
		if (modifier >= 0)
			Rotation(radians * modifier, direction)
		else
			Rotation(radians * -1 * modifier, direction.opposite)
	}
	
	/**
	  * Combines two rotations
	  */
	def +(other: Rotation) =
	{
		if (direction == other.direction)
			Rotation(radians + other.radians, direction)
		else if (radians >= other.radians)
			Rotation(radians - other.radians, direction)
		else
			Rotation(other.radians - radians, other.direction)
	}
	
	/**
	  * Subtracts a rotation from this one
	  */
	def -(other: Rotation) = this + (-other)
	
	
	// OTHER	--------------------------
	
	/**
	  * @param direction New rotation direction
	  * @return A copy of this rotation with the specified direction
	  */
	def towards(direction: RotationDirection) = copy(direction = direction)
}