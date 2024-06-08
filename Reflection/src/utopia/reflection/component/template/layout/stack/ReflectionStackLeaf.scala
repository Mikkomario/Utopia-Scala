package utopia.reflection.component.template.layout.stack

import utopia.flow.collection.immutable.Empty
import utopia.reflection.event.StackHierarchyListener

/**
  * This trait is implemented by components that don't wrap or contain any other stackable instances,
  * by components that represent "leaves" in the stack hierarchy
  * @author Mikko Hilpinen
  * @since 13.3.2020, v1
  */
trait ReflectionStackLeaf extends ReflectionStackable
{
	// ATTRIBUTES	-----------------------
	
	private var _isAttachedToMainHierarchy = false
	
	override var stackHierarchyListeners: Seq[StackHierarchyListener] = Empty
	
	
	// IMPLEMENTED	-----------------------
	
	override def isAttachedToMainHierarchy = _isAttachedToMainHierarchy
	
	override def isAttachedToMainHierarchy_=(newAttachmentStatus: Boolean) = {
		if (newAttachmentStatus != _isAttachedToMainHierarchy) {
			_isAttachedToMainHierarchy = newAttachmentStatus
			fireStackHierarchyChangeEvent(newAttachmentStatus)
		}
	}
}
