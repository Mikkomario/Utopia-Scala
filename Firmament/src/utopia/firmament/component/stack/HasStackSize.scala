package utopia.firmament.component.stack

import utopia.firmament.model.stack.StackSize

/**
  * Common trait for components that can specify a stack size
  * @author Mikko Hilpinen
  * @since 3.5.2023, v1.1
  */
trait HasStackSize
{
	/**
	  * @return The current size requirements of this component
	  */
	def stackSize: StackSize
}
