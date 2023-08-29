package utopia.terra.model.enumeration

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.flow.operator.{BinarySigned, Sign}
import utopia.paradigm.angular.Rotation
import utopia.paradigm.enumeration.RotationDirection
import utopia.terra.model.enumeration.CompassDirection.CompassAxis

/**
 * Common trait for the four compass directions: NORTH, SOUTH, EAST and WEST
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 */
sealed trait CompassDirection extends BinarySigned[CompassDirection]
{
	// ABSTRACT --------------------
	
	/**
	 * @return Axis along which this direction runs
	 */
	def axis: CompassAxis
	
	
	// COMPUTED --------------------
	
	/**
	 * @return The rotation direction that matches this compass direction
	 *         (assuming angular coordinate system)
	 */
	def rotationDirection = RotationDirection(sign)
	
	
	// OTHER    --------------------
	
	/**
	 * @param rotation Amount of rotation to apply towards this direction in degrees
	 * @return A rotation instance based with this direction and the specified amount
	 */
	def degrees(rotation: Double) = Rotation.ofDegrees(rotation, rotationDirection)
}

object CompassDirection
{
	// NESTED   ------------------------------
	
	object CompassAxis
	{
		/**
		 * All compass axes (east-west & north-south)
		 */
		val values = Pair(EastWest, NorthSouth)
	}
	/**
	 * Common trait for the two axes used in compass directions,
	 * i.e. the north-to-south axis and the east-to-west axis.
	 */
	sealed trait CompassAxis
	{
		/**
		 * @param sign targeted sign (positive | negative)
		 * @return Direction that matches that sign on this axis
		 */
		def apply(sign: Sign): CompassDirection
	}
	
	/**
	 * An axis that goes from the north (-) to the south (+).
	 * Used in latitude coordinates.
	 * Zero is considered to be at the equator.
	 */
	case object NorthSouth extends CompassAxis
	{
		// ATTRIBUTES   -----------------
		
		/**
		 * All north-to-south directions (north & south)
		 */
		val values = Pair(North, South)
		
		
		// IMPLEMENTED  -----------------
		
		override def apply(sign: Sign): NorthSouth = sign match {
			case Positive => South
			case Negative => North
		}
	}
	/**
	 * Common trait for the north-to-south directions (i.e. northward and southward)
	 */
	sealed trait NorthSouth extends CompassDirection with BinarySigned[NorthSouth]
	{
		override def self: NorthSouth = this
		override def axis = NorthSouth
	}
	/**
	 * The northward direction (negative)
	 */
	case object North extends NorthSouth
	{
		override def sign: Sign = Negative
		
		override def unary_- : NorthSouth = South
	}
	/**
	 * The southward direction (positive)
	 */
	case object South extends NorthSouth
	{
		override def sign: Sign = Positive
		
		override def unary_- : NorthSouth = North
	}
	
	/**
	 * A circular axis that goes from the east (-) to the west (+)
	 */
	case object EastWest extends CompassAxis
	{
		// ATTRIBUTES   ----------------------
		
		/**
		 * All east-to-west values (east & west)
		 */
		val values = Pair(East, West)
		
		
		// IMPLEMENTED  ----------------------
		
		override def apply(sign: Sign): EastWest = sign match {
			case Positive => West
			case Negative => East
		}
	}
	/**
	 * Common trait for east-to-west directions
	 */
	sealed trait EastWest extends CompassDirection with BinarySigned[EastWest]
	{
		override def self: EastWest = this
		override def axis = EastWest
	}
	
	/**
	 * Direction eastward (circular, negative)
	 */
	case object East extends EastWest
	{
		override def sign: Sign = Negative
		override def unary_- : EastWest = West
	}
	/**
	 * Direction westward (circular, positive)
	 */
	case object West extends EastWest
	{
		override def sign: Sign = Positive
		override def unary_- : EastWest = East
	}
}