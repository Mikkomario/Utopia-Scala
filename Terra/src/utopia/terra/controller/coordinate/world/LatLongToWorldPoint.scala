package utopia.terra.controller.coordinate.world

/**
  * Common trait for factories used for converting latitude-longitude coordinates plus altitude information
  * into world coordinates
  * @author Mikko Hilpinen
  * @since 6.9.2023, v1.0
  * @tparam S Type of Earth's surface points produced by this factory
  * @tparam A Type of (aerial) world points produced by this factory
  */
trait LatLongToWorldPoint[+S, +A] extends LatLongToSurfacePoint[S] with LatLongToAerialPoint[A]