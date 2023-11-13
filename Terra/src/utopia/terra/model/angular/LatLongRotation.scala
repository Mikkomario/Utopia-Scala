package utopia.terra.model.angular

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.combine.LinearScalable
import utopia.paradigm.angular.Rotation
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.shape.template.{DimensionalBuilder, DimensionalFactory, Dimensions, FromDimensionsFactory, HasDimensions}
import utopia.terra.model.enumeration.CompassDirection
import utopia.terra.model.enumeration.CompassDirection.{East, EastWest, North, NorthSouth, South, West}

object LatLongRotation
	extends DimensionalFactory[Rotation, LatLongRotation] with FromDimensionsFactory[Rotation, LatLongRotation]
{
	import LatLongLike._
	
	// ATTRIBUTES   ------------------------
	
	/**
	  * Rotation of 0 degrees over both axes
	  */
	val zero = empty
	
	
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
		case llr: LatLongRotation => llr
		case ll: LatLong => ll.toRotation
		case o => _apply(o.dimensions)
	}
	override def from(values: IterableOnce[Rotation]): LatLongRotation = values match {
		case s: Seq[Rotation] => _apply(s)
		case i => _apply(i.iterator.take(2).toSeq)
	}
	
	
	// OTHER    -----------------------------
	
	/**
	  * Creates a new 2-dimensional rotation by wrapping a one-dimensional rotation
	  * @param rotation A rotation to wrap
	  * @return A two-dimensional version of the specified rotation
	  */
	def apply(rotation: CompassRotation): LatLongRotation = rotation match {
		case ns: NorthSouthRotation => apply(ns)
		case ew: EastWestRotation => apply(eastWest = ew)
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
	
	private def _apply(values: Seq[Rotation]) = {
		if (values.isEmpty)
			zero
		else {
			val northSouth = NorthSouth(values.head)
			if (values hasSize 1)
				apply(northSouth)
			else
				apply(northSouth, EastWest(values(1)))
		}
	}
}

/**
  * Represents rotation that potentially targets both the latitude (North-South) and longitude (East-West) axes.
  * @author Mikko Hilpinen
  * @since 6.9.2023, v1.0
  */
case class LatLongRotation(northSouth: NorthSouthRotation = NorthSouthRotation.zero,
                           eastWest: EastWestRotation = EastWestRotation.zero)
	extends LatLongLike[LatLongRotation] with LinearScalable[LatLongRotation]
{
	// ATTRIBUTES   ------------------------
	
	override lazy val dimensions: Dimensions[Rotation] = super.dimensions
	override lazy val components = super.components
	
	
	// COMPUTED ----------------------------
	
	/**
	  * @return Latitude-longitude coordinates based on this rotation.
	  *         Same as adding this rotation to the (0,0) latitude-longitude location.
	  */
	def toLatLong = LatLong(northSouth, eastWest.toAngle)
	
	
	// IMPLEMENTED  ------------------------
	
	override def self: LatLongRotation = this
	
	override def withDimensions(newDimensions: Dimensions[Rotation]): LatLongRotation = LatLongRotation(newDimensions)
	
	override def *(mod: Double): LatLongRotation = LatLongRotation(northSouth * mod, eastWest * mod)
	
	
	// OTHER    ---------------------------
	
	@deprecated("Please use .apply(CompassDirection) instead", "v1.1")
	def towards(direction: CompassDirection) = apply(direction)
	
	/**
	  * @param direction Targeted compass direction
	  * @return Amount of rotation in degrees applied towards that direction
	  */
	@deprecated("Please use .apply(CompassDirection).degrees instead", "v1.1")
	def degreesTowards(direction: CompassDirection) = towards(direction).degrees
}
