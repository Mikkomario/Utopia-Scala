package utopia.terra.controller.coordinate.world

/**
  * Common trait for factories that construct world points (aerial & surface) from vectors and
  * latitude-longitude coordinates
  * @author Mikko Hilpinen
  * @since 12.11.2023, v1.1
  */
trait WorldPointFactory[-V2D, -V3D, +S, +A] extends VectorToWorldPoint[V2D, V3D, S, A] with LatLongToWorldPoint[S, A]
