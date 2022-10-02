package utopia.paradigm.path

import utopia.flow.operator.{Combinable, HasLength, LinearScalable}

/**
  * This path provides a direct path between two values
  * @author Mikko Hilpinen
  * @since Genesis 20.6.2019, v2.1+
  */
case class LinearPath[P <: Combinable[P, P] with LinearScalable[P] with HasLength](start: P, end: P)
	extends LinearPathLike[P] with HasLength
{
	override def length = (end - start).length
}
