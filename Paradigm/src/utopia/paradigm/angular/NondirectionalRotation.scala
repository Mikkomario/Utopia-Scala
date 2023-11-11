package utopia.paradigm.angular

import utopia.flow.operator.Combinable.SelfCombinable
import utopia.flow.operator.EqualsExtensions._
import utopia.flow.operator.{CanBeAboutZero, LinearScalable, SelfComparable, Sign, SignOrZero, SignedOrZero}
import utopia.paradigm.enumeration.RotationDirection
import utopia.paradigm.enumeration.RotationDirection.{Clockwise, Counterclockwise}

object NondirectionalRotation extends RotationFactory[NondirectionalRotation]
{
	// ATTRIBUTES   -----------------------
	
	/**
	  * A zero rotation
	  */
	override val zero = apply(0)
	
	
	// IMPLEMENTED	--------------------------
	
	override def radians(rads: Double) = apply(rads)
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param rotations A number of rotations
	  * @return An average between the rotations
	  */
	def average(rotations: Iterable[NondirectionalRotation]) =
		if (rotations.isEmpty) zero else radians(rotations.map { _.radians }.sum / rotations.size)
}

/**
  * Represents rotation around some axis. Doesn't specify direction. May be negative.
  * @author Mikko Hilpinen
  * @since 10.11.2023, v1.5
  * @param radians The amount of rotation in radians
  */
//noinspection SpellCheckingInspection
case class NondirectionalRotation private(radians: Double)
	extends SelfCombinable[NondirectionalRotation] with LinearScalable[NondirectionalRotation]
		with CanBeAboutZero[NondirectionalRotation, NondirectionalRotation]
		with SelfComparable[NondirectionalRotation] with SignedOrZero[NondirectionalRotation]
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
	def circles = radians / (2 * math.Pi)
	
	/**
	  * @return Copy of this rotation that's directed to the clockwise direction
	  */
	def clockwise = towards(Clockwise)
	/**
	  * @return Copy of this rotation that's directed to the counter-clockwise direction
	  */
	def counterclockwise = towards(Counterclockwise)
	
	
	// IMPLEMENTED    --------------------
	
	override def self = this
	
	override def zero = NondirectionalRotation.zero
	override def isZero = radians == 0.0
	override def isAboutZero: Boolean = radians ~== 0.0
	
	override def toString = f"$degrees%1.2f degrees"
	
	override def compareTo(o: NondirectionalRotation) = radians.compareTo(o.radians)
	
	override def ~==(other: NondirectionalRotation) = radians ~== other.radians
	
	override def *(modifier: Double) = NondirectionalRotation(radians * modifier)
	override def +(other: NondirectionalRotation) = NondirectionalRotation(radians + other.radians)
	
	
	// OTHER	--------------------------
	
	/**
	  * Subtracts the other rotation from this rotation.
	  * @param other Another rotation
	  * @return This rotation subtracted by the other rotation.
	  */
	def -(other: NondirectionalRotation) = NondirectionalRotation(radians - other.radians)
	
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