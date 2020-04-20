package utopia.reflection.container.stack

import utopia.reflection.component.stack.Stackable
import utopia.reflection.container.ContainerLike

/**
  * This is a common trait for containers with stackable
  * @author Mikko Hilpinen
  * @since 21.4.2019, v1+
  */
// FIXME: This cannot be a "like" trait because it uses "this" and defines attributes (cannot be used in wrappers)
trait StackContainerLike[C <: Stackable] extends ContainerLike[C] with Stackable
{
	// ATTRIBUTES	-----------------------
	
	private var _isAttachedToMainHierarchy = false
	
	
	// IMPLEMENTED	-----------------------
	
	override def children: Seq[Stackable] = components
	
	override def isAttachedToMainHierarchy = _isAttachedToMainHierarchy
	
	override def isAttachedToMainHierarchy_=(newAttachmentStatus: Boolean) =
	{
		if (_isAttachedToMainHierarchy != newAttachmentStatus)
		{
			_isAttachedToMainHierarchy = newAttachmentStatus
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
