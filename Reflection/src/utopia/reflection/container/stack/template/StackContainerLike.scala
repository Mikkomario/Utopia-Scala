package utopia.reflection.container.stack.template

import utopia.reflection.component.template.layout.stack.Stackable
import utopia.reflection.container.template.ContainerLike
import utopia.reflection.event.StackHierarchyListener

/**
  * This is a common trait for containers with stackable
  * @author Mikko Hilpinen
  * @since 21.4.2019, v1+
  */
trait StackContainerLike[C <: Stackable] extends ContainerLike[C] with Stackable
{
	// ATTRIBUTES	-----------------------
	
	private var _isAttachedToMainHierarchy = false
	
	override var stackHierarchyListeners = Vector[StackHierarchyListener]()
	
	
	// IMPLEMENTED	-----------------------
	
	override def children: Seq[Stackable] = components
	
	override def isAttachedToMainHierarchy = _isAttachedToMainHierarchy
	
	override def isAttachedToMainHierarchy_=(newAttachmentStatus: Boolean) =
	{
		if (_isAttachedToMainHierarchy != newAttachmentStatus)
		{
			_isAttachedToMainHierarchy = newAttachmentStatus
			fireStackHierarchyChangeEvent(newAttachmentStatus)
			// When connected to stack hierarchy, connects the children as well
			children.foreach { child =>
				if (newAttachmentStatus)
					child.attachToStackHierarchyUnder(this)
				else
					child.isAttachedToMainHierarchy = newAttachmentStatus
			}
		}
	}
}
