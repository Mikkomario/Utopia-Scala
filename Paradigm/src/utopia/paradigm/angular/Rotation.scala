package utopia.paradigm.angular

import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.operator.Combinable.SelfCombinable
import utopia.flow.operator.EqualsExtensions._
import utopia.flow.operator.{ApproxEquals, LinearScalable, SelfComparable, SignOrZero, SignedOrZero}
import utopia.paradigm.enumeration.RotationDirection
import utopia.paradigm.enumeration.RotationDirection.{Clockwise, Counterclockwise}
import utopia.paradigm.generic.ParadigmDataType.RotationType

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
    def ofRadians(rads: Double, direction: RotationDirection = Clockwise) = {
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
	def between(start: Angle, end: Angle, direction: RotationDirection) = {
		if (start ~== end)
			ofRadians(2 * math.Pi, direction)
		else {
			val rotationAmount = {
				if (direction == Clockwise) {
					if (end > start) end.radians - start.radians else end.radians + math.Pi * 2 - start.radians
				}
				else
					if (start > end) start.radians - end.radians else start.radians + math.Pi * 2 - end.radians
			}
			ofRadians(rotationAmount, direction)
		}
	}
	
	/**
	  * @param arcLength    Targeted arc length
	  * @param circleRadius Radius of the applicable circle
	  * @param direction    Direction of travel matching a positive arc length
	  * @return Rotation that is required for producing the specified arc length over the specified travel radius
	  */
	def forArcLength(arcLength: Double, circleRadius: Double, direction: RotationDirection = Clockwise) = {
		// Whole circle diameter is 2*Pi*r. Length of the arc is (a / 2*Pi) * r, where a is the targeted angle
		// Therefore a = l/r, where l is the desired arc length
		ofRadians(arcLength / circleRadius, direction)
	}
	
	/**
	  * @param rotations A number of rotations
	  * @return An average between the rotations
	  */
	def average(rotations: Iterable[Rotation]) = {
		if (rotations.isEmpty)
			zero
		else
			ofRadians(rotations.map { _.clockwiseRadians }.sum / rotations.size)
	}
}

/**
* This class represents a rotation around a certain axis
* @author Mikko Hilpinen
* @since Genesis 21.11.2018
  * @param radians The amount of radians to rotate (always positive or zero)
  * @param direction The rotation direction (default = clockwise)
**/
case class Rotation private(radians: Double, direction: RotationDirection = Clockwise)
	extends LinearScalable[Rotation] with SelfCombinable[Rotation]
		with SignedOrZero[Rotation] with SelfComparable[Rotation] with ApproxEquals[Rotation]
		with ValueConvertible
{
	// PROPS    --------------------------
	
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
	  * @return Size of this rotation in complete clockwise circles
	  */
	def clockwiseCircles = (radians / (2 * math.Pi)) * direction.modifier
	/**
	  * @return Size of this rotation in complete counter-clockwise circles
	  */
	def counterClockwiseCircles = -clockwiseCircles
	
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
	
	override def self = this
	
	override def sign: SignOrZero = direction.sign
	
	override def zero = Rotation.zero
	override def isZero = radians == 0
	
	override def toString = f"$degrees%1.2f degrees $direction"
	override implicit def toValue: Value = new Value(Some(this), RotationType)
	
	override def compareTo(o: Rotation) = clockwiseRadians.compareTo(o.clockwiseRadians)
	
	def ~==(other: Rotation) = radians * direction.modifier ~== other.radians * direction.modifier
	
	def *(modifier: Double) = {
		if (modifier >= 0)
			Rotation(radians * modifier, direction)
		else
			Rotation(radians * -1 * modifier, direction.opposite)
	}
	def +(other: Rotation) = {
		if (direction == other.direction)
			Rotation(radians + other.radians, direction)
		else if (radians >= other.radians)
			Rotation(radians - other.radians, direction)
		else
			Rotation(other.radians - radians, other.direction)
	}
	def -(other: Rotation) = this + (-other)
	
	
	// OTHER	--------------------------
	
	/**
	  * @param direction Targeted rotation direction
	  * @return This rotation as radians towards that direction.
	  *         Negative if this rotation is towards the opposite direction.
	  */
	def radiansTowards(direction: RotationDirection) = direction match {
		case Clockwise => clockwiseRadians
		case Counterclockwise => counterClockwiseRadians
	}
	/**
	  * @param direction Targeted rotation direction
	  * @return This rotation as degrees towards that direction.
	  *         Negative if this rotation is towards the opposite direction.
	  */
	def degreesTowards(direction: RotationDirection) = direction match {
		case Clockwise => clockwiseDegrees
		case Counterclockwise => counterClockwiseDegrees
	}
	
	/**
	  * @param direction New rotation direction
	  * @return A copy of this rotation with the specified direction
	  */
	def towards(direction: RotationDirection) = copy(direction = direction)
	
	/**
	  * Calculates the length of an arc produced by this rotation / angle over a specific circle
	  * @param circleRadius Radius of the circle
	  * @param positiveDirection Rotation direction that produces a positive arc length.
	  *                          Default = direction of this rotation,
	  *                          which means that by default every result is positive.
	  * @return Length of the arc produced by this rotation.
	  *         Negative if 'positiveDirection' was set to that opposite to this rotation's direction.
	  */
	// Arc length = L * a/2*Pi, where L is the total circle radius 2*Pi*r and a is the angle produced by this rotation
	// Therefore arc length = r * a
	def arcLengthOver(circleRadius: Double, positiveDirection: RotationDirection = direction) =
		circleRadius * radiansTowards(positiveDirection)
}