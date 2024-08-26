package utopia.terra.controller.coordinate.world

/**
  * Common trait for coordinate conversions
  * that are able to convert between latitude-longitude coordinates and vectors (including distance conversions).
  * @tparam V Type of vectors constructed from latitude-longitude coordinates (the primary (surface) vector form)
  * @tparam VL Type of vectors which may be converted to latitude-longitude coordinates (the more general vector form)
  * @author Mikko Hilpinen
  * @since 26.08.2024, v1.2
  */
trait SurfaceVectorConversions[+V, -VL]
	extends VectorFromLatLongFactory[V] with LatLongFromVectorFactory[VL] with VectorDistanceConversion
