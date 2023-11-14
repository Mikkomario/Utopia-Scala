package utopia.paradigm.angular

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.operator.sign.Sign
import utopia.paradigm.enumeration.RotationDirection
import utopia.paradigm.enumeration.RotationDirection.{Clockwise, Counterclockwise}
import utopia.paradigm.generic.ParadigmDataType.RotationType

object DirectionalRotation extends BidirectionalRotationFactory[RotationDirection, DirectionalRotation]
{
	// ATTRIBUTES	-----------------------
	
	/**
	  * A zero rotation
	  */
	override val zero = new DirectionalRotation(Rotation.zero, Clockwise)
	
	/**
	  * A factory used for constructing clockwise rotations
	  */
	val clockwise = apply(Clockwise)
	/**
	  * A factory used for constructing counter-clockwise rotations
	  */
	val counterclockwise = apply(Counterclockwise)
	
	
	// IMPLEMENTED  ----------------------
	
	override protected def directionForSign(sign: Sign): RotationDirection = RotationDirection(sign)
	
	override protected def _apply(absolute: Rotation, direction: RotationDirection): DirectionalRotation =
		new DirectionalRotation(absolute, direction)
	
	
	// OTHER	--------------------------
	
	/**
	  * @param rotations A number of rotations
	  * @return An average between the rotations
	  */
	def average(rotations: Iterable[DirectionalRotation]): DirectionalRotation =
		rotations.emptyOneOrMany match {
			case None => clockwise.zero
			case Some(Left(rotation)) => rotation
			case Some(Right(rotations)) => rotations.reduce { _ + _ } / rotations.size
		}
}

/**
* This class represents a rotation that's either clockwise or counter-clockwise
* @author Mikko Hilpinen
* @since Genesis 21.11.2018
  * @param absolute The absolute rotation amount (i.e. rotation without direction)
  * @param direction The rotation direction (default = clockwise)
**/
case class DirectionalRotation private(absolute: Rotation, direction: RotationDirection = Clockwise)
	extends DirectionalRotationLike[RotationDirection, DirectionalRotation, DirectionalRotation]
		with SelfComparable[DirectionalRotation] with ValueConvertible
{
	// ATTRIBUTES   ----------------------
	
	override lazy val unidirectional = super.unidirectional
	
	/**
	  * @return Counter-clockwise amount of this rotation
	  */
	lazy val counterClockwise = -clockwise
	
	
	// COMPUTED    --------------------------
	
	/**
	  * @return Clockwise amount of this rotation
	  */
	def clockwise = unidirectional
	
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
	  * @return Whether this rotation is towards the clockwise direction
	  */
	def isClockwise = direction == Clockwise
	/**
	  * @return Whether this rotation is towards the counter clockwise direction
	  */
	def isCounterClockwise = direction == Counterclockwise
	
	
	// IMPLEMENTED    --------------------
	
	override def self = this
	
	override def zero = DirectionalRotation.zero
	
	override implicit def toValue: Value = new Value(Some(this), RotationType)
	
	override protected def copy(amount: Rotation, reverseDirection: Boolean) =
		DirectionalRotation(amount, if (reverseDirection) -direction else direction)
	
	
	// OTHER	--------------------------
	
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
	  * Calculates the length of an arc produced by this rotation / angle over a specific circle
	  * @param circleRadius Radius of the circle
	  * @param positiveDirection Rotation direction that produces a positive arc length.
	  *                          Default = positive rotation direction (i.e. Clockwise),
	  *                          which means that by default every result is positive.
	  * @return Length of the arc produced by this rotation.
	  *         NB: May be negative.
	  */
	@deprecated("Please use .arcLengthOver(Double) instead. This version will be removed in the future", "v1.5")
	def arcLengthOver(circleRadius: Double, positiveDirection: RotationDirection): Double = {
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