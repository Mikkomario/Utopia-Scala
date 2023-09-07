package utopia.terra.controller.coordinate.map

import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.template.MatrixLike
import utopia.paradigm.shape.template.vector.DoubleVectorLike
import utopia.terra.controller.coordinate.world.{LatLongToSurfacePoint, LatLongToWorldPoint}
import utopia.terra.model.angular.LatLong
import utopia.terra.model.map.MapPoint
import utopia.terra.model.world.WorldPoint

/**
 * Common trait for coordinate transformation systems that may be used for projecting
 * vector locations on a 2D map
 * @author Mikko Hilpinen
 * @since 31.8.2023, v1.0
  * @tparam V Type of vector representations used by this map
  * @tparam M Type of matrices used by this map
 */
trait PointMapLike[V <: DoubleVectorLike[V], +M <: MatrixLike[V, M]]
{
	// ABSTRACT ----------------------------
	
	/**
	  * @return Map point used as the origin (0,0) for the transformation matrices
	  */
	protected def origin: MapPoint[V]
	
	/**
	  * @return Transformation that takes world point vector coordinates and produces a product of the reference vectors
	  */
	protected def vectorTransform: M
	/**
	  * @return Transformation that takes world point vector coordinates and produces image coordinate vectors,
	  *         relative to [[origin]]
	  */
	protected def fullTransform: M
	
	/**
	  * @param vectorPosition A real world position in its vector representation
	  * @return A point on this map's image that matches the specified location.
	  *         Relative to the image origin coordinates.
	  */
	def pointOnMap(vectorPosition: V) = {
		// Relates the specified position to origin vector
		val vectorRelativeToOrigin = vectorPosition - origin.vector
		// Transforms this point to image world space (relative to image origin vector)
		val mapPointRelativeToOrigin = fullTransform(vectorRelativeToOrigin)
		
		// Applies the correct image origin - May need to mirror the vector in case of some transformations
		if (vectorTransform.determinant >= 0)
			origin.mapLocation + mapPointRelativeToOrigin
		else
			origin.mapLocation - mapPointRelativeToOrigin
	}
	/**
	  * @param point A world position
	  * @return A point on this map's image that matches the specified location.
	  *         Relative to the image origin coordinates.
	  */
	def pointOnMap(point: WorldPoint[V]): Point = pointOnMap(point.vector)
	
	/**
	  * @param latLong A latitude-longitude coordinate
	  * @param worldView Implied world view
	  * @return That location on this map (in the image coordinate space), relative to the map's origin
	  */
	def latLongOnMap(latLong: LatLong)(implicit worldView: LatLongToSurfacePoint[WorldPoint[V]]) =
		pointOnMap(worldView(latLong))
	/**
	  * @param latLong A latitude-longitude coordinate
	  * @param altitude Target altitude
	  * @param worldView Implied world view
	  * @return That location on this map (in the image coordinate space), relative to the map's origin
	  */
	def latLongOnMap(latLong: LatLong, altitude: Distance)(implicit worldView: LatLongToWorldPoint[_, WorldPoint[V]]) =
		pointOnMap(worldView(latLong, altitude))
}
