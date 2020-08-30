package utopia.reflection.component.template.layout.stack

import utopia.reflection.component.template.ComponentWrapper
import utopia.reflection.event.StackHierarchyListener

/**
  * This wrapper wraps a stackable, providing full stackable interface itself
  * @author Mikko Hilpinen
  * @since 28.4.2019, v1+
  */
trait StackableWrapper extends ComponentWrapper with Stackable
{
	// ABSTRACT	---------------------
	
	override protected def wrapped: Stackable
	
	
	// IMPLEMENTED	-----------------
	
	override def children = wrapped.children
	
	override def updateLayout() = wrapped.updateLayout()
	
	override def stackSize = wrapped.stackSize
	
	override def resetCachedSize() = wrapped.resetCachedSize()
	
	override def stackId = wrapped.stackId
	
	override def isAttachedToMainHierarchy = wrapped.isAttachedToMainHierarchy
	
	override def isAttachedToMainHierarchy_=(newAttachmentStatus: Boolean) =
		wrapped.isAttachedToMainHierarchy_=(newAttachmentStatus)
	
	override def stackHierarchyListeners = wrapped.stackHierarchyListeners
	
	override def stackHierarchyListeners_=(newListeners: Vector[StackHierarchyListener]) =
		wrapped.stackHierarchyListeners = newListeners
}
