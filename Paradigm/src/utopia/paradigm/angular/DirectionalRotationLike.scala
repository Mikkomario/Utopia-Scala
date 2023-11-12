package utopia.paradigm.angular

import utopia.flow.operator._


/**
* Common trait for rotations around a some axis, which specify a direction from binary options
* @author Mikko Hilpinen
* @since 11.11.2023, v1.5
  * @tparam Direction Type of rotation direction used
  * @tparam C Highest comparable rotation class
  * @tparam Repr Implementing class
**/
trait DirectionalRotationLike[+Direction <: Signed[Direction], -C <: DirectionalRotationLike[_, _, _], +Repr]
	extends LinearScalable[Repr] with Combinable[C, Repr] with CanBeAboutZero[C, Repr]
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
	
	
	// PROPS    --------------------------
	
	/**
	  * @return Copy of this rotation without direction information.
	  *         The resulting rotation amount may be negative.
	  */
	def unidirectional = absolute * direction.sign
	
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
	
	
	// IMPLEMENTED    --------------------
	
	override def sign: SignOrZero = direction.sign
	
	override def isZero = absolute.isZero
	override def isAboutZero: Boolean = absolute.isAboutZero
	
	override def toString = s"$absolute $direction"
	
	override def compareTo(o: C) = unidirectional.compareTo(o.unidirectional)
	def ~==(other: C) = unidirectional ~== other.unidirectional
	
	def *(modifier: Double) =
		if (modifier >= 0) copy(absolute * modifier) else copy(absolute * (-1 * modifier), reverseDirection = true)
	def +(other: C) = {
		if (direction == other.direction)
			copy(absolute + other.absolute)
		else
			this - other.absolute
	}
	
	
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