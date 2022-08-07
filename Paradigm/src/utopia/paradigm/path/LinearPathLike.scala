package utopia.paradigm.path

import utopia.flow.operator.{Combinable, LinearScalable}

/**
  * Linear paths calculate a direct path between two items
  * @author Mikko Hilpinen
  * @since Genesis 20.6.2019, v2.1+
  */
trait LinearPathLike[P <: LinearScalable[P] with Combinable[P, P]] extends Path[P]
{
	override def apply(t: Double) = end * t + start * (1 - t)
}
