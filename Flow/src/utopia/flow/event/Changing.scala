package utopia.flow.event

import scala.concurrent.{ExecutionContext, Promise}

/**
  * Changing instances generate change events
  * @author Mikko Hilpinen
  * @since 26.5.2019, v1.4.1+
  */
trait Changing[A]
{
	// ATTRIBUTES	-----------------
	
	/**
	  * The listeners interested in this mutable item's changes
	  */
	var listeners = Vector[ChangeListener[A]]()
	
	
	// ABSTRACT	---------------------
	
	/**
	  * @return The current value of this changing element
	  */
	def value: A
	
	
	// OTHER	--------------------
	
	/**
	  * Adds a new listener to this mutable
	  * @param changeListener A change listener that will be informed when the value of this mutable changes
	  * @param generateChangeEventFromOldValue None if no change event should be generated for the new listener.
	  *                                        Some with "old" value if a change event should be triggered
	  *                                        <b>for this new listener</b>. Default = None
	  */
	def addListener(changeListener: ChangeListener[A], generateChangeEventFromOldValue: Option[A] = None) =
	{
		listeners :+= changeListener
		generateChangeEventFromOldValue.foreach { old =>
			val newValue = value
			if (old != newValue)
				changeListener.onChangeEvent(ChangeEvent(old, newValue))
		}
	}
	
	/**
	  * Removes a listener from the informed change listeners
	  * @param listener A listener that will be removed
	  */
	def removeListener(listener: Any) = listeners = listeners.filterNot { _ == listener }
	
	/**
	 * @param valueCondition A condition for finding a suitable future
	 * @param exc Implicit execution context
	 * @return A future where this changing instance's value triggers the specified condition the first time
	 *         (immediately completed if current value already triggers the condition). Please note that the future
	 *         might never complete.
	 */
	def futureWhere(valueCondition: A => Boolean)(implicit exc: ExecutionContext) =
	{
		val listener = new FutureValueListener(value, valueCondition)
		addListener(listener)
		// Will not need to listen anymore once the future has been completed
		listener.future.foreach { _ => removeListener(listener) }
		listener.future
	}
	
	/**
	  * Fires a change event for all the listeners
	  * @param oldValue The old value of this changing element (call-by-name)
	  */
	protected def fireChangeEvent(oldValue: => A) =
	{
		lazy val event = ChangeEvent(oldValue, value)
		listeners.foreach { _.onChangeEvent(event) }
	}
}

private class FutureValueListener[A](initialValue: A, trigger: A => Boolean) extends ChangeListener[A]
{
	// ATTRIBUTES	-------------------
	
	private val promise = Promise[A]()
	
	
	// INITIAL CODE	-------------------
	
	if (trigger(initialValue))
		promise.success(initialValue)
	
	
	// COMPUTED	-----------------------
	
	/**
	 * @return Future for the first value that triggers the specified condition
	 */
	def future = promise.future
	
	
	// IMPLEMENTED	-------------------
	
	override def onChangeEvent(event: ChangeEvent[A]) =
	{
		if (!promise.isCompleted && trigger(event.newValue))
			promise.trySuccess(event.newValue)
	}
}