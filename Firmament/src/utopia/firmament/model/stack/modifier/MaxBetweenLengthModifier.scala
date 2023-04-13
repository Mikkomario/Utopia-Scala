package utopia.firmament.model.stack.modifier

import utopia.firmament.model.stack.StackLength

/**
  * A stack length modifier which applies a minimum length limit
  * @author Mikko Hilpinen
  * @since 15.11.2020, Reflection v2
  */
case class MaxBetweenLengthModifier(min: StackLength) extends StackLengthModifier
{
	override def apply(length: StackLength) = length max min
}
