package utopia.terra.model.angular

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.{ApproxSelfEquals, Combinable, EqualsFunction}
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.terra.model.enumeration.CompassDirection
import utopia.terra.model.enumeration.CompassDirection._

object LatLong
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * An equality function that allows for some inaccuracy
	  */
	val approxEquals: EqualsFunction[LatLong] =
		(a, b) => { (a.latitude ~== b.latitude) && (a.longitude ~== b.longitude) }
	
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
		apply(Rotation.ofDegrees(latitude, North.rotationDirection), Angle.ofDegrees(East.sign * longitude))
}
/**
 * A point on the earth's surface expressed as latitude and longitude angular values
  * @author Mikko Hilpinen
  * @since 29.8.2023, v1.0
 * @param latitude  The latitude coordinate of this position in the GPS system.
 *                  0 means equator. 90 COUNTER-CLOCKWISE means the NORTH pole
 *                  and 90 CLOCKWISE means the SOUTH pole or rim.
  *                  Default = 0
 * @param longitude The longitude coordinate of this position in the GPS system.
 *                  0 means Greenwich, England. Positive ]0, 180[ values are towards the WEST
 *                  and negative ]180, 360[ towards the EAST.
  *                  Default = 0
  */
case class LatLong(latitude: Rotation = Rotation.zero, longitude: Angle = Angle.zero)
	extends Combinable[LatLongRotation, LatLong] with ApproxSelfEquals[LatLong]
{
	// ATTRIBUTES   --------------------------
	
	/**
	 * The latitude portion of this coordinate as degrees of rotation from the equator towards the NORTH [-90, 90].
	 * Negative values are used for south-side points.
	 */
	lazy val latitudeDegrees: Double = {
		// Corrects the amount to [-90, 90] degree range
		val base = latitude.degreesTowards(North.rotationDirection)
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
	/**
	 * The longitude portion of this coordinate as rotation from Greenwich, England towards EAST [-180,180].
	 * Negative values represent points towards the west.
	 */
	lazy val longitudeDegrees: Double = {
		val base = longitude.degrees
		// Corrects to [-180, 180] range
		val corrected = if (base <= 180) base else base - 360
		// Returns in correct direction (EASTWARD)
		East.sign * corrected
	}
	
	/**
	  * The side of the equator on which this location resides (North or South).
	  * If this location resides exactly at the equator, returns North.
	  */
	lazy val northSouth = latitude.sign.binary match {
		case Some(sign) =>
			// Handles cases where the rotation is > 90 degrees or < -90 degrees
			// After 180 degrees rotation, the targeted hemisphere "flips"
			val absQuarter = if ((latitude.absoluteRadians / math.Pi).toInt % 4 < 2) South else North
			absQuarter * sign
		// 0 degrees latitude is considered to be on the North side
		case None => North
	}
	/**
	  * This location's relative direction from longitude 0 (i.e. Greenwich, England)
	  */
	// 0 degrees is considered West, 180 degrees is considered East
	lazy val eastWest: EastWest = if (longitude.radians < math.Pi) West else East
	
	
	// COMPUTED -----------------------
	
	/**
	 * @return The latitude and longitude coordinates of this point, as degrees
	 *         (between lat: -90 and 90, and lon: -180 and 180)
	 */
	def latLongDegrees = Pair(latitudeDegrees, longitudeDegrees)
	
	/**
	  * @return A two-dimensional rotation based on this location.
	  *         Same as calling this - LatLong.origin
	  */
	def toRotation = LatLongRotation(NorthSouth(latitude), EastWest(longitude.toShortestRotation))
	
	
	// IMPLEMENTED  -------------------
	
	override def self: LatLong = this
	
	override implicit def equalsFunction: EqualsFunction[LatLong] = LatLong.approxEquals
	
	override def toString = s"${latitudeDegrees.abs} $northSouth, ${longitudeDegrees.abs} $eastWest"
	
	/**
	 * @param rotation Amount of rotation to apply (north-to-south & east-to-west)
	 * @return Copy of this coordinate shifted by the specified amount
	 */
	override def +(rotation: LatLongRotation) =
		copy(latitude + rotation.northSouth, longitude + rotation.eastWest)
	
	
	// OTHER    -----------------------
	
	/**
	 * @param amount Targeted axis and the rotation to apply
	 * @return Copy of this coordinate shifted by the specified amount
	 */
	def +(amount: CompassRotation) = amount.compassAxis match {
		case NorthSouth => copy(latitude = latitude + amount)
		case EastWest => copy(longitude = longitude + amount)
	}
	
	/**
	 * @param other Another latitude longitude -coordinate
	 * @return The angular difference between these two coordinate points.
	  *        I.e. the amount of rotation required so that 'other' + rotation = this.
	 */
	def -(other: LatLong) =
		LatLongRotation(NorthSouth(latitude - other.latitude), EastWest(longitude - other.longitude))
	/**
	 * @param rotation Amount of rotation to subtract (north-to-south & east-to-west)
	 * @return Copy of this coordinate shifted by the specified amount (to opposite direction)
	 */
	def -(rotation: LatLongRotation) = this + (-rotation)
	/**
	 * @param amount Targeted axis and the rotation to subtract
	 * @return Copy of this coordinate shifted by the specified amount (to the opposite direction)
	 */
	def -(amount: CompassRotation) = this + (-amount)
	
	/**
	 * @param direction Targeted direction
	 * @return The component of this coordinate that matches the specified direction,
	 *         represented with a Rotation instance
	 */
	def apply(direction: CompassDirection) = direction.axis match {
		case NorthSouth => latitude
		case EastWest => longitude.toShortestRotation
	}
}
