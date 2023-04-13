package utopia.firmament.model.stack.modifier

import utopia.firmament.model.stack.StackLength

/**
  * Places a maximum limit on the optimal stack length
  * @author Mikko Hilpinen
  * @since 9.12.2020, Reflection v2
  */
case class MaxOptimalLengthModifier(max: Double) extends StackLengthModifier
{
	override def apply(length: StackLength) = if (length.optimal <= max) length else length.withOptimal(max)
}
