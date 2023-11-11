package utopia.terra.model.enumeration

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.flow.operator.{BinarySigned, Sign, Signed}
import utopia.paradigm.angular.{Rotation, RotationFactory}
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.{Axis2D, RotationDirection}
import utopia.paradigm.measurement.Distance
import utopia.terra.model.CompassTravel
import utopia.terra.model.angular.{CompassRotation, EastWestRotation, NorthSouthRotation}
import utopia.terra.model.enumeration.CompassDirection.CompassAxis

/**
 * Common trait for the four compass directions: NORTH, SOUTH, EAST and WEST
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 */
sealed trait CompassDirection extends BinarySigned[CompassDirection] with RotationFactory[CompassRotation]
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
	
	
	// IMPLEMENTED  ----------------
	
	/**
	  * @param rotation Amount of rotation to apply towards this direction in radians
	  * @return A rotation instance with this direction and the specified amount
	  */
	override def radians(rotation: Double) = CompassRotation(axis, Rotation(rotationDirection).radians(rotation))
	
	
	// OTHER    --------------------
	
	/**
	  * @param distance Travel distance
	  * @return Specified length of travel towards this direction
	  */
	def apply(distance: Distance) = CompassTravel(axis, sign * distance)
}

object CompassDirection
{
	// NESTED   ------------------------------
	
	sealed trait AxisToDirection[+D, +R] extends RotationFactory[R]
	{
		// ABSTRACT ------------------------
		
		/**
		  * @param sign targeted sign (positive | negative)
		  * @return Direction that matches that sign on this axis
		  */
		def apply(sign: Sign): D
		
		/**
		  * @param rotation Amount of rotation to apply along this axis
		  * @return The specified rotation along this axis
		  */
		def apply(rotation: Rotation): R
		
		
		// IMPLEMENTED  --------------------
		
		override def radians(rads: Double): R = apply(Rotation.clockwise.radians(rads))
		
		
		// OTHER    ------------------------
		
		/**
		  * @param direction Rotation direction
		  * @return Matching direction on this axis
		  */
		def apply(direction: RotationDirection): D = apply(direction.sign)
		
		/**
		  * @param a A signed item
		  * @return Compass direction that matches that item's sign
		  */
		def of(a: BinarySigned[_]) = apply(a.sign)
		/**
		  * @param a A signed item
		  * @return Compass direction that matches that item's sign. None if that item is zero.
		  */
		def of(a: Signed[_]) = a.sign.binary.map(apply)
		/**
		  * @param n A number
		  * @return Compass direction that matches that numbers's sign. None if the number is zero.
		  */
		def of(n: Double) = Sign.of(n).binary.map(apply)
	}
	
	object CompassAxis
	{
		// ATTRIBUTES   ------------------------
		
		/**
		 * All compass axes (east-west & north-south)
		 */
		val values = Pair(EastWest, NorthSouth)
		
		
		// OTHER    ----------------------------
		
		/**
		  * @param axis A "traditional" 2D axis
		  * @return Compass axis that matches that axis
		  */
		def apply(axis: Axis2D): CompassAxis = axis match {
			case X => NorthSouth
			case Y => EastWest
		}
	}
	/**
	 * Common trait for the two axes used in compass directions,
	 * i.e. the north-to-south axis and the east-to-west axis.
	 */
	sealed trait CompassAxis extends AxisToDirection[CompassDirection, CompassRotation]
	{
		/**
		  * @return A 2D axis that matches this compass direction
		  */
		def axis: Axis2D
	}
	
	/**
	 * An axis that goes from the north (-) to the south (+).
	 * Used in latitude coordinates.
	 * Zero is considered to be at the equator.
	 */
	case object NorthSouth extends CompassAxis with AxisToDirection[NorthSouth, NorthSouthRotation]
	{
		// ATTRIBUTES   -----------------
		
		/**
		 * All north-to-south directions (north & south)
		 */
		val values = Pair(North, South)
		
		
		// IMPLEMENTED  -----------------
		
		// X because of the word "latitude", which implies lateral shift, which implies horizontal movement
		override def axis: Axis2D = X
		
		override def apply(sign: Sign): NorthSouth = sign match {
			case Positive => South
			case Negative => North
		}
		override def apply(rotation: Rotation) = NorthSouthRotation(rotation)
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
	case object EastWest extends CompassAxis with AxisToDirection[EastWest, EastWestRotation]
	{
		// ATTRIBUTES   ----------------------
		
		/**
		 * All east-to-west values (east & west)
		 */
		val values = Pair(East, West)
		
		
		// IMPLEMENTED  ----------------------
		
		override def axis: Axis2D = Y
		
		override def apply(sign: Sign): EastWest = sign match {
			case Positive => West
			case Negative => East
		}
		override def apply(rotation: Rotation) = EastWestRotation(rotation)
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