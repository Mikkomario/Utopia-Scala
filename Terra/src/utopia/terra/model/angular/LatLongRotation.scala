package utopia.terra.model.angular

import utopia.flow.collection.immutable.Pair
import utopia.paradigm.angular.Rotation
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.shape.template.{Dimensional, DimensionalBuilder, DimensionalFactory, Dimensions, FromDimensionsFactory, HasDimensions}
import utopia.terra.model.angular.LatLongRotation.dimensionsFactory
import utopia.terra.model.enumeration.CompassDirection
import utopia.terra.model.enumeration.CompassDirection.{CompassAxis, East, EastWest, North, NorthSouth, South, West}

object LatLongRotation
	extends DimensionalFactory[Rotation, LatLongRotation] with FromDimensionsFactory[Rotation, LatLongRotation]
{
	// ATTRIBUTES   ------------------------
	
	private val dimensionsFactory = Dimensions(zeroValue)
	
	/**
	  * Rotation of 0 degrees over both axes
	  */
	val zero = empty
	
	
	// COMPUTED ----------------------------
	
	private def zeroValue = Rotation.zero
	
	
	// IMPLEMENTED  ------------------------
	
	override def newBuilder: DimensionalBuilder[Rotation, LatLongRotation] =
		dimensionsFactory.newBuilder.mapResult(apply)
	
	override def apply(values: IndexedSeq[Rotation]): LatLongRotation = _apply(values)
	override def apply(values: Map[Axis, Rotation]): LatLongRotation = apply(
		values.get(NorthSouth.axis).map { South(_) }.getOrElse(South.zero),
		values.get(EastWest.axis).map { West(_) }.getOrElse(West.zero)
	)
	override def apply(dimensions: Dimensions[Rotation]): LatLongRotation = _apply(dimensions)
	
	override def from(other: HasDimensions[Rotation]): LatLongRotation = other match {
		case ll: LatLongRotation => ll
		case o => _apply(o.dimensions)
	}
	override def from(values: IterableOnce[Rotation]): LatLongRotation = values match {
		case s: Seq[Rotation] => _apply(s)
		case i =>
			val iter = i.iterator
			if (iter.hasNext) {
				val ns = South(iter.next())
				val ew = West(iter.nextOption().getOrElse(zeroValue))
				apply(ns, ew)
			}
			else
				zero
	}
	
	
	// OTHER    -----------------------------
	
	/**
	  * Creates a new 2-dimensional rotation by wrapping a one-dimensional rotation
	  * @param rotation A rotation to wrap
	  * @return A two-dimensional version of the specified rotation
	  */
	def apply(rotation: CompassRotation): LatLongRotation = rotation match {
		case ns: NorthSouthRotation => apply(ns, EastWestRotation.zero)
		case ew: EastWestRotation => apply(NorthSouthRotation.zero, ew)
		case r =>
			r.compassAxis match {
				case NorthSouth => apply(NorthSouthRotation(r.unidirectional))
				case EastWest => apply(EastWestRotation(r.unidirectional))
			}
	}
	
	/**
	  * Wraps a latitude-longitude degree pair
	  * @param latitudeNorth Rotation towards North in degrees
	  * @param longitudeEast Rotation towards East in degrees
	  * @return A new 2-dimensional rotation instance
	  */
	def degrees(latitudeNorth: Double, longitudeEast: Double) =
		apply(North.degrees(latitudeNorth), East.degrees(longitudeEast))
	/**
	  * @param latLong Latitude (1) and longitude (2) rotation, where
	  *                latitude is in degrees towards NORTH and
	  *                longitude is in degrees towards EAST.
	  * @return A new 2-dimensional rotation instance
	  */
	def degrees(latLong: Pair[Double]): LatLongRotation = degrees(latLong.first, latLong.second)
	
	private def _apply(values: Seq[Rotation]) = apply(
		NorthSouthRotation(values.headOption.getOrElse(zeroValue)),
		EastWestRotation(values.lift(1).getOrElse(zeroValue))
	)
}

/**
  * Represents rotation that potentially targets both the latitude (North-South) and longitude (East-West) axes.
  * @author Mikko Hilpinen
  * @since 6.9.2023, v1.0
  */
case class LatLongRotation(northSouth: NorthSouthRotation, eastWest: EastWestRotation)
	extends Dimensional[Rotation, LatLongRotation]
{
	// ATTRIBUTES   ------------------------
	
	override lazy val dimensions: Dimensions[Rotation] =
		dimensionsFactory(northSouth.unidirectional, eastWest.unidirectional)
	override lazy val components = Pair(northSouth, eastWest)
	
	
	// COMPUTED ----------------------------
	
	/**
	  * @return Latitude-longitude coordinates based on this rotation.
	  *         Same as adding this rotation to the (0,0) latitude-longitude location.
	  */
	def toLatLong = LatLong(northSouth, eastWest.toAngle)
	
	
	// IMPLEMENTED  ------------------------
	
	override def self: LatLongRotation = this
	
	override def x = northSouth.unidirectional
	override def y = eastWest.unidirectional
	
	override def toString = s"($northSouth, $eastWest)"
	
	override def withDimensions(newDimensions: Dimensions[Rotation]): LatLongRotation = LatLongRotation(newDimensions)
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param axis Targeted axis
	  * @return The rotation component that applies to that axis
	  */
	def apply(axis: CompassAxis) = axis match {
		case NorthSouth => northSouth
		case EastWest => eastWest
	}
	
	/**
	  * @param direction Targeted compass direction
	  * @return Amount of rotation applied towards that direction
	  */
	def towards(direction: CompassDirection) = apply(direction.axis).towards(direction)
	/**
	  * @param direction Targeted compass direction
	  * @return Amount of rotation in degrees applied towards that direction
	  */
	@deprecated("Please use .towards(CompassDirection).degrees instead", "v1.1")
	def degreesTowards(direction: CompassDirection) = towards(direction).degrees
}
