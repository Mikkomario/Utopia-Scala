package utopia.firmament.model.stack.modifier

import utopia.firmament.model.stack.StackSize
import utopia.paradigm.shape.shape2d.vector.size.Size

/**
  * Makes sure the optimal stack length is at least a specific fixed amount
  * @author Mikko Hilpinen
  * @since 01.02.2025, v1.4.1
  */
case class MinOptimalSizeModifier(minSize: Size) extends StackSizeModifier
{
	// ATTRIBUTES   ---------------------
	
	private val modifiers = minSize.components.view.map { d => d.axis -> MinOptimalLengthModifier(d.value) }.toMap
	
	
	// IMPLEMENTED  ---------------------
	
	override def apply(size: StackSize): StackSize = size.map { (axis, length) => modifiers(axis)(length) }
}
