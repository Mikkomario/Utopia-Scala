package utopia.flow.event

import utopia.flow.async.AsyncMirror
import utopia.flow.datastructure.template.{ListenableLazyLike, Viewable}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.Duration
import scala.util.Try

object ChangingLike
{
	// EXTENSIONS	-----------------
	
	implicit class ChangingFlag(val c: ChangingLike[Boolean]) extends AnyVal
	{
		// COMPUTED	-----------------
		
		/**
		  * @return Whether this flag will always remain true
		  */
		def isAlwaysTrue = c.existsFixed { b => b }
		
		/**
		  * @return Whether this flag will always remain false
		  */
		def isAlwaysFalse = c.existsFixed { !_ }
		
		/**
		  * @return A reversed copy of this flag
		  */
		def unary_! = c.fixedValue match
		{
			case Some(fixed) => if (fixed) AlwaysFalse else AlwaysTrue
			case None => c.map { !_ }
		}
		
		
		// OTHER	-----------------
		
		/**
		  * @param other Another flag
		  * @return A flag that contains true when both of these flags contain true
		  */
		def &&(other: ChangingLike[Boolean]) =
		{
			// If one of the pointers is always false, returns always false
			// If one of the pointers is always true, returns the other pointer
			// If both are changing, returns a combination of these pointers
			c.fixedValue match
			{
				case Some(fixed) => if (fixed) other else AlwaysFalse
				case None =>
					other.fixedValue match
					{
						case Some(fixed) => if (fixed) c else AlwaysFalse
						case None => c.mergeWith(other) { _ && _ }
					}
			}
		}
		
		/**
		  * @param other Another flag
		  * @return A flag that contains true when either one of these flags contains true
		  */
		def ||(other: ChangingLike[Boolean]) =
		{
			// If one of the pointers is always false, returns the other pointer
			// If one of the pointers is always true, returns always true
			// If both are changing, returns a combination of these pointers
			c.fixedValue match
			{
				case Some(fixed) => if (fixed) AlwaysTrue else other
				case None =>
					other.fixedValue match
					{
						case Some(fixed) => if (fixed) AlwaysTrue else c
						case None => c.mergeWith(other) { _ || _ }
					}
			}
		}
	}
}

/**
  * A common trait for items which have the potential of changing their contents and generating change events, although
  * they may not utilize it.
  * @author Mikko Hilpinen
  * @since 26.5.2019, v1.9
  */
trait ChangingLike[+A] extends Viewable[A]
{
	// ABSTRACT	---------------------
	
	/**
	  * @return Whether this item will <b>ever</b> change its value.
	  */
	def isChanging: Boolean
	
	/**
	  * Registers a new listener to be informed whenever this item's value changes
	  * @param changeListener A listener that should be informed (call by name)
	  */
	def addListener(changeListener: => ChangeListener[A]): Unit
	
	/**
	  * Registers a new listener to be informed whenever this item's value changes
	  * @param simulatedOldValue A simulated 'old' value for this changing item to inform the listener of
	  *                                        the initial state of this item. Won't inform the listener
	  *                                        if equal to this item's current value.
	  * @param changeListener A listener that should be informed (call by name)
	  */
	def addListenerAndSimulateEvent[B >: A](simulatedOldValue: B)(changeListener: => ChangeListener[B]): Unit
	
	/**
	  * Makes sure the specified change listener won't be informed of possible future change events
	  * @param changeListener A listener to no longer be informed
	  */
	def removeListener(changeListener: Any): Unit
	
	/**
	  * Registers a new high-priority listener to be informed whenever this item's value changes.
	  * These dependencies must be informed first before triggering any normal listeners.
	  * @param dependency A dependency to add (call by name)
	  */
	def addDependency(dependency: => ChangeDependency[A]): Unit
	
	/**
	  * @param valueCondition A condition for finding a suitable future
	  * @param exc Implicit execution context
	  * @return A future where this changing instance's value triggers the specified condition the first time
	  *         (immediately completed if current value already triggers the condition). Please note that the future
	  *         might never complete.
	  */
	def futureWhere(valueCondition: A => Boolean)(implicit exc: ExecutionContext): Future[A]
	
	/**
	  * @param f A mapping function
	  * @tparam B Mapping result type
	  * @return A mirrored version of this item, using specified mapping function
	  */
	def map[B](f: A => B): ChangingLike[B]
	
	/**
	  * @param f A mapping function
	  * @tparam B Mapping result type
	  * @return A lazily mirrored version of this item that uses the specified mapping function
	  */
	def lazyMap[B](f: A => B): ListenableLazyLike[B]
	
	/**
	  * @param other Another changing item
	  * @param f A merge function
	  * @tparam B Type of the other changing item
	  * @tparam R Type of merge result
	  * @return A mirror that merges the values from both of these items
	  */
	def mergeWith[B, R](other: ChangingLike[B])(f: (A, B) => R): ChangingLike[R]
	
	/**
	 * @param first Another changing item
	 * @param second Yet another changing item
	 * @param merge A merge function
	 * @tparam B Type of the second changing item
	 * @tparam C Type of the third changing item
	 * @tparam R Type of merge result
	 * @return A mirror that merges the values from all three of these items
	 */
	def mergeWith[B, C, R](first: ChangingLike[B], second: ChangingLike[C])(merge: (A, B, C) => R): ChangingLike[R]
	
	/**
	  * @param other Another changing item
	  * @param f A merge function
	  * @tparam B Type of the other item's value
	  * @tparam R Type of merge result
	  * @return A mirror that lazily merges the values from both of these items
	  */
	def lazyMergeWith[B, R](other: ChangingLike[B])(f: (A, B) => R): ListenableLazyLike[R]
	
	/**
	  * @param threshold A required pause between changes in this pointer before the view fires a change event
	  * @param exc Implicit execution context
	  * @return A view into this pointer that only fires change events when there is a long enough pause in
	  *         this pointer's changes
	  */
	def delayedBy(threshold: Duration)(implicit exc: ExecutionContext): ChangingLike[A]
	
	
	// COMPUTED	--------------------
	
	/**
	  * @return Whether this item will <b>never</b> change its value
	  */
	def isFixed: Boolean = !isChanging
	
	/**
	  * @return The current fixed value of this pointer (will continue to remain the same)
	  */
	def fixedValue = if (isChanging) None else Some(value)
	
	
	// OTHER	--------------------
	
	/**
	  * @param condition A condition to test fixed values with
	  * @return Whether this changing item is fixed to a value that fulfils the specified condition
	  */
	def existsFixed(condition: A => Boolean) = if (isChanging) false else condition(value)
	
	/**
	  * @param condition A condition to test fixed values with
	  * @return This item if changing or not fixed to a value the specified condition returns true for
	  */
	def notFixedWhere(condition: A => Boolean) =
	{
		if (isChanging)
			Some(this)
		else if (condition(value))
			None
		else Some(this)
	}
	
	/**
	  * Adds a new function to be called whenever this item's value changes
	  * @param onChange A function that will be called whenever this item's value changes but which won't receive the
	  *                 change event itself
	  */
	def addAnyChangeListener(onChange: => Unit) = addListener(ChangeListener.onAnyChange(onChange))
	
	/**
	  * Adds a new dependency to this changing item
	  * @param beforeChange A function called before each change event (accepts change event that will be fired)
	  * @param afterChange A function called after each change event (accepts change event that was fired)
	  */
	def addDependency[B](beforeChange: ChangeEvent[A] => B)(afterChange: B => Unit): Unit =
		addDependency(ChangeDependency.beforeAndAfter(beforeChange)(afterChange))
	
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
	def mapAsyncMerging[A2 >: A, B, R](placeHolderResult: R)(f: A2 => B)(merge: (R, Try[B]) => R)
									  (implicit exc: ExecutionContext) =
		new AsyncMirror[A2, B, R](this, placeHolderResult)(f)(merge)
	
	/**
	  * Simulates a change event for the specified listener, if necessary
	  * @param listener A listener to inform
	  * @param simulatedOldValue A simulated old value for this item. A change event will be generated only if this
	  *                          value is different from this item's current value.
	  * @tparam B Type of the simulated value / listener
	  */
	protected def simulateChangeEventFor[B >: A](listener: => ChangeListener[B], simulatedOldValue: B) =
	{
		val current = value
		if (simulatedOldValue != current)
			listener.onChangeEvent(ChangeEvent(simulatedOldValue, current))
	}
	
	/**
	  * A default implementation of the 'futureWhere' function
	  * @param condition Condition to search for values with
	  * @param exc An implicit execution context
	  * @return A value future where the specified condition returns true (may never complete)
	  */
	protected def defaultFutureWhere(condition: A => Boolean)(implicit exc: ExecutionContext) =
	{
		// Only listens to changes while this instance is still changing
		if (isChanging)
		{
			val listener = new FutureValueListener(value, condition)
			addListener(listener)
			// Will not need to listen anymore once the future has been completed
			listener.future.foreach { _ => removeListener(listener) }
			listener.future
		}
		else if (condition(value))
			Future.successful(value)
		else
			Future.never
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

