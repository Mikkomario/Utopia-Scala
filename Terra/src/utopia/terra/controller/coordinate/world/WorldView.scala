package utopia.terra.controller.coordinate.world

/**
 * Common trait for different world representation models used for coordinate transformations
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 * @tparam V2D Type of the vector representation of surface-level points
 * @tparam V3D Type of the vector representation of aerial points
 * @tparam Surface Surface level (2D) point type
 * @tparam Aerial Aerial (3D) point type
 */
trait WorldView[V2D, -V3D, +Surface, +Aerial]
	extends LatLongToWorldPoint[Surface, Aerial] with VectorToWorldPoint[V2D, V3D, Surface, Aerial]
		with VectorDistanceConversion with LatLongFromVectorFactory[V2D]
		with VectorFromLatLongFactory[V2D]
