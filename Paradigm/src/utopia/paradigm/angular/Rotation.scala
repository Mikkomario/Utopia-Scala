package utopia.paradigm.angular

import utopia.flow.operator.combine.Combinable.SelfCombinable
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.operator.sign.{Sign, SignOrZero, SignedOrZero}
import utopia.flow.operator.MayBeAboutZero
import utopia.flow.operator.combine.LinearScalable
import utopia.paradigm.enumeration.RotationDirection
import utopia.paradigm.enumeration.RotationDirection.{Clockwise, Counterclockwise}

object Rotation extends RotationFactory[Rotation]
{
	// ATTRIBUTES   -----------------------
	
	/**
	  * A zero rotation
	  */
	override val zero = apply(0)
	
	/**
	  * A factory used for constructing clockwise rotations
	  */
	val clockwise = apply(Clockwise)
	/**
	  * A factory used for constructing counter-clockwise rotations
	  */
	val counterclockwise = apply(Counterclockwise)
	
	override lazy val quarter = super.quarter
	override lazy val circle = super.circle
	override lazy val halfCircle = super.halfCircle
	
	
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
	
	
	// IMPLEMENTED	--------------------------
	
	override def radians(rads: Double) = apply(rads)
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param amount    Amount of rotation to apply towards the specified direction
	  * @param direction Targeted direction
	  * @return A new directional rotation instance
	  */
	def apply(amount: Rotation, direction: RotationDirection) =
		DirectionalRotation(amount, direction)
	/**
	  * @param direction Targeted rotation direction
	  * @return A factory used for constructing rotations towards that direction
	  */
	def apply(direction: RotationDirection) = DirectionalRotation(direction)
	
	/**
	  * @param rotations A number of rotations
	  * @return An average between the rotations
	  */
	def average(rotations: Iterable[Rotation]) =
		if (rotations.isEmpty) zero else radians(rotations.view.map { _.radians }.sum / rotations.size)
	
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
	  * @param circles   The number of full circles (360 degrees or 2Pi radians) rotated
	  * @param direction Rotation direction (default = Clockwise)
	  * @return A new rotation
	  */
	@deprecated("Please use .circles(Double) instead, possibly combining with .apply(RotationDirection)", "v1.5")
	def ofCircles(circles: Double, direction: RotationDirection = Clockwise) =
		ofRadians(circles * 2 * math.Pi, direction)
	/**
	  * Calculates the rotation between two angles
	  * @param start     The start angle
	  * @param end       The end angle
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
}

/**
  * Represents rotation around some axis. Doesn't specify direction. May be negative.
  * @author Mikko Hilpinen
  * @since 10.11.2023, v1.5
  * @param radians The amount of rotation in radians
  */
//noinspection SpellCheckingInspection
case class Rotation private(radians: Double)
	extends SelfCombinable[Rotation] with LinearScalable[Rotation]
		with MayBeAboutZero[Rotation, Rotation]
		with SelfComparable[Rotation] with SignedOrZero[Rotation]
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * This rotation in degrees
	  */
	lazy val degrees = radians.toDegrees
	
	override lazy val sign: SignOrZero = Sign.of(radians)
	
	
	// PROPS    --------------------------
	
	/**
	  * @return Size of this rotation in complete circles
	  */
	def circles = this / Rotation.circle
	/**
	  * @return The size of this rotation in 90 degree quarters / angles.
	  *         E.g. If this rotation was a full circle (360 degrees), this function would return 4.0.
	  */
	def quarters = this / Rotation.quarter
	
	/**
	  * @return Copy of this rotation that's directed to the clockwise direction
	  */
	def clockwise = towards(Clockwise)
	/**
	  * @return Copy of this rotation that's directed to the counter-clockwise direction
	  */
	def counterclockwise = towards(Counterclockwise)
	
	/**
	  * @return This rotation as an angle.
	  *         NB: Negative rotation creates angles equal to 360 degrees - this.
	  */
	def toAngle = Angle.radians(radians)
	
	
	// IMPLEMENTED    --------------------
	
	override def self = this
	
	override def zero = Rotation.zero
	override def isZero = radians == 0.0
	override def isAboutZero: Boolean = radians ~== 0.0
	
	override def toString = f"$degrees%1.2f degrees"
	
	override def compareTo(o: Rotation) = radians.compareTo(o.radians)
	
	override def ~==(other: Rotation) = radians ~== other.radians
	
	override def *(modifier: Double) = Rotation(radians * modifier)
	override def +(other: Rotation) = Rotation(radians + other.radians)
	
	
	// OTHER	--------------------------
	
	/**
	  * Subtracts the other rotation from this rotation.
	  * @param other Another rotation
	  * @return This rotation subtracted by the other rotation.
	  */
	def -(other: Rotation) = Rotation(radians - other.radians)
	/**
	  * @param other Another rotation
	  * @return The ratio of these rotations
	  */
	def /(other: Rotation) = radians / other.radians
	
	/**
	  * @param direction New rotation direction
	  * @return A copy of this rotation with the specified direction
	  */
	def towards(direction: RotationDirection) = Rotation(this, direction)
	
	/**
	  * Calculates the length of an arc produced by this rotation / angle over a specific circle
	  * @param circleRadius Radius of the circle
	  * @return Length of the arc produced by this rotation.
	  *         NB: Always positive
	  */
	// Arc length = L * a/2*Pi, where L is the total circle radius 2*Pi*r and a is the angle produced by this rotation
	// Therefore arc length = r * a
	def arcLengthOver(circleRadius: Double) = circleRadius * radians.abs
}