package utopia.reflection.component.template.layout.stack

import utopia.reflection.component.template.ReflectionComponentWrapper
import utopia.reflection.event.StackHierarchyListener

/**
  * This wrapper wraps a stackable, providing full stackable interface itself
  * @author Mikko Hilpinen
  * @since 28.4.2019, v1+
  */
trait ReflectionStackableWrapper extends StackableWrapper2 with ReflectionStackable with ReflectionComponentWrapper
{
	// ABSTRACT	---------------------
	
	override protected def wrapped: ReflectionStackable
	
	
	// IMPLEMENTED	-----------------
	
	override def children = wrapped.children
	
	override def stackId = wrapped.stackId
	
	override def isAttachedToMainHierarchy = wrapped.isAttachedToMainHierarchy
	
	override def isAttachedToMainHierarchy_=(newAttachmentStatus: Boolean) =
		wrapped.isAttachedToMainHierarchy_=(newAttachmentStatus)
	
	override def stackHierarchyListeners = wrapped.stackHierarchyListeners
	
	override def stackHierarchyListeners_=(newListeners: Vector[StackHierarchyListener]) =
		wrapped.stackHierarchyListeners = newListeners
}
