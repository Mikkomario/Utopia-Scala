package utopia.paradigm.path

import utopia.flow.operator.LinearMeasurable

/**
  * This path doesn't move anywhere from a single point
  * @author Mikko Hilpinen
  * @since Genesis 20.6.2019, v2.1+
  */
case class EmptyPath[P](point: P) extends Path[P] with LinearMeasurable
{
	override def start = point
	
	override def end = point
	
	override def length = 0.0
	
	override def apply(t: Double) = point
}
