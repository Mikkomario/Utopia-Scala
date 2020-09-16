package utopia.genesis.shape.path

import utopia.genesis.util.{Arithmetic, DistanceLike}

/**
  * This path provides a direct path between two values
  * @author Mikko Hilpinen
  * @since 20.6.2019, v2.1+
  */
case class LinearPath[P <: Arithmetic[P, P] with DistanceLike](start: P, end: P) extends LinearPathLike[P]
	with DistanceLike
{
	override def length = (end - start).length
}
