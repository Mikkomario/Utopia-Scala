package utopia.paradigm.angular

import utopia.flow.operator._
import utopia.flow.operator.combine.{Combinable, LinearScalable}
import utopia.flow.operator.ordering.RichComparable
import utopia.flow.operator.sign.{SignOrZero, Signed, SignedOrZero}


/**
* Common trait for rotations around a some axis, which specify a direction from binary options
* @author Mikko Hilpinen
* @since 11.11.2023, v1.5
  * @tparam Direction Type of rotation direction used
  * @tparam C Highest comparable rotation class
  * @tparam Repr Implementing class
**/
trait DirectionalRotationLike[+Direction <: Signed[Direction], -C <: DirectionalRotationLike[_, _, _], +Repr]
	extends LinearScalable[Repr] with Combinable[C, Repr] with MayBeAboutZero[C, Repr]
		with SignedOrZero[Repr] with RichComparable[C]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return The size of this rotation (non-directional, always positive)
	  */
	def absolute: Rotation
	/**
	  * @return The effective direction of this rotation
	  */
	def direction: Direction
	
	/**
	  * Creates a new, adjusted copy of this rotation
	  * @param amount New rotation amount to assign (>= 0)
	  * @param reverseDirection Whether the direction of this rotation should be reversed (true)
	  *                          or preserved (false).
	  *                          Default = false.
	  * @return Copy of this rotation instance with the specified amount and direction
	  */
	protected def copy(amount: Rotation = absolute, reverseDirection: Boolean = false): Repr
	
	
	// COMPUTED    --------------------------
	
	/**
	  * @return Copy of this rotation without direction information.
	  *         The resulting rotation amount may be negative.
	  */
	def unidirectional = direction.sign * absolute
	
	/**
	 * This rotation as an angle from origin (right)
	 */
	def toAngle = unidirectional.toAngle
	
	/**
	  * @return Sine of this rotation (clockwise) angle
	  */
	def sine = math.sin(unidirectional.radians)
	/**
	  * @return Arc sine of this rotation (clockwise) angle
	  */
	def arcSine = math.asin(unidirectional.radians)
	/**
	  * @return Cosine of this rotation (clockwise) angle
	  */
	def cosine = math.cos(unidirectional.radians)
	/**
	  * @return Arc cosine of this rotation (clockwise) angle
	  */
	def arcCosine = math.acos(unidirectional.radians)
	/**
	  * @return Tangent (tan) of this rotation (clockwise) angle
	  */
	def tangent = math.tan(unidirectional.radians)
	/**
	  * @return Arc tangent (atan) of this rotation (clockwise) angle
	  */
	def arcTangent = math.atan(unidirectional.radians)
	
	/**
	  * @return A copy of this rotation that reaches the same end angle,
	  *         but does so by traversing to the opposite direction.
	  *
	  *         E.g. If this rotation is 60 degrees clockwise,
	  *         the complementary rotation is 300 degrees counter-clockwise.
	  *
	  *         If this rotation is larger than 360 degrees,
	  *         each full revolution is converted to a full revolution with a different direction.
	  *         E.g. If this rotation is 370 degrees counter-clockwise,
	  *         the complementary is 360 + 350 = 710 degrees clockwise.
	  */
	def complementary = {
		// Converts this rotation into circle units
		// and splits into full circles (i.e. 360 degree rotations) and partial circles (i.e. the remainder)
		val circles = absolute / Rotation.revolution
		val fullCircles = circles.toInt
		val incompleteCircles = circles - fullCircles
		
		// Full circles are reversed and the remainder is converted,
		// so that the same angle is reached but from the other direction
		val absoluteComplementaryRotation = {
			if (incompleteCircles > 0)
				Rotation.circles(circles + (1 - incompleteCircles))
			else
				Rotation.circles(circles)
		}
		copy(absoluteComplementaryRotation, reverseDirection = true)
	}
	
	
	// IMPLEMENTED    --------------------
	
	override def sign: SignOrZero = direction.sign
	
	override def isZero = absolute.isZero
	override def isAboutZero: Boolean = absolute.isAboutZero
	
	override def toString = s"$absolute $direction"
	
	override def compareTo(o: C) = unidirectional.compareTo(o.unidirectional)
	def ~==(other: C) = unidirectional ~== other.unidirectional
	
	def *(modifier: Double) =
		if (modifier >= 0) copy(absolute * modifier) else copy(absolute * (-1 * modifier), reverseDirection = true)
	def +(other: C) = this + other.unidirectional
	
	
	// OTHER	--------------------------
	
	/**
	  * @param other Rotation increment. Assumed to be towards the positive direction.
	  * @return A directional combination of these two rotations.
	  */
	def +(other: Rotation) = {
		val total = unidirectional + other
		if (total.sign == direction.sign)
			copy(total.abs)
		else
			copy(total.abs, reverseDirection = true)
	}
	/**
	  * @param other A non-directional rotation.
	  *              Assumed to be towards the positive direction
	  * @return Copy of this rotation decreased from its current direction by the specified amount.
	  */
	def -(other: Rotation) = this + (-other)
	/**
	  * @param other Another rotation instance
	  * @return Subtraction between these two rotations
	  */
	def -(other: C) =
		if (direction == other.direction) copy(absolute - other.absolute) else copy(absolute + other.absolute)
	
	/**
	  * @param direction Targeted direction
	  * @return Whether this rotation is towards that direction
	  */
	def isTowards[D >: Direction](direction: D) = this.direction == direction
	/**
	  * @param direction New rotation direction
	  * @return A copy of this rotation with the specified direction.
	  *         Returns a zero rotation for directions that are not "parallel" with this rotation
	  *         (i.e. directions that don't match the direction of this rotation, nor its opposite direction).
	  */
	def towards[D >: Direction <: Reversible[D]](direction: D) = {
		if (isTowards(direction))
			absolute
		else if (isTowards(-direction))
			-absolute
		else
			Rotation.zero
	}
	
	/**
	  * Converts this rotation to a rotation of the specified direction, preserving the resulting end angle.
	  *
	  * E.g. If this rotation is 40 degrees clockwise,
	  * converting it to a counter-clockwise rotation would yield 320 degrees counter-clockwise.
	  *
	  * When changing directions, full revolutions are preserved but reversed.
	  * E.g. If this is a rotation of 390 degrees counter-clockwise,
	  * the clockwise counterpart would be 360 + 330 = 690 degrees clockwise.
	  *
	  * @param direction Direction of the resulting rotation
	  * @tparam D Type of the direction used
	  *
	  * @return If this rotation is towards that direction already, returns this,
	  *         otherwise returns a rotation complementary to this one, leading to the same end angle.
	  *
	  * @see [[complementary]]
	  */
	def towardsPreservingEndAngle[D >: Direction](direction: D) =
		if (isTowards(direction)) self else complementary
	
	/**
	  * Calculates the length of an arc produced by this rotation / angle over a specific circle
	  * @param circleRadius Radius of the circle
	  * @return Length of the arc produced by this rotation.
	  *         NB: May be negative.
	  */
	// Arc length = L * a/2*Pi, where L is the total circle radius 2*Pi*r and a is the angle produced by this rotation
	// Therefore arc length = r * a
	def arcLengthOver(circleRadius: Double) = {
		val length = absolute.arcLengthOver(circleRadius)
		if (isNotNegative)
			length
		else
			-length
	}
}