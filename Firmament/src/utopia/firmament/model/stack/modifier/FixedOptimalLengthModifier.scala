package utopia.firmament.model.stack.modifier

import utopia.firmament.model.stack.StackLength

/**
  * Fixes the optimal length of the targeted item to a specific value
  * @author Mikko Hilpinen
  * @since 10.4.2023, v2.0
  */
case class FixedOptimalLengthModifier(value: Double) extends StackLengthModifier
{
	def apply(length: StackLength): StackLength = length.withOptimal(value)
}
