package utopia.flow.event

import scala.concurrent.{ExecutionContext, Promise}

object Changing
{
	// OTHER	---------------------
	
	/**
	  * Wraps an immutable value as a "changing" instance that won't actually change
	  * @param value A value being wrapped
	  * @tparam A Type of wrapped value
	  * @return A "changing" instance that will always have the specified value
	  */
	def wrap[A](value: A): Changing[A] = new Unchanging(value)
	
	
	// NESTED	---------------------
	
	private class Unchanging[A](override val value: A) extends Changing[A]
	{
		var listeners = Vector[ChangeListener[A]]()
	}
}

/**
  * Changing instances generate change events
  * @author Mikko Hilpinen
  * @since 26.5.2019, v1.4.1+
  */
trait Changing[A]
{
	// ABSTRACT	---------------------
	
	/**
	  * @return The current value of this changing element
	  */
	def value: A
	
	/**
	 * @return Listeners linked to this changing instance
	 */
	def listeners: Vector[ChangeListener[A]]
	
	/**
	 * Updates this instances listeners
	 * @param newListeners New listeners to associate with this changing instance
	 */
	def listeners_=(newListeners: Vector[ChangeListener[A]]): Unit
	
	
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
	 * @param f A mapping function
	 * @tparam B Mapping result type
	 * @return A mirrored version of this item, using specified mapping function
	 */
	def map[B](f: A => B) = Mirror.of(this)(f)
	
	/**
	 * @param other Another changing item
	 * @param f A merge function
	 * @tparam B Type of the other changing item
	 * @tparam R Type of merge result
	 * @return A mirror that merges the values from both of these items
	 */
	def mergeWith[B, R](other: Changing[B])(f: (A, B) => R) = MergeMirror.of(this, other)(f)
	
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