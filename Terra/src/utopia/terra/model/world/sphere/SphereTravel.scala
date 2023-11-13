package utopia.terra.model.world.sphere

import utopia.paradigm.shape.shape3d.Vector3D
import utopia.paradigm.shape.template.vector.DoubleVector
import utopia.terra.controller.coordinate.GlobeMath
import utopia.terra.controller.coordinate.world.SphericalEarth
import utopia.terra.model.world.{Travel, WorldDistance}

/**
  * Represents a travel path that works on the spherical earth model
  * @author Mikko Hilpinen
  * @since 13.11.2023, v1.1
  */
case class SphereTravel(start: SpherePoint, end: SpherePoint) extends Travel[Double, SpherePoint, Vector3D, DoubleVector]
{
	override implicit protected def worldView: SphericalEarth.type = SphericalEarth
	
	override def altitudeIncrease: WorldDistance = end.altitude - start.altitude
	
	override def arcingDistance: WorldDistance = GlobeMath
		.haversineDistanceBetween(ends.map { _.latLong }, ends.map { _.altitude }, worldView.globeRadius)
	
	override def apply(progress: Double) = arcingProgress(progress)
	override def arcingProgress(progress: Double): SpherePoint = arcingProgress3D(progress) { _.soarBy(_) }
}
