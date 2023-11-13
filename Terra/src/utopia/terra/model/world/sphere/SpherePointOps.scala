package utopia.terra.model.world.sphere

import utopia.paradigm.shape.shape3d.Vector3D
import utopia.terra.controller.coordinate.world.SphericalEarth
import utopia.terra.model.CompassTravel
import utopia.terra.model.world.DoubleWorldPointOps

/**
  * Common trait for world points in the Spherical world system that provide mathematical functions
  * @author Mikko Hilpinen
  * @since 7.9.2023, v1.0
  */
trait SpherePointOps[P, +Aerial, +T] extends DoubleWorldPointOps[Vector3D, P, Aerial, T]
{
	// COMPUTED ------------------------------
	
	/**
	  * @return The implied world view (i.e. perfectly spherical earth)
	  */
	override protected implicit def worldView: SphericalEarth.type = SphericalEarth
	
	
	// IMPLEMENTED  --------------------------
	
	override def +(travel: CompassTravel): P = {
		// Converts the travel distance to rotation
		this + travel.compassAxis.rotation.forArcLength(worldView.vectorLengthOf(travel.distance), vector.length)
	}
}
