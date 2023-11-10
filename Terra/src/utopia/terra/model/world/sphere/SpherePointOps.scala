package utopia.terra.model.world.sphere

import utopia.paradigm.angular.Rotation
import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.vector.DoubleVector
import utopia.terra.controller.coordinate.world.SphericalEarth
import utopia.terra.model.CompassTravel
import utopia.terra.model.angular.{CompassRotation, LatLong, LatLongRotation}
import utopia.terra.model.world.{WorldPoint, WorldPointOps}

/**
  * Common trait for world points in the Spherical world system that provide mathematical functions
  * @author Mikko Hilpinen
  * @since 7.9.2023, v1.0
  */
trait SpherePointOps[-WP <: WorldPoint[HasDoubleDimensions], +Repr]
	extends WorldPointOps[Vector3D, WP, DoubleVector, Repr]
{
	// ABSTRACT ------------------------------
	
	/**
	  * @param latLong New latitude-longitude coordinates
	  * @return Copy of this point that resides over the specified coordinates
	  */
	protected def at(latLong: LatLong): Repr
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return The implied world view (i.e. perfectly spherical earth)
	  */
	protected implicit def worldView: SphericalEarth.type = SphericalEarth
	
	
	// IMPLEMENTED  --------------------------
	
	override def linearDistanceFrom(other: WP): Distance = worldView.distanceOf((vector - other.vector).length)
	
	override def +(travel: CompassTravel): Repr = {
		// Converts the travel distance to rotation
		val rotation = Rotation.forArcLength(worldView.vectorLengthOf(travel.distance), vector.length)
		this + travel.compassAxis(rotation)
	}
	override def +(rotation: CompassRotation): Repr = at(latLong + rotation)
	override def +(other: LatLongRotation): Repr = at(latLong + other)
	override def +(vectorTravel: DoubleVector): Repr = ???
}
