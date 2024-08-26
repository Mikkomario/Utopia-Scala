package utopia.terra.controller.coordinate.world

/**
 * Common trait for different world representation models used for coordinate transformations
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 * @tparam V2D Type of the vector representation of surface-level points (output type)
 * @tparam V2DI Type of vector representations that can be converted to latitude-longitude coordinates
  *              and to surface points (i.e. the input vector type)
  * @tparam V3D Type of the vector representation of aerial points
 * @tparam Surface Surface level (2D) point type
 * @tparam Aerial Aerial (3D) point type
 */
trait WorldView[+V2D, -V2DI, -V3D, +Surface, +Aerial]
	extends WorldPointFactory[V2DI, V3D, Surface, Aerial] with SurfaceVectorConversions[V2D, V2DI]
