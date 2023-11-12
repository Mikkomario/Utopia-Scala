package utopia.terra.model.world.grid

import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.terra.controller.coordinate.world.GridArea
import utopia.terra.model.angular.LatLong
import utopia.terra.model.world.AerialPoint

object GridPoint
{
	// OTHER    ---------------------
	
	def apply(vector: Vector3D)(implicit grid: GridArea): GridPoint = new VectorOverGrid(vector)
	def apply(latLong: LatLong, altitude: Distance)(implicit grid: GridArea): GridPoint =
		new LatLongOverGrid(latLong, altitude)
	
	
	// NESTED   ---------------------
	
	class VectorOverGrid(override val vector: Vector3D)(implicit override val grid: GridArea) extends GridPoint
	{
		// ATTRIBUTES   -------------
		
		private lazy val surface = GridSurfacePoint(vector.in2D)
		
		
		// IMPLEMENTED  -------------
		
		override def latLong: LatLong = surface.latLong
		override def altitude: Distance = grid.distanceOf(vector.z)
		
		override def toSurfacePoint: GridSurfacePoint = surface
	}
	class LatLongOverGrid(override val latLong: LatLong, override val altitude: Distance)
	                     (implicit override val grid: GridArea)
		extends GridPoint
	{
		// ATTRIBUTES   -----------------
		
		override lazy val vector: Vector3D = {
			val surfaceVector = grid.latLongToVector(latLong)
			Vector3D(surfaceVector.x, surfaceVector.y, grid.vectorLengthOf(altitude))
		}
		
		
		// IMPLEMENTED  -----------------
		
		override def toSurfacePoint: GridSurfacePoint = GridSurfacePoint(latLong)
	}
}

/**
  * Represents a world location in a grid-based model
  * using three dimensions.
  * @author Mikko Hilpinen
  * @since 2.11.2023, v1.0.1
  */
trait GridPoint
	extends AerialPoint[Vector3D, GridSurfacePoint] with GridPointOps[Vector3D, GridPoint]
{
	override protected def at(location: HasDoubleDimensions): GridPoint = GridPoint(Vector3D.from(location))
	override protected def at(latLong: LatLong): GridPoint = GridPoint(latLong, altitude)
}
