package utopia.genesis.shape.path

import utopia.genesis.util.{Combinable, Scalable}

/**
  * Linear paths calculate a direct path between two items
  * @author Mikko Hilpinen
  * @since 20.6.2019, v2.1+
  */
trait LinearPathLike[P <: Scalable[P] with Combinable[P, P]] extends Path[P]
{
	override def apply(t: Double) = end * t + start * (1 - t)
}
