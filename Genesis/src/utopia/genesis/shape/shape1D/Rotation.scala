package utopia.genesis.shape.shape1D

import utopia.flow.util.SelfComparable
import utopia.genesis.shape.shape1D.RotationDirection.{Clockwise, Counterclockwise}
import utopia.genesis.util.Extensions._
import utopia.genesis.util.{ApproximatelyEquatable, Arithmetic}

object Rotation
{
	// ATTRIBUTES	-----------------------
	
    /**
     * A zero rotation
     */
    val zero = Rotation(0)
	
	/**
	  * A full 360 degrees rotation clockwise
	  */
	val clockwiseCircle = ofCircles(1)
	/**
	  * A full 360 degrees rotation counter-clockwise
	  */
	val counterclockwiseCircle = ofCircles(1, Counterclockwise)
	/**
	  * A 90 degrees rotation clockwise
	  */
	val quarterClockwise = ofCircles(0.25)
	/**
	  * A 90 degrees rotation counter-clockwise
	  */
	val quarterCounterclockwise = ofCircles(0.25, Counterclockwise)
    
	
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
	
	/**
	  * @param rotations A number of rotations
	  * @return An average between the rotations
	  */
	def average(rotations: Iterable[Rotation]) =
	{
		if (rotations.isEmpty)
			zero
		else
			ofRadians(rotations.map { _.clockwiseRadians }.sum / rotations.size)
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
	extends ApproximatelyEquatable[Rotation] with Arithmetic[Rotation, Rotation] with SelfComparable[Rotation]
{
	// PROPS    --------------------------
	
	/**
	  * @return Whether this rotation is exactly zero
	  */
	def isZero = radians == 0
	
	/**
	  * @return Whether this rotation is not exactly zero
	  */
	def nonZero = !isZero
	
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
	
	/**
	  * @return Sine of this rotation (clockwise) angle
	  */
	def sine = math.sin(clockwiseRadians)
	
	/**
	  * @return Arc sine of this rotation (clockwise) angle
	  */
	def arcSine = math.asin(clockwiseRadians)
	
	/**
	  * @return Cosine of this rotation (clockwise) angle
	  */
	def cosine = math.cos(clockwiseRadians)
	
	/**
	  * @return Arc cosine of this rotation (clockwise) angle
	  */
	def arcCosine = math.acos(clockwiseRadians)
	
	/**
	  * @return Tangent (tan) of this rotation (clockwise) angle
	  */
	def tangent = math.tan(clockwiseRadians)
	
	/**
	  * @return Arc tangent (atan) of this rotation (clockwise) angle
	  */
	def arcTangent = math.atan(clockwiseRadians)
	
	
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