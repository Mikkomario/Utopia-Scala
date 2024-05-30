package utopia.paradigm.path

import utopia.flow.operator.combine.{Combinable, LinearScalable}

/**
  * Linear paths calculate a direct path between two items
  * @author Mikko Hilpinen
  * @since Genesis 20.6.2019, v2.1+
  */
// TODO: Skip this "like" trait and just replace with LinearPath (which should be converted to a trait instead)
trait LinearPathLike[P <: LinearScalable[P] with Combinable[P, P]] extends Path[P]
{
	override def apply(t: Double) = end * t + start * (1 - t)
}
