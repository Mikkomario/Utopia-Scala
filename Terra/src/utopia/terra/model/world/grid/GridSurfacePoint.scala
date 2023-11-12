package utopia.terra.model.world.grid

import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.terra.controller.coordinate.world.GridArea
import utopia.terra.model.angular.LatLong
import utopia.terra.model.world.SurfacePoint

object GridSurfacePoint
{
	// OTHER    ---------------------
	
	/**
	  * Converts a vector into a grid surface point
	  * @param vector A vector to convert
	  * @param grid Grid to assume implicitly
	  * @return A new grid surface point based on the vector
	  */
	def apply(vector: Vector2D)(implicit grid: GridArea): GridSurfacePoint = new VectorOnGrid(vector)
	/**
	  * Converts a latitude-longitude coordinate into a grid surface point
	  * @param latLong The coordinate to convert
	  * @param grid   Grid to assume implicitly
	  * @return A new grid surface point based on the specified coordinate
	  */
	def apply(latLong: LatLong)(implicit grid: GridArea): GridSurfacePoint = new LatLongOnGrid(latLong)
	
	
	// NESTED   ---------------------
	
	private class VectorOnGrid(override val vector: Vector2D)(implicit override val grid: GridArea)
		extends GridSurfacePoint
	{
		override lazy val latLong: LatLong = grid.vectorToLatLong(vector)
		
		override def withAltitude(altitude: Distance): GridPoint =
			GridPoint(Vector3D(vector.x, vector.y, grid.vectorLengthOf(altitude)))
	}
	
	private class LatLongOnGrid(override val latLong: LatLong)(implicit override val grid: GridArea)
		extends GridSurfacePoint
	{
		override lazy val vector: Vector2D = grid.latLongToVector(latLong)
		
		override def withAltitude(altitude: Distance): GridPoint = GridPoint(latLong, altitude)
	}
}

/**
  * Represents a point on the earth's surface when
  * using a grid-based world model.
  * @author Mikko Hilpinen
  * @since 2.11.2023, v1.0.1
  */
trait GridSurfacePoint
	extends SurfacePoint[Vector2D, GridPoint] with GridPointOps[Vector2D, GridSurfacePoint]
{
	override protected def at(location: HasDoubleDimensions): GridSurfacePoint =
		GridSurfacePoint(Vector2D.from(location))
	override protected def at(latLong: LatLong): GridSurfacePoint = GridSurfacePoint(latLong)
}