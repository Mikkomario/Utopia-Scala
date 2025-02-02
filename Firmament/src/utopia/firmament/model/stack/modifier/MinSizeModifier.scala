package utopia.firmament.model.stack.modifier

import utopia.firmament.model.stack.StackSize
import utopia.paradigm.shape.shape2d.vector.size.Size

/**
  * Applies a minimum stack size to an existing stack size
  * @author Mikko Hilpinen
  * @since 01.02.2025, v1.4.1
  */
case class MinSizeModifier(minSize: Size) extends StackSizeModifier
{
	override def apply(size: StackSize): StackSize = size.mergeWith(minSize) { (len, min) => len.mapMin { _ max min } }
}
