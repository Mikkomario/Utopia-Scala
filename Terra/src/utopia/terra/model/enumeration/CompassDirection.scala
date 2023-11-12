package utopia.terra.model.enumeration

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.flow.operator.{BinarySigned, Sign, Signed}
import utopia.paradigm.angular.{BidirectionalRotationFactory, DirectionalRotation, Rotation, RotationFactory}
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.RotationDirection.{Clockwise, Counterclockwise}
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
	
	/**
	  * @param rotation Amount of rotation to apply (towards this direction)
	  * @return Specified rotation towards this direction
	  */
	def apply(rotation: Rotation): CompassRotation
	
	
	// IMPLEMENTED  ----------------
	
	/**
	  * @param rotation Amount of rotation to apply towards this direction in radians
	  * @return A rotation instance with this direction and the specified amount
	  */
	override def radians(rotation: Double) = CompassRotation(this).radians(rotation)
	
	
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
	
	sealed trait AxisToDirection[+D]
	{
		// ABSTRACT ------------------------
		
		/**
		  * @param sign targeted sign (positive | negative)
		  * @return Direction that matches that sign on this axis
		  */
		def apply(sign: Sign): D
		
		
		// OTHER    ------------------------
		
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
	sealed trait CompassAxis extends AxisToDirection[CompassDirection]
	{
		// ABSTRACT ------------------------
		
		/**
		  * @return A 2D axis that matches this compass direction
		  */
		def axis: Axis2D
		
		/**
		  * @return Factory used for constructing rotation instances along this axis
		  */
		def rotation: BidirectionalRotationFactory[_ <: CompassDirection, CompassRotation]
		
		
		// OTHER    ----------------------
		
		/**
		  * @param rotation A rotation to apply to this axis
		  * @return Specified rotation along this axis
		  */
		def apply(rotation: Rotation) = this.rotation(rotation)
	}
	
	/**
	 * An axis that goes from the north (-) to the south (+).
	 * Used in latitude coordinates.
	 * Zero is considered to be at the equator.
	 */
	case object NorthSouth extends CompassAxis with AxisToDirection[NorthSouth]
	{
		// ATTRIBUTES   -----------------
		
		/**
		 * All north-to-south directions (north & south)
		 */
		val values = Pair(North, South)
		
		
		// IMPLEMENTED  -----------------
		
		// X because of the word "latitude", which implies lateral shift, which implies horizontal movement
		override def axis: Axis2D = X
		
		override def rotation = NorthSouthRotation
		
		override def apply(sign: Sign): NorthSouth = sign match {
			case Positive => South
			case Negative => North
		}
		
		override def apply(rotation: Rotation) = NorthSouthRotation(rotation)
	}
	/**
	 * Common trait for the north-to-south directions (i.e. northward and southward)
	 */
	sealed trait NorthSouth
		extends CompassDirection with BinarySigned[NorthSouth] with RotationFactory[NorthSouthRotation]
	{
		// IMPLEMENTED  ------------------------
		
		override def self: NorthSouth = this
		override def axis = NorthSouth
		
		override def apply(rotation: Rotation): NorthSouthRotation = NorthSouthRotation(this)(rotation)
		override def radians(rotation: Double) = NorthSouthRotation(this).radians(rotation)
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
	case object EastWest extends CompassAxis with AxisToDirection[EastWest]
	{
		// ATTRIBUTES   ----------------------
		
		/**
		 * All east-to-west values (east & west)
		 */
		val values = Pair(East, West)
		
		
		// IMPLEMENTED  ----------------------
		
		override def axis: Axis2D = Y
		
		override def rotation = EastWestRotation
		
		override def apply(sign: Sign): EastWest = sign match {
			case Positive => West
			case Negative => East
		}
		
		
		// OTHER    --------------------------
		
		/**
		  * @param direction A rotation direction
		  * @return The east-west direction that matches that clock/sun rotation direction
		  */
		def apply(direction: RotationDirection): EastWest = direction match {
			case Clockwise => West
			case Counterclockwise => East
		}
		override def apply(rotation: Rotation) = EastWestRotation(rotation)
		/**
		  * @param rotation Rotation to convert
		  * @return An east-west rotation that matches the specified rotation
		  */
		def apply(rotation: DirectionalRotation): EastWestRotation = EastWestRotation(rotation)
	}
	/**
	 * Common trait for east-to-west directions
	 */
	sealed trait EastWest extends CompassDirection with BinarySigned[EastWest] with RotationFactory[EastWestRotation]
	{
		// COMPUTED --------------------------
		
		/**
		  * @return The rotation direction that matches this compass direction
		  *         (assuming angular coordinate system)
		  */
		def rotationDirection = RotationDirection(sign)
		
		
		// IMPLEMENTED  ----------------------
		
		override def self: EastWest = this
		override def axis = EastWest
		
		override def apply(rotation: Rotation): EastWestRotation = EastWestRotation(this)(rotation)
		override def radians(rotation: Double) = EastWestRotation(this).radians(rotation)
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