package utopia.terra.model.world.sphere

import utopia.paradigm.shape.shape3d.Vector3D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.terra.controller.coordinate.world.SphericalEarth
import utopia.terra.model.CompassTravel
import utopia.terra.model.world.WorldPoint
import utopia.terra.model.world.grid.DoubleWorldPointOps

/**
  * Common trait for world points in the Spherical world system that provide mathematical functions
  * @author Mikko Hilpinen
  * @since 7.9.2023, v1.0
  */
trait SpherePointOps[-WP <: WorldPoint[HasDoubleDimensions], +Repr] extends DoubleWorldPointOps[Vector3D, WP, Repr]
{
	// COMPUTED ------------------------------
	
	/**
	  * @return The implied world view (i.e. perfectly spherical earth)
	  */
	override protected implicit def worldView: SphericalEarth.type = SphericalEarth
	
	
	// IMPLEMENTED  --------------------------
	
	override def +(travel: CompassTravel): Repr = {
		// Converts the travel distance to rotation
		this + travel.compassAxis.rotation.forArcLength(worldView.vectorLengthOf(travel.distance), vector.length)
	}
}
