package utopia.reflection.shape.stack.modifier

import utopia.reflection.shape.stack.StackLength

/**
  * This stack length modifier applies two stack length modifiers consecutively. This is useful when
  * specific ordering is required between the modifiers.
  * @author Mikko Hilpinen
  * @since 30.8.2020, Reflection v1.2.1
  */
class CombinedLengthModifier(first: StackLengthModifier, second: StackLengthModifier) extends StackLengthModifier
{
	override def apply(length: StackLength) = second(first(length))
}
