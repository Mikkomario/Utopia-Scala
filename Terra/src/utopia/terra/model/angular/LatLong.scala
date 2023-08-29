package utopia.terra.model.angular

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.{Combinable, Sign}
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.enumeration.RotationDirection.Counterclockwise
import utopia.terra.model.enumeration.CompassDirection
import utopia.terra.model.enumeration.CompassDirection.{CompassAxis, EastWest, NorthSouth}

object LatLong
{
	// ATTRIBUTES   ------------------------
	
	/**
	 * A latitude longitude -coordinate that lies on the equator at the same longitude as Greenwich, England
	 */
	val origin = apply(Rotation.zero, Angle.zero)
	
	
	// OTHER    ----------------------------
	
	/**
	 * Converts a latitude, longitude -pair into a map point
	 * @param latitude  Latitude as degrees to north from the equator (negative is south)
	 * @param longitude Longitude as an angle relative to the Greenwich, England, where positive direction is east
	 * @return A new map point
	 */
	def fromDegrees(latitude: Double, longitude: Double): LatLong =
		apply(Rotation.ofDegrees(latitude, Counterclockwise), Angle.ofDegrees(-longitude))
}
/**
 * A point on the earth's surface expressed as latitude and longitude angular values
  * @author Mikko Hilpinen
  * @since 29.8.2023, v1.0
 * @param latitude  The latitude coordinate of this position in the GPS system.
 *                  0 means equator. 90 COUNTER-CLOCKWISE means the NORTH pole
 *                  and 90 CLOCKWISE means the SOUTH pole or rim.
 * @param longitude The longitude coordinate of this position in the GPS system.
 *                  0 means Greenwich, England. Positive ]0, 180[ values are towards the WEST
 *                  and negative ]180, 360[ towards the EAST.
  */
case class LatLong(latitude: Rotation, longitude: Angle) extends Combinable[Pair[Rotation], LatLong]
{
	// ATTRIBUTES   --------------------------
	
	/**
	 * The latitude portion of this coordinate as degrees of rotation from the equator towards the NORTH [-90, 90].
	 * Negative values are used for south-side points.
	 */
	lazy val latitudeDegrees = {
		// Corrects the amount to [-90, 90] degree range
		val base = latitude.counterClockwiseDegrees
		val div = base.abs / 90.0
		val segment = div.toInt
		val remainder = div - segment
		val corrected = segment % 4 match {
			// Case: 0-90 degrees => Returns value as is
			case 0 => remainder
			// Case: 90-180 degrees => Goes from the "pole" towards the equator
			case 1 => 90.0 - remainder
			// Case: 180-270 degrees => Opposite latitude
			case 2 => -remainder
			// Case: 270-360 degrees => Goes from the opposite "pole" towards the equator
			case 3 => -(90.0 - remainder)
		}
		// Applies the correct direction
		Sign.of(base) * corrected
	}
	/**
	 * The longitude portion of this coordinate as rotation from Greenwich, England towards EAST [-180,180].
	 * Negative values represent points towards the west.
	 */
	lazy val longitudeDegrees = {
		val base = longitude.degrees
		// Corrects to [-180, 180] range
		val corrected = if (base <= 180) base else base - 360
		// Returns in correct direction (EASTWARD)
		-corrected
	}
	
	
	// COMPUTED -----------------------
	
	/**
	 * @return The latitude and longitude coordinates of this point, as degrees
	 *         (between lat: -90 and 90, and lon: -180 and 180)
	 */
	def latLongDegrees = Pair(latitudeDegrees, longitudeDegrees)
	
	
	// IMPLEMENTED  -------------------
	
	/**
	 * @param rotation Amount of rotation to apply (north-to-south & east-to-west)
	 * @return Copy of this coordinate shifted by the specified amount
	 */
	override def +(rotation: Pair[Rotation]) = copy(latitude + rotation.first, longitude + rotation.second)
	
	
	// OTHER    -----------------------
	
	/**
	 * @param amount Targeted axis and the rotation to apply
	 * @return Copy of this coordinate shifted by the specified amount
	 */
	def +(amount: (CompassAxis, Rotation)) = amount._1 match {
		case NorthSouth => copy(latitude = latitude + amount._2)
		case EastWest => copy(longitude = longitude + amount._2)
	}
	
	/**
	 * @param other Another latitude longitude -coordinate
	 * @return The angular difference between these two coordinate points
	 */
	def -(other: LatLong) = Pair(latitude - other.latitude, longitude - other.longitude)
	/**
	 * @param rotation Amount of rotation to subtract (north-to-south & east-to-west)
	 * @return Copy of this coordinate shifted by the specified amount (to opposite direction)
	 */
	def -(rotation: Pair[Rotation]) = this + rotation.map { -_ }
	/**
	 * @param amount Targeted axis and the rotation to subtract
	 * @return Copy of this coordinate shifted by the specified amount (to the opposite direction)
	 */
	def -(amount: (CompassAxis, Rotation)) = this + (amount._1 -> -amount._2)
	
	/**
	 * @param direction Targeted direction
	 * @return The component of this coordinate that matches the specified direction,
	 *         represented with a Rotation instance
	 */
	def apply(direction: CompassDirection) = direction.axis match {
		case NorthSouth => latitude
		case EastWest => longitude.toRotation
	}
}
