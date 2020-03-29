package utopia.reflection.component

import utopia.flow.event.{ChangeListener, Changing}

/**
  * This pool provides access to a changing element
  * @author Mikko Hilpinen
  * @since 29.6.2019, v1+
  */
trait PoolWithPointer[A, +P <: Changing[A]] extends Pool[A]
{
	// ABSTRACT	----------------
	
	/**
	  * @return A pointer into this pool's contents
	  */
	def contentPointer: P
	
	
	// IMPLEMENTED	------------
	
	override def content = contentPointer.value
	
	
	// OTHER	----------------
	
	/**
	  * Adds a new listener to be informed about content changes
	  * @param listener                        The new listener to be added
	  * @param generateChangeEventFromOldValue None if no change event should be generated for the new listener.
	  *                                        Some with "old" value if a change event should be triggered
	  *                                        <b>for this new listener</b>. Default = None
	  */
	def addContentListener(listener: ChangeListener[A], generateChangeEventFromOldValue: Option[A] = None) =
		contentPointer.addListener(listener, generateChangeEventFromOldValue)
	
	/**
	  * Removes a listener from informed listeners
	  * @param listener A listener
	  */
	def removeContentListener(listener: Any) = contentPointer.removeListener(listener)
}
