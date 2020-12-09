package utopia.reflection.shape.stack.modifier

import utopia.reflection.shape.stack.StackLength

/**
  * A stack length modifier which applies a minimum length limit
  * @author Mikko Hilpinen
  * @since 15.11.2020, v2
  */
case class MaxBetweenLengthModifier(min: StackLength) extends StackLengthModifier
{
	override def apply(length: StackLength) = length max min
}
