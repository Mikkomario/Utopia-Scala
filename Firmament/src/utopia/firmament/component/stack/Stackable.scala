package utopia.firmament.component.stack

import utopia.firmament.component.Component
import utopia.reflection.shape.stack.StackSize

/**
* This trait is inherited by component classes that can be placed in stacks (component hierarchies with auto-layout)
* @author Mikko Hilpinen
* @since 25.2.2019, Reflection v1
**/
trait Stackable extends Component
{
	// ABSTRACT	---------------------
	
	/**
	  * Updates the layout (and other contents) of this stackable instance. This method will be called if the component,
	  * or its child is revalidated. The stack sizes of this component, as well as those of revalidating children
	  * should be reset at this point.
	  */
	def updateLayout(): Unit
	
	/**
	  * The current size requirements of this component
	  */
	def stackSize: StackSize
	
	/**
	  * Resets cached stackSize, if there is one, so that it will be recalculated when requested next time
	  */
	def resetCachedSize(): Unit
	
	
	// COMPUTED	---------------------
	
	/**
	  * @return Optimal width for this component
	  */
	def optimalWidth = stackSize.width.optimal
	
	/**
	  * @return Optimal height for this component
	  */
	def optimalHeight = stackSize.height.optimal
	
	/**
	  * @return Whether this component is now larger than its maximum size
	  */
	def isOverSized = stackSize.maxWidth.exists { _ < width } || stackSize.maxHeight.exists { _ < height }
	
	/**
	  * @return Whether this component is now smaller than its minimum size
	  */
	def isUnderSized = width < stackSize.minWidth || height < stackSize.minHeight
	
	
	// OTHER	---------------------
	
	/**
	 * Sets the size of this component to optimal (by stack size)
	 */
	def setToOptimalSize() = size = stackSize.optimal
	
	/**
	 * Sets the size of this component to minimum (by stack size)
	 */
	def setToMinSize() = size = stackSize.min
}
