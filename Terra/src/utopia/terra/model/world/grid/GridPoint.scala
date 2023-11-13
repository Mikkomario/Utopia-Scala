package utopia.terra.model.world.grid

import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.terra.controller.coordinate.world.{GridArea, WorldPointFactory}
import utopia.terra.model.angular.LatLong
import utopia.terra.model.world.WorldDistance

import scala.annotation.unused
import scala.language.implicitConversions

/**
  * Used for constructing grid points
  * @author Mikko Hilpinen
  * @since 12.11.2023, v1.1
  */
object GridPoint
{
	// IMPLICIT ---------------------
	
	implicit def accessFactory(@unused g: GridPoint.type)(implicit grid: GridArea): GridPointFactory =
		new GridPointFactory()
	
	
	// NESTED   ---------------------
	
	class GridPointFactory(override protected implicit val worldView: GridArea)
		extends WorldPointFactory[Vector2D, Vector3D, GridSurfacePoint, AerialGridPoint]
	{
		override def apply(latLong: LatLong): GridSurfacePoint = GridSurfacePoint(latLong)
		override def apply(latLong: LatLong, altitude: WorldDistance): AerialGridPoint =
			AerialGridPoint(latLong, altitude)
		
		override def surfaceVector(vector: Vector2D): GridSurfacePoint = GridSurfacePoint(vector)
		override def aerialVector(vector: Vector3D): AerialGridPoint = AerialGridPoint(vector)
	}
}
