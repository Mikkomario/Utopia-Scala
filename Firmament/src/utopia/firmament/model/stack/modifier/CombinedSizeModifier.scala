package utopia.reflection.shape.stack.modifier

import utopia.reflection.shape.stack.StackSize

/**
  * This stack size modifier applies two modifiers consecutively
  * @author Mikko Hilpinen
  * @since 30.8.2020, Reflection v1.2.1
  */
class CombinedSizeModifier(first: StackSizeModifier, second: StackSizeModifier) extends StackSizeModifier
{
	override def apply(size: StackSize) = second(first(size))
}
