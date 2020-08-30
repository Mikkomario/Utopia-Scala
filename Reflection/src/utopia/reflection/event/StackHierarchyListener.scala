package utopia.reflection.event

import scala.language.implicitConversions

object StackHierarchyListener
{
	// IMPLICIT	------------------------------
	
	implicit def functionToListener[U](f: Boolean => U): StackHierarchyListener = apply(f)
	
	
	// OTHER	------------------------------
	
	/**
	  * Creates a new stack hierarchy listener
	  * @param f A function called when a component is attached to or detached from the main stack hierarchy
	  *          (accepts new attachment status)
	  * @tparam U Arbitrary result type
	  * @return A new stack hierarchy listener that calls the specified function when targeted component's connection
	  *         status changes
	  */
	def apply[U](f: Boolean => U): StackHierarchyListener = new FunctionalListener(f)
	
	/**
	  * Creates a new stack hierarchy listener
	  * @param f A function called when a component is attached to the main stack hierarchy
	  * @tparam U Arbitrary result type
	  * @return A new stack hierarchy listener that calls the specified function whenever the targeted component is
	  *         attached to the main stack hierarchy
	  */
	def attachmentListener[U](f: => U) = apply { if (_) f }
	
	/**
	  * Creates a new stack hierarchy listener
	  * @param f A function called when a component is detached from the main stack hierarchy
	  * @tparam U Arbitrary result type
	  * @return A new stack hierarchy listener that calls the specified function whenever the targeted component is
	  *         detached from the main stack hierarchy
	  */
	def detachmentListener[U](f: => U) = apply { newStatus => if (newStatus) f }
	
	
	// NESTED	------------------------------
	
	private class FunctionalListener[+U](f: Boolean => U) extends StackHierarchyListener
	{
		override def onComponentAttachmentChanged(newAttachmentStatus: Boolean) = f(newAttachmentStatus)
	}
}

/**
  * Classes extending this trait are interested in receiving events about changes in stack component hierarchy
  * @author Mikko Hilpinen
  * @since 26.8.2020, v1.2
  */
trait StackHierarchyListener
{
	/**
	  * This method is called when a listened component's stack hierarchy connection is either established or broken
	  * @param newAttachmentStatus Whether the listened component is now part of the main stack hierarchy
	  */
	def onComponentAttachmentChanged(newAttachmentStatus: Boolean): Unit
}
