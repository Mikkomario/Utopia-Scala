package utopia.firmament.model.stack.modifier

import utopia.firmament.model.stack.StackSize

/**
  * This stack size modifier applies two modifiers consecutively
  * @author Mikko Hilpinen
  * @since 30.8.2020, Reflection v1.2.1
  */
class CombinedSizeModifier(first: StackSizeModifier, second: StackSizeModifier) extends StackSizeModifier
{
	override def apply(size: StackSize) = second(first(size))
}
