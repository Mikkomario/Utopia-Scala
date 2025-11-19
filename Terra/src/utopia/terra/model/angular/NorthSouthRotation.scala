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
		
	
	// OTHER    ----------------------
	
	/**
	 * @param latitudeDegrees A latitude coordinate as a value in degrees
	 * @return A north rotation representing the specified latitude coordinate
	 */
	def fromLatitudeDegrees(latitudeDegrees: Double) =
		apply(Rotation.degrees(latitudeDegrees), North)
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
	// ATTRIBUTES   ---------------------
	
	/**
	 * This rotation as a degree-based latitude coordinate
	 * (assuming that this rotation originates from the equator / 0 latitude)
	 */
	lazy val toLatitudeDegrees = {
		// Corrects the amount to [-90, 90] degree range
		val base = north.degrees
		val div = base.abs / 90.0
		val segment = div.toInt
		val remainder = base % 90.0
		/*val corrected = */segment % 4 match {
			// Case: 0-90 degrees => Returns value as is
			case 0 => remainder
			// Case: 90-180 degrees => Goes from the "pole" towards the equator
			case 1 => 90.0 - remainder
			// Case: 180-270 degrees => Opposite latitude
			case 2 => -remainder
			// Case: 270-360 degrees => Goes from the opposite "pole" towards the equator
			case 3 => -(90.0 - remainder)
		}
	}
	
	
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
