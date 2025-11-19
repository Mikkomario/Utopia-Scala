package utopia.terra.model.angular

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.range.Span
import utopia.flow.operator.combine.Combinable
import utopia.flow.operator.equality.{ApproxSelfEquals, EqualsFunction}
import utopia.paradigm.angular.{Angle, AngleRange, Rotation}
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.shape.template._
import utopia.terra.model.enumeration.CompassDirection._

object LatLong extends DimensionalFactory[Rotation, LatLong] with FromDimensionsFactory[Rotation, LatLong]
{
	import LatLongLike._
	
	// ATTRIBUTES   ------------------------
	
	/**
	  * An equality function that allows for some inaccuracy
	  */
	val approxEquals: EqualsFunction[LatLong] =
		(a, b) => { (a.latitude ~== b.latitude) && (a.longitude ~== b.longitude) }
	
	/**
	 * A latitude longitude -coordinate that lies on the equator at the same longitude as Greenwich, England
	 */
	val origin = apply(NorthSouthRotation.zero, Angle.zero)
	
	
	// IMPLEMENTED  ------------------------
	
	override def newBuilder: DimensionalBuilder[Rotation, LatLong] = dimensionsFactory.newBuilder.mapResult(apply)
	
	override def apply(values: IndexedSeq[Rotation]): LatLong = _apply(values)
	override def apply(values: Map[Axis, Rotation]): LatLong = {
		val latitude = values.get(NorthSouth.axis) match {
			case Some(rotation) => NorthSouth(rotation)
			case None => NorthSouthRotation.zero
		}
		val longitude = values.get(EastWest.axis) match {
			case Some(rotation) => rotation.toAngle
			case None => Angle.zero
		}
		apply(latitude, longitude)
	}
	override def apply(dimensions: Dimensions[Rotation]): LatLong = _apply(dimensions)
	
	override def from(values: IterableOnce[Rotation]): LatLong = values match {
		case s: Seq[Rotation] => _apply(s)
		case i => _apply(i.iterator.take(2).toSeq)
	}
	override def from(other: HasDimensions[Rotation]): LatLong = other match {
		case ll: LatLong => ll
		case llr: LatLongRotation => llr.toLatLong
		case o => _apply(o.dimensions)
	}
	
	
	// OTHER    ----------------------------
	
	/**
	 * Converts a latitude, longitude -pair into a map point
	 * @param latitude  Latitude as degrees to north from the equator (negative is south)
	 * @param longitude Longitude as an angle relative to the Greenwich, England, where positive direction is east
	 * @return A new map point
	 */
	def degrees(latitude: Double, longitude: Double): LatLong =
		apply(NorthSouthRotation.fromLatitudeDegrees(latitude), degreesToLongitude(longitude))
	/**
	  * @param latLongDegrees A pair containing the latitude (first, north) and the longitude (second, east) as degrees
	  * @return A coordinate that matches the specified degrees input
	  */
	def degrees(latLongDegrees: Pair[Double]): LatLong = degrees(latLongDegrees.first, latLongDegrees.second)
	@deprecated("Replaced with .degrees(Double, Double)", "v1.1")
	def fromDegrees(latitude: Double, longitude: Double): LatLong = degrees(latitude, longitude)
	
	/**
	 * @param locations A set of locations. Must not be empty.
	 * @return Returns the area covering all those locations.
	 *         The area is returned as 2 spans:
	 *              1. Span between the lowest & highest latitude coordinates
	 *              1. Span between the most extreme longitude angles
	 */
	@throws[NoSuchElementException]("If the specified collection is empty")
	def areaAround(locations: Iterable[LatLong]) = {
		val latitudeRange = locations.iterator.map { _.latitude }.minMax
		val longitudeRange = AngleRange.around { locations.map { _.longitude } }
		Span(latitudeRange) -> longitudeRange
	}
	/**
	 * @param locations A set of locations.
	 * @return Returns the area covering all those locations.
	 *         The area is returned as 2 spans:
	 *              1. Span between the lowest & highest latitude coordinates
	 *              1. Span between the most extreme longitude angles
	 *
	 *         Yields None if 'locations' is empty.
	 */
	def areaAroundOption(locations: Iterable[LatLong]) =
		if (locations.isEmpty) None else Some(areaAround(locations))
	
	/**
	 * @param longitude A longitude angle
	 * @return A longitude degrees value
	 */
	def longitudeToDegrees(longitude: Angle) = {
		val base = longitude.degrees
		// Corrects to [-180, 180] range
		val corrected = if (base <= 180) base else base - 360
		// Returns in correct direction (EASTWARD)
		East.sign * corrected
	}
	/**
	 * @param longitudeDegrees A longitude coordinate as degrees
	 * @return A longitude angle matching that value
	 */
	def degreesToLongitude(longitudeDegrees: Double) = Angle.degrees(East.sign * longitudeDegrees)
	
	private def _apply(values: Seq[Rotation]) = {
		if (values.isEmpty)
			origin
		else {
			val latitude = NorthSouthRotation(values.head)
			if (values hasSize 1)
				apply(latitude)
			else
				apply(latitude, values(1).toAngle)
		}
	}
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
case class LatLong(latitude: NorthSouthRotation = NorthSouthRotation.zero, longitude: Angle = Angle.zero)
	extends LatLongLike[LatLong] with Combinable[LatLongRotation, LatLong] with ApproxSelfEquals[LatLong]
{
	// ATTRIBUTES   --------------------------
	
	/**
	  * @return The longitude component of this coordinate represented as a rotation instance
	  */
	lazy val longitudeRotation = EastWestRotation(longitude.toShortestRotation.unidirectional)
	
	override lazy val dimensions = super.dimensions
	override lazy val components = super.components
	
	/**
	 * The longitude portion of this coordinate as rotation from Greenwich, England towards EAST [-180,180].
	 * Negative values represent points towards the west.
	 */
	lazy val longitudeDegrees: Double = LatLong.longitudeToDegrees(longitude)
	
	/**
	  * The side of the equator on which this location resides (North or South).
	  * If this location resides exactly at the equator, returns North.
	  */
	lazy val northSouthSide = latitude.sign.binary match {
		case Some(sign) =>
			// Handles cases where the rotation is > 90 degrees or < -90 degrees
			// After 180 degrees rotation, the targeted hemisphere "flips"
			val absQuarter = if ((latitude.absolute.radians / math.Pi).toInt % 4 < 2) South else North
			absQuarter * sign
		// 0 degrees latitude is considered to be on the North side
		case None => North
	}
	/**
	  * This location's relative direction from longitude 0 (i.e. Greenwich, England)
	  */
	// 0 degrees is considered West, 180 degrees is considered East
	lazy val eastWestSide: EastWest = if (longitude.radians < math.Pi) West else East
	
	
	// COMPUTED -----------------------
	
	/**
	 * The latitude portion of this coordinate as degrees of rotation from the equator towards the NORTH [-90, 90].
	 * Negative values are used for south-side points.
	 */
	def latitudeDegrees: Double = latitude.toLatitudeDegrees
	/**
	 * @return The latitude and longitude coordinates of this point, as degrees
	 *         (between lat: -90 and 90, and lon: -180 and 180)
	 */
	def latLongDegrees = Pair(latitudeDegrees, longitudeDegrees)
	
	/**
	  * @return A two-dimensional rotation based on this location.
	  *         Same as calling this - LatLong.origin
	  */
	def toRotation = LatLongRotation(northSouth, eastWest)
	
	
	// IMPLEMENTED  -------------------
	
	override def self: LatLong = this
	
	override def northSouth: NorthSouthRotation = latitude
	override def eastWest: EastWestRotation = longitudeRotation
	
	override implicit def approxEqualsFunction: EqualsFunction[LatLong] = LatLong.approxEquals
	
	override def toString = s"${latitudeDegrees.abs} $northSouthSide, ${longitudeDegrees.abs} $eastWestSide"
	
	override def withDimensions(newDimensions: Dimensions[Rotation]): LatLong = LatLong(newDimensions)
	
	/**
	 * @param rotation Amount of rotation to apply (north-to-south & east-to-west)
	 * @return Copy of this coordinate shifted by the specified amount
	 */
	override def +(rotation: LatLongRotation) =
		copy(latitude + rotation.northSouth, longitude + rotation.eastWest.toDirectionalRotation)
	
	
	// OTHER    -----------------------
	
	/**
	 * @param other Another latitude longitude -coordinate
	 * @return The angular difference between these two coordinate points.
	  *        I.e. the amount of rotation required so that 'other' + rotation = this.
	 */
	def -(other: LatLong) =
		LatLongRotation(latitude - other.latitude, EastWestRotation(longitude - other.longitude))
}
