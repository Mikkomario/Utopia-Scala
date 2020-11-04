package utopia.flow.event

import utopia.flow.async.{AsyncMirror, DelayedView}
import utopia.flow.datastructure.mutable.MutableLazy
import utopia.flow.datastructure.template.{LazyLike, Viewable}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

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
		// ATTRIBUTES	-------------
		
		var listeners = Vector[ChangeListener[A]]()
		
		
		// IMPLEMENTED	-------------
		
		override def addListener(changeListener: ChangeListener[A], generateChangeEventFromOldValue: Option[A]) =
		{
			// Will not add the listener since no change in value will ever be made (no events will be fired either)
			// May fire the initial "simulated" event, however
			generateChangeEventFromOldValue.filterNot { _ == value }
				.foreach { old => changeListener.onChangeEvent(ChangeEvent(old, value)) }
		}
		
		override def futureWhere(valueCondition: A => Boolean)(implicit exc: ExecutionContext) =
		{
			// Will return either a completed future or a future that never completes.
			// Will not react or test for changes because there won't be any
			if (valueCondition(value))
				Future.successful(value)
			else
				Future.never
		}
		
		// Simplifies the mapping function since it's known that the mapping only needs to happen once
		override def map[B](f: A => B): Changing[B] = new Unchanging(f(value))
		
		// Instead of reacting to changes, lazily maps the value once
		override def lazyMap[B](f: A => B) = MutableLazy { f(value) }
		
		// Since this value won't vary, only reacts to the other pointer's changes
		override def mergeWith[B, R](other: Changing[B])(f: (A, B) => R) = other.map { f(value, _) }
		
		// Since this value won't vary, only reacts to the other pointer's changes (lazily)
		override def lazyMergeWith[B, R](other: Changing[B])(f: (A, B) => R) =
			other.lazyMap { f(value, _) }
		
		// Delay has no effect on this pointer so it can just return itself
		override def delayedBy(threshold: Duration)(implicit exc: ExecutionContext): Changing[A] = this
	}
}

/**
  * Changing instances generate change events
  * @author Mikko Hilpinen
  * @since 26.5.2019, v1.4.1
  */
trait Changing[A] extends Viewable[A]
{
	// ABSTRACT	---------------------
	
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
	def map[B](f: A => B): Changing[B] = Mirror.of(this)(f)
	
	/**
	  * @param f A mapping function
	  * @tparam B Mapping result type
	  * @return A lazily mirrored version of this item that uses the specified mapping function
	  */
	def lazyMap[B](f: A => B): LazyLike[B] = LazyMirror.of(this)(f)
	
	/**
	 * @param other Another changing item
	 * @param f A merge function
	 * @tparam B Type of the other changing item
	 * @tparam R Type of merge result
	 * @return A mirror that merges the values from both of these items
	 */
	def mergeWith[B, R](other: Changing[B])(f: (A, B) => R): Changing[R] = MergeMirror.of(this, other)(f)
	
	/**
	  * @param other Another changing item
	  * @param f A merge function
	  * @tparam B Type of the other item's value
	  * @tparam R Type of merge result
	  * @return A mirror that lazily merges the values from both of these items
	  */
	def lazyMergeWith[B, R](other: Changing[B])(f: (A, B) => R): LazyLike[R] =
		LazyMergeMirror.of(this, other)(f)
	
	/**
	  * @param threshold A required pause between changes in this pointer before the view fires a change event
	  * @param exc Implicit execution context
	  * @return A view into this pointer that only fires change events when there is a long enough pause in
	  *         this pointer's changes
	  */
	def delayedBy(threshold: Duration)(implicit exc: ExecutionContext): Changing[A] =
		new DelayedView(this, threshold)
	
	/**
	  * Creates an asynchronously mapping view of this changing item
	  * @param placeHolderResult Value placed in the view before the first value has been calculated
	  * @param f A synchronous mapping function that catches errors, returning a try
	  * @param errorHandler A function called for all received errors
	  * @param exc Implicit execution context
	  * @tparam B Successful mapping result type
	  * @return An asynchronously mapped view of this changing item
	  */
	def tryMapAsync[B](placeHolderResult: B)(f: A => Try[B])(errorHandler: Throwable => Unit)
	                       (implicit exc: ExecutionContext) =
		AsyncMirror.trying(this, placeHolderResult)(f)(errorHandler)
	
	/**
	  * Creates an asynchronously mapping view of this changing item
	  * @param placeHolderResult Value placed in the view before the first value has been calculated
	  * @param f A synchronous mapping function that may throw errors
	  * @param errorHandler A function called for all catched errors
	  * @param exc Implicit execution context
	  * @tparam B Successful mapping result type
	  * @return An asynchronously mapped view of this changing item
	  */
	def mapAsyncCatching[B](placeHolderResult: B)(f: A => B)(errorHandler: Throwable => Unit)
	                       (implicit exc: ExecutionContext) =
		AsyncMirror.catching(this, placeHolderResult)(f)(errorHandler)
	
	/**
	  * Creates an asynchronously mapping view of this changing item
	  * @param placeHolderResult Value placed in the view before the first value has been calculated
	  * @param f A synchronous mapping function that is not expected to throw errors (if it throws, those errors
	  *          are printed yet not propagated)
	  * @param exc Implicit execution context
	  * @tparam B Mapping result type
	  * @return An asynchronously mapped view of this changing item
	  */
	def mapAsync[B](placeHolderResult: B)(f: A => B)(implicit exc: ExecutionContext) =
		AsyncMirror(this, placeHolderResult)(f)
	
	/**
	  * Creates an asynchronously mapping view of this changing item
	  * @param placeHolderResult Value placed in the view before the first value has been calculated
	  * @param f A synchronous mapping function that may throw
	  * @param merge A function for handling possible error cases and merging gained results with those
	  *              previously acquired
	  * @param exc Implicit execution context
	  * @tparam B Type of mapping result
	  * @tparam R Type of merged / reduced mapping results
	  * @return An asynchronously mapped view of this changing item
	  */
	def mapAsyncMerging[B, R](placeHolderResult: R)(f: A => B)(merge: (R, Try[B]) => R)(implicit exc: ExecutionContext) =
		new AsyncMirror[A, B, R](this, placeHolderResult)(f)(merge)
	
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