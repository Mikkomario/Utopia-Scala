package utopia.terra.model.world.circle

import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.terra.controller.coordinate.world.{CircleOfEarth, WorldPointFactory}
import utopia.terra.model.angular.LatLong
import utopia.terra.model.world.WorldDistance

/**
  * Used for constructing new circle world points
  * @author Mikko Hilpinen
  * @since 12.11.2023, v1.1
  */
object CirclePoint extends WorldPointFactory[Vector2D, Vector3D, CircleSurfacePoint, AerialCirclePoint]
{
	override protected implicit def worldView: CircleOfEarth.type = CircleOfEarth
	
	override def apply(latLong: LatLong): CircleSurfacePoint = CircleSurfacePoint(latLong)
	override def apply(latLong: LatLong, altitude: WorldDistance): AerialCirclePoint =
		AerialCirclePoint(latLong, altitude)
	
	override def surfaceVector(vector: Vector2D): CircleSurfacePoint = CircleSurfacePoint(vector)
	override def aerialVector(vector: Vector3D): AerialCirclePoint = AerialCirclePoint(vector)
}
