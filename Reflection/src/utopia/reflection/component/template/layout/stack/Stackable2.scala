package utopia.reflection.component.template.layout.stack

import utopia.reflection.component.template.ComponentLike2
import utopia.reflection.shape.stack.StackSize

/**
* This trait is inherited by component classes that can be placed in stacks (component hierarchies with auto-layout)
* @author Mikko Hilpinen
* @since 25.2.2019, v1
**/
trait Stackable2 extends ComponentLike2
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
	
	/**
	  * @return A unique identifier for this stackable instance. These id's are used in stack hierarchy to
	  *         distinquish between items. If this stackable simply wraps another item, it should use the same id,
	  *         otherwise the id should be unique (usually it is enough to return hashCode).
	  */
	def stackId: Int
	
	/**
	  * Child components under this stackable instance (all of which should be stackable)
	  */
	override def children: Seq[Stackable2] = Vector()
	
	
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
