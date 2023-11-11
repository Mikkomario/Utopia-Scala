package utopia.paradigm.angular

import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.operator.Combinable.SelfCombinable
import utopia.flow.operator.EqualsExtensions._
import utopia.flow.operator.{ApproxEquals, CanBeAboutZero, LinearScalable, SelfComparable, SignOrZero, SignedOrZero}
import utopia.paradigm.enumeration.RotationDirection
import utopia.paradigm.enumeration.RotationDirection.{Clockwise, Counterclockwise}
import utopia.paradigm.generic.ParadigmDataType.RotationType

object Rotation extends RotationFactory[NondirectionalRotation]
{
	// ATTRIBUTES	-----------------------
	
    /**
     * A zero rotation
     */
    protected val _zero = new Rotation(NondirectionalRotation.zero, Clockwise)
	
	/**
	  * A factory used for constructing clockwise rotations
	  */
	val clockwise = apply(Clockwise)
	/**
	  * A factory used for constructing counter-clockwise rotations
	  */
	val counterclockwise = apply(Counterclockwise)
	
	
	// COMPUTED ---------------------------
	
	/**
	  * A full 360 degrees rotation clockwise
	  */
	@deprecated("Please use .clockwise.circle instead", "v1.5")
	def clockwiseCircle = ofCircles(1)
	/**
	  * A full 360 degrees rotation counter-clockwise
	  */
	@deprecated("Please use .counterclockwise.circle instead", "v1.5")
	def counterclockwiseCircle = ofCircles(1, Counterclockwise)
	/**
	  * A 90 degrees rotation clockwise
	  */
	@deprecated("Please use .clockwise.quarter instead", "v1.5")
	def quarterClockwise = ofCircles(0.25)
	/**
	  * A 90 degrees rotation counter-clockwise
	  */
	@deprecated("Please use .counterclockwise.quarter instead", "v1.5")
	def quarterCounterclockwise = ofCircles(0.25, Counterclockwise)
	
	
	// IMPLEMENTED  ----------------------
	
	override def radians(rads: Double) = NondirectionalRotation.radians(rads)
    
	
	// OTHER	--------------------------
	
	/**
	  * @param amount Amount of rotation to apply towards the specified direction
	  * @param direction Targeted direction
	  * @return A new directional rotation instance
	  */
	def apply(amount: NondirectionalRotation, direction: RotationDirection) = {
		// Case: Amount would be negative => Converts it into a positive value (and reverses direction)
		if (amount.isNegative)
			new Rotation(-amount, direction.opposite)
		else
			new Rotation(amount, direction)
	}
	/**
	  * @param direction Targeted rotation direction
	  * @return A factory used for constructing rotations towards that direction
	  */
	def apply(direction: RotationDirection) = DirectionalRotationFactory(direction)
	
    /**
     * Converts a radian amount to a rotation
     */
    @deprecated("Please use .radians(Double) instead, possibly combining with .apply(RotationDirection)", "v1.5")
    def ofRadians(rads: Double, direction: RotationDirection = Clockwise) =
	    apply(direction).radians(rads)
    /**
     * Converts a degree amount to a rotation
     */
    @deprecated("Please use .degrees(Double) instead, possibly combining with .apply(RotationDirection)", "v1.5")
    def ofDegrees(degrees: Double, direction: RotationDirection = Clockwise) =
		ofRadians(degrees.toRadians, direction)
	/**
	  * @param circles The number of full circles (360 degrees or 2Pi radians) rotated
	  * @param direction Rotation direction (default = Clockwise)
	  * @return A new rotation
	  */
	@deprecated("Please use .circles(Double) instead, possibly combining with .apply(RotationDirection)", "v1.5")
	def ofCircles(circles: Double, direction: RotationDirection = Clockwise) =
		ofRadians(circles * 2 * math.Pi, direction)
	
	/**
	  * Calculates the rotation between two angles
	  * @param start The start angle
	  * @param end The end angle
	  * @param direction The rotation direction used
	  * @return A rotation from start to end with specified direction
	  */
	@deprecated("Please use .apply(Direction).between(Angle, Angle) instead", "v1.5")
	def between(start: Angle, end: Angle, direction: RotationDirection) =
		apply(direction).between(start, end)
	
	/**
	  * @param arcLength    Targeted arc length
	  * @param circleRadius Radius of the applicable circle
	  * @param direction    Direction of travel matching a positive arc length (default = Clockwise)
	  * @return Rotation that is required for producing the specified arc length over the specified travel radius
	  */
	@deprecated("Please use .apply(RotationDirection).forArcLength(Double, Double) instead", "v1.5")
	def forArcLength(arcLength: Double, circleRadius: Double,
	                 direction: RotationDirection) =
		apply(direction).forArcLength(arcLength, circleRadius)
	
	/**
	  * @param rotations A number of rotations
	  * @return An average between the rotations
	  */
	def average(rotations: Iterable[Rotation]): Rotation = {
		if (rotations.isEmpty)
			clockwise.zero
		else
			clockwise.radians(rotations.map { _.clockwise.radians }.sum / rotations.size)
	}
	
	
	// NESTED   ---------------------
	
	case class DirectionalRotationFactory(direction: RotationDirection) extends RotationFactory[Rotation]
	{
		// ATTRIBUTES   -------------
		
		override lazy val zero = super.zero
		
		
		// IMPLEMENTED  -------------
		
		override def radians(rads: Double): Rotation = Rotation(NondirectionalRotation(rads), direction)
		
		
		// OTHER    -----------------
		
		/**
		  * @param amount The amount of rotation to apply
		  * @return A rotation with the specified amount to the targeted direction
		  */
		def apply(amount: NondirectionalRotation) = Rotation(amount, direction)
		
		/**
		  * Calculates the rotation between two angles
		  * @param start     The start angle
		  * @param end       The end angle
		  * @return A rotation from start to end with specified direction
		  */
		def between(start: Angle, end: Angle) = {
			if (start == end)
				circle
			else {
				val rotationAmount = {
					if (direction == Clockwise) {
						if (end > start) end.radians - start.radians else end.radians + math.Pi * 2 - start.radians
					}
					else if (start > end) start.radians - end.radians else start.radians + math.Pi * 2 - end.radians
				}
				radians(rotationAmount)
			}
		}
	}
}

/**
* This class represents a rotation around a certain axis
* @author Mikko Hilpinen
* @since Genesis 21.11.2018
  * @param absolute The absolute rotation amount (i.e. rotation without direction)
  * @param direction The rotation direction (default = clockwise)
**/
case class Rotation private(absolute: NondirectionalRotation, direction: RotationDirection = Clockwise)
	extends LinearScalable[Rotation] with SelfCombinable[Rotation] with CanBeAboutZero[Rotation, Rotation]
		with SignedOrZero[Rotation] with SelfComparable[Rotation] with ValueConvertible
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * @return Clockwise amount of this rotation
	  */
	lazy val clockwise = absolute * direction.sign
	/**
	  * @return Counter-clockwise amount of this rotation
	  */
	lazy val counterClockwise = absolute * direction.sign.opposite
	
	
	// PROPS    --------------------------
	
	@deprecated("Please use .absolute.radians instead", "v1.5")
	def absoluteRadians = absolute.radians
	@deprecated("Please use .absolute.radians or .clockwise.radians instead", "v1.4.1")
	def radians = absoluteRadians
	
	/**
	  * This rotation in degrees (>= 0)
	  */
	@deprecated("Please use .absolute.degrees instead", "v1.5")
	def absoluteDegrees = absoluteRadians.toDegrees
	@deprecated("Please use .absolute.degrees or .clockwise.degrees instead", "v1.4.1")
	def degrees = absoluteDegrees
	
	/**
	 * A radian double value of this rotation
	 */
	@deprecated("Please use clockwiseRadians instead", "v2.3")
	def toDouble = clockwiseRadians
	
	/**
	  * @return A radian double value of this rotation to clockwise direction. Negative if this rotation is counter
	  *         clockwise
	  */
	@deprecated("Please use .clockwise.radians instead", "v1.5")
	def clockwiseRadians = absolute.radians * direction.modifier
	/**
	  * @return A degree double value of this rotation to clockwise direction. Negative if this rotation is counter
	  *         clockwise
	  */
	@deprecated("Please use .clockwise.degrees instead", "v1.5")
	def clockwiseDegrees = absolute.degrees * direction.modifier
	/**
	  * @return A radian double value of this rotation to counter clockwise direction. Negative if this direction is
	  *         clockwise.
	  */
	@deprecated("Please use .counterClockwise.radians instead", "v1.5")
	def counterClockwiseRadians = -clockwiseRadians
	/**
	  * @return A degree double value of this rotation to counter clockwise direction. Negative if this direction is
	  *         counter clockwise.
	  */
	@deprecated("Please use .counterClockwise.degrees instead", "v1.5")
	def counterClockwiseDegrees = -clockwiseDegrees
	/**
	  * @return Size of this rotation in complete clockwise circles
	  */
	@deprecated("Please use .clockwise.circles instead", "v1.5")
	def clockwiseCircles = (absoluteRadians / (2 * math.Pi)) * direction.modifier
	/**
	  * @return Size of this rotation in complete counter-clockwise circles
	  */
	@deprecated("Please use .counterClockwise.radians instead", "v1.5")
	def counterClockwiseCircles = -clockwiseCircles
	
	/**
	 * This rotation as an angle from origin (right)
	 */
	def toAngle = Angle.radians(clockwise.radians)
	
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
	def sine = math.sin(clockwise.radians)
	/**
	  * @return Arc sine of this rotation (clockwise) angle
	  */
	def arcSine = math.asin(clockwise.radians)
	/**
	  * @return Cosine of this rotation (clockwise) angle
	  */
	def cosine = math.cos(clockwise.radians)
	/**
	  * @return Arc cosine of this rotation (clockwise) angle
	  */
	def arcCosine = math.acos(clockwise.radians)
	/**
	  * @return Tangent (tan) of this rotation (clockwise) angle
	  */
	def tangent = math.tan(clockwise.radians)
	/**
	  * @return Arc tangent (atan) of this rotation (clockwise) angle
	  */
	def arcTangent = math.atan(clockwise.radians)
	
	
	// IMPLEMENTED    --------------------
	
	override def self = this
	
	override def sign: SignOrZero = direction.sign
	
	override def zero = Rotation._zero
	override def isZero = absolute.isZero
	override def isAboutZero: Boolean = absolute.isAboutZero
	
	override def toString = s"$absolute $direction"
	override implicit def toValue: Value = new Value(Some(this), RotationType)
	
	override def compareTo(o: Rotation) = clockwise.compareTo(o.clockwise)
	
	def ~==(other: Rotation) = clockwise.radians ~== other.clockwise.radians
	
	def *(modifier: Double) = {
		if (modifier >= 0)
			Rotation(absolute * modifier, direction)
		else
			Rotation(absolute * (-1 * modifier), direction.opposite)
	}
	def +(other: Rotation) = {
		if (direction == other.direction)
			copy(absolute + other.absolute, direction)
		else
			this - other.absolute
	}
	def -(other: Rotation) = this + (-other)
	
	
	// OTHER	--------------------------
	
	/**
	  * @param other A non-directional rotation.
	  *              Assumed to be towards this same direction (if positive)
	  * @return Copy of this rotation increased to its current direction by the specified amount.
	  */
	def +(other: NondirectionalRotation) = {
		if (other.isNegative && other.radians.abs > absolute.radians)
			Rotation(other.abs - absolute, direction.opposite)
		else
			copy(absolute + other)
	}
	/**
	  * @param other A non-directional rotation.
	  *              Assumed to be towards this same direction (if positive)
	  * @return Copy of this rotation decreased from its current direction by the specified amount.
	  */
	def -(other: NondirectionalRotation) = this + (-other)
	
	/**
	  * @param direction Targeted rotation direction
	  * @return This rotation as radians towards that direction.
	  *         Negative if this rotation is towards the opposite direction.
	  */
	@deprecated("Please use .towards(RotationDirection).radians instead", "v1.5")
	def radiansTowards(direction: RotationDirection) = direction match {
		case Clockwise => clockwiseRadians
		case Counterclockwise => counterClockwiseRadians
	}
	/**
	  * @param direction Targeted rotation direction
	  * @return This rotation as degrees towards that direction.
	  *         Negative if this rotation is towards the opposite direction.
	  */
	@deprecated("Please use .towards(RotationDirection).degrees instead", "v1.5")
	def degreesTowards(direction: RotationDirection) = direction match {
		case Clockwise => clockwiseDegrees
		case Counterclockwise => counterClockwiseDegrees
	}
	
	/**
	  * @param direction New rotation direction
	  * @return A copy of this rotation with the specified direction
	  */
	def towards(direction: RotationDirection) =
		if (this.direction == direction) absolute else -absolute
	
	/**
	  * Calculates the length of an arc produced by this rotation / angle over a specific circle
	  * @param circleRadius Radius of the circle
	  * @param positiveDirection Rotation direction that produces a positive arc length.
	  *                          Default = positive rotation direction (i.e. Clockwise),
	  *                          which means that by default every result is positive.
	  * @return Length of the arc produced by this rotation.
	  *         NB: May be negative.
	  */
	// Arc length = L * a/2*Pi, where L is the total circle radius 2*Pi*r and a is the angle produced by this rotation
	// Therefore arc length = r * a
	def arcLengthOver(circleRadius: Double, positiveDirection: RotationDirection = RotationDirection.positive) = {
		val length = absolute.arcLengthOver(circleRadius)
		if (positiveDirection == direction)
			length
		else
			-length
	}
	/**
	  * Calculates the length of an arc produced by this rotation / angle over a specific circle.
	  * NB: The returned length is always positive.
	  * @param circleRadius      Radius of the circle
	  * @return Length of the arc produced by this rotation.
	  */
	@deprecated("Please use .absolute.arcLengthOver(Double) instead", "v1.5")
	def absoluteArcLengthOver(circleRadius: Double) = absolute.arcLengthOver(circleRadius)
}