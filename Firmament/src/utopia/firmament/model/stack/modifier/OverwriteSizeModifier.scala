package utopia.firmament.model.stack.modifier

import utopia.firmament.model.stack.StackSize

/**
  * A stack size modifier that overwrites the size with a new value
  * @author Mikko Hilpinen
  * @since 23/02/2024, v1.3
  */
case class OverwriteSizeModifier(size: StackSize) extends StackSizeModifier
{
	override def apply(size: StackSize): StackSize = this.size
}
