package utopia.terra.model.angular

import utopia.flow.operator.sign.Sign
import utopia.paradigm.angular.{BidirectionalRotationFactory, Rotation}
import utopia.terra.model.enumeration.CompassDirection.{North, NorthSouth, South}

object NorthSouthRotation extends BidirectionalRotationFactory[NorthSouth, NorthSouthRotation]
{
	// ATTRIBUTES   ------------------
	
	/**
	  * Factory used for constructing rotations towards the north
	  */
	lazy val north = apply(North)
	/**
	  * Factory used for constructing rotations towards the south
	  */
	lazy val south = apply(South)
	
	
	// IMPLEMENTED  ------------------
	
	override protected def directionForSign(sign: Sign): NorthSouth = NorthSouth(sign)
	
	override protected def _apply(absolute: Rotation, direction: NorthSouth): NorthSouthRotation =
		new NorthSouthRotation(absolute, direction)
}

/**
  * Represents rotation along the North-to-South axis.
  * Wraps a Rotation instance.
  * @author Mikko Hilpinen
  * @since 6.9.2023, v1.0
  */
class NorthSouthRotation private(override val absolute: Rotation, override val direction: NorthSouth)
	extends CompassRotationLike[NorthSouth, NorthSouthRotation] with CompassRotation
{
	// COMPUTED -------------------------
	
	/**
	  * @return The amount of this rotation towards the north. May be negative.
	  */
	def north = towards(North)
	/**
	  * @return The amount of this rotation towards the south. May be negative.
	  */
	def south = towards(South)
	
	
	// IMPLEMENTED  ---------------------
	
	override def self: NorthSouthRotation = this
	override def zero: NorthSouthRotation = NorthSouthRotation.zero
	
	override protected def copy(amount: Rotation, reverseDirection: Boolean) =
		new NorthSouthRotation(amount, if (reverseDirection) -direction else direction)
}
