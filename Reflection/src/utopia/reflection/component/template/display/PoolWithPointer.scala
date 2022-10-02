package utopia.reflection.component.template.display

import utopia.flow.event.listener.ChangeListener
import utopia.flow.view.template.eventful.Changing

object PoolWithPointer
{
	/**
	  * A pool with a generic pointer
	  */
	type ChangingPool[A] = PoolWithPointer[A, Changing[A]]
}

/**
  * This pool provides access to a changing element
  * @author Mikko Hilpinen
  * @since 29.6.2019, v1+
  */
trait PoolWithPointer[+A, +P <: Changing[A]] extends Pool[A]
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
	  */
	def addContentListener(listener: ChangeListener[A]) = contentPointer.addListener(listener)
	
	/**
	  * Adds a new listener to be informed about content changes
	  * @param simulatedOldValue A simulated old value which is used for generating an initial event for this listener
	  * @param listener                        The new listener to be added
	  */
	def addContentListenerAndSimulateEvent[B >: A](simulatedOldValue: B)(listener: => ChangeListener[B]) =
		contentPointer.addListenerAndSimulateEvent(simulatedOldValue)(listener)
	
	/**
	  * Removes a listener from informed listeners
	  * @param listener A listener
	  */
	def removeContentListener(listener: Any) = contentPointer.removeListener(listener)
}
