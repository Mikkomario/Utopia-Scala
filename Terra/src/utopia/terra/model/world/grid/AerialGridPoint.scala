package utopia.terra.model.world.grid

import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.vector.DoubleVector
import utopia.terra.controller.coordinate.world.GridArea
import utopia.terra.model.angular.LatLong
import utopia.terra.model.world.{AerialPoint, WorldDistance}

object AerialGridPoint
{
	// OTHER    ---------------------
	
	def apply(vector: Vector3D)(implicit grid: GridArea): AerialGridPoint = new VectorOverGrid(vector)
	def apply(latLong: LatLong, altitude: WorldDistance)(implicit grid: GridArea): AerialGridPoint =
		new LatLongOverGrid(latLong, altitude)
	
	
	// NESTED   ---------------------
	
	class VectorOverGrid(override val vector: Vector3D)(implicit override val grid: GridArea) extends AerialGridPoint
	{
		// ATTRIBUTES   -------------
		
		private lazy val surface = GridSurfacePoint(vector.in2D)
		
		
		// IMPLEMENTED  -------------
		
		override def latLong: LatLong = surface.latLong
		override def altitude: WorldDistance = grid.distanceOf(vector.z)
		
		override def toSurfacePoint: GridSurfacePoint = surface
		
		override def withAltitude(altitude: Distance): AerialGridPoint = withAltitude(grid.vectorLengthOf(altitude))
		override def withAltitude(altitude: Double): AerialGridPoint = new VectorOverGrid(vector.withZ(altitude))
	}
	class LatLongOverGrid(override val latLong: LatLong, override val altitude: WorldDistance)
	                     (implicit override val grid: GridArea)
		extends AerialGridPoint
	{
		// ATTRIBUTES   -----------------
		
		override lazy val vector: Vector3D = {
			val surfaceVector = grid.latLongToVector(latLong)
			Vector3D(surfaceVector.x, surfaceVector.y, grid.vectorLengthOf(altitude))
		}
		
		
		// IMPLEMENTED  -----------------
		
		override def toSurfacePoint: GridSurfacePoint = GridSurfacePoint(latLong)
		
		override def withAltitude(altitude: Distance): AerialGridPoint = new LatLongOverGrid(latLong, altitude)
		override def withAltitude(altitude: Double): AerialGridPoint = new LatLongOverGrid(latLong, altitude)
	}
}

/**
  * Represents a world location in a grid-based model
  * using three dimensions.
  * @author Mikko Hilpinen
  * @since 2.11.2023, v1.0.1
  */
trait AerialGridPoint
	extends AerialPoint[Vector3D, AerialGridPoint, DoubleVector, GridSurfacePoint, AerialGridTravel]
		with GridPointOps[Vector3D, AerialGridPoint, GridSurfacePoint, AerialGridTravel]
{
	override protected def at(location: HasDoubleDimensions): AerialGridPoint = AerialGridPoint(Vector3D.from(location))
	override protected def at(latLong: LatLong): AerialGridPoint = AerialGridPoint(latLong, altitude.distance)
	
	override def to(target: AerialGridPoint): AerialGridTravel = AerialGridTravel(this, target)
	override def -(origin: AerialGridPoint): AerialGridTravel = AerialGridTravel(origin, this)
}
