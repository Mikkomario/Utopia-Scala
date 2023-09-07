package utopia.terra.model.map

import utopia.flow.operator.{Combinable, LinearScalable}
import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.terra.controller.coordinate.world.{LatLongToSurfacePoint, LatLongToWorldPoint}
import utopia.terra.model.angular.LatLong
import utopia.terra.model.world.WorldPoint

object MapPoint
{
	// OTHER    --------------------------
	
	/**
	  * Creates a new map point
	  * @param vector A vector coordinate matching the specified map location
	  * @param mapLocation A map location relative to map image origin
	  * @tparam V Type of the vector representation used
	  * @return A new map point
	  */
	def apply[V <: Combinable[V, V] with LinearScalable[V]](vector: V, mapLocation: Point): MapPoint[V] =
		new _MapPoint[V](vector, mapLocation)
	/**
	  * Creates a new map point
	  * @param worldPoint  A location matching the specified map location
	  * @param mapLocation A map location relative to map image origin
	  * @tparam V Type of the vector representation used
	  * @return A new map point
	  */
	def apply[V <: Combinable[V, V] with LinearScalable[V]](worldPoint: WorldPoint[V], mapLocation: Point): MapPoint[V] =
		apply(worldPoint.vector, mapLocation)
	/**
	  * Creates a new map point
	  * @param latLong     Latitude-longitude coordinate that matches the specified map location
	  * @param mapLocation A map location relative to map image origin
	  * @param worldView Worldview to assume (implicit)
	  * @tparam V Type of the vector representation used
	  * @return A new map point
	  */
	def apply[V <: Combinable[V, V] with LinearScalable[V]](latLong: LatLong, mapLocation: Point)
	                                                       (implicit worldView: LatLongToSurfacePoint[WorldPoint[V]]): MapPoint[V] =
		apply(worldView(latLong), mapLocation)
	/**
	  * Creates a new map point
	  * @param latLong     Latitude-longitude coordinate that matches the specified map location
	  * @param altitude    Target altitude, relative to the sea level
	  * @param mapLocation A map location relative to map image origin
	  * @param worldView   Worldview to assume (implicit)
	  * @tparam V Type of the vector representation used
	  * @return A new map point representing the specified (aerial) location
	  */
	def apply[V <: Combinable[V, V] with LinearScalable[V]](latLong: LatLong, altitude: Distance, mapLocation: Point)
	                                                       (implicit worldView: LatLongToWorldPoint[_, WorldPoint[V]]): MapPoint[V] =
		apply(worldView(latLong, altitude), mapLocation)
	
	
	// NESTED   --------------------------
	
	private class _MapPoint[V <: Combinable[V, V] with LinearScalable[V]](override val vector: V,
	                                                                      override val mapLocation: Point)
		extends MapPoint[V]
	{
		override def self: MapPoint[V] = this
		
		override def +(other: MapPoint[V]): MapPoint[V] =
			new _MapPoint[V](vector + other.vector, mapLocation + other.mapLocation)
		
		override def *(mod: Double): MapPoint[V] = new _MapPoint[V](vector * mod, mapLocation * mod)
	}
}

/**
 * Common trait for points that bind a world position into a 2D map location.
 * Implementing classes are typically expected to extend [[MapPointLike]] as well
 * @author Mikko Hilpinen
 * @since 31.8.2023, v1.0
 */
trait MapPoint[V] extends MapPointLike[V, MapPoint[V]]
