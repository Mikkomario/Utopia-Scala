package utopia.firmament.model.stack.modifier

import utopia.firmament.model.stack.StackLength

/**
  * Specifies a minimum optimal stack length
  * @author Mikko Hilpinen
  * @since 01.02.2025, v1.4.1
  */
case class MinOptimalLengthModifier(minLength: Double) extends StackLengthModifier
{
	override def apply(length: StackLength): StackLength =
		if (length.optimal >= minLength) length else length.withOptimal(minLength)
}