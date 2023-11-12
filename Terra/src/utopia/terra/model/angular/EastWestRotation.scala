package utopia.terra.model.angular

import utopia.flow.operator.Sign
import utopia.paradigm.angular.{BidirectionalRotationFactory, DirectionalRotation, Rotation}
import utopia.terra.model.enumeration.CompassDirection.{East, EastWest, West}

object EastWestRotation extends BidirectionalRotationFactory[EastWest, EastWestRotation]
{
	// ATTRIBUTES   --------------------
	
	lazy val east = apply(East)
	lazy val west = apply(West)
	
	
	// IMPLEMENTED    ------------------------
	
	override protected def directionForSign(sign: Sign): EastWest = EastWest(sign)
	
	override protected def _apply(absolute: Rotation, direction: EastWest): EastWestRotation =
		new EastWestRotation(absolute, direction)
}

/**
  * Represents a rotation along the East-to-West axis.
  * Wraps a rotation instance
  * @author Mikko Hilpinen
  * @since 6.9.2023, v1.0
  */
class EastWestRotation(override val absolute: Rotation, override val direction: EastWest)
	extends CompassRotationLike[EastWest, EastWestRotation] with CompassRotation
{
	// COMPUTED ----------------------
	
	/**
	  * @return The amount of this rotation towards the east.
	  *         May be negative
	  */
	def east = towards(East)
	/**
	  * @return The amount of this rotation towards the east.
	  *         May be negative
	  */
	def west = towards(West)
	
	/**
	  * @return A directional (clockwise or counter-clockwise) rotation based on this rotation.
	  */
	def toDirectionalRotation = DirectionalRotation(direction.rotationDirection)(absolute)
	
	
	// IMPLEMENTED  ------------------
	
	override def self: EastWestRotation = this
	override def zero: EastWestRotation = EastWestRotation.zero
	
	override protected def copy(amount: Rotation, reverseDirection: Boolean) =
		new EastWestRotation(amount, if (reverseDirection) -direction else direction)
}
