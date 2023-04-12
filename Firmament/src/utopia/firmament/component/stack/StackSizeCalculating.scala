package utopia.firmament.component.stack

import utopia.reflection.shape.stack.StackSize

/**
  * These items are able to calculate their stack size
  * @author Mikko Hilpinen
  * @since 28.4.2019, Reflection v1+
  */
trait StackSizeCalculating
{
	/**
	  * Calculates an up-to-date stack size for this component
	  * @return An up-to-date stack size for this component
	  */
	def calculatedStackSize: StackSize
}
