package utopia.flow.view.template.eventful

import utopia.flow.async.AsyncExtensions._
import utopia.flow.event.listener.{ChangeDependency, ChangeListener}
import utopia.flow.event.model.{ChangeEvent, DetachmentChoice}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.{AsyncMirror, ChangeFuture, DelayedView, Fixed, FlatteningMirror, LazyMergeMirror, LazyMirror, ListenableLazy, MergeMirror, Mirror, TripleMergeMirror}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

object Changing
{
	/**
	  * Creates a changing item that changes its value once a future resolves (successfully or unsuccessfully)
	  * @param placeholder A placeholder value returned until the future resolves
	  * @param future A future
	  * @param processResult A function to process the successful or failed future result once it arrives
	  * @param exc Implicit execution context
	  * @tparam A Type of processed future result, as well as the placeholder value
	  *           (i.e. the type of value stored in the changing item)
	  * @tparam F Type of value yielded by the future, when successful
	  * @return A changing item, based on the future
	  */
	def future[A, F](placeholder: => A, future: Future[F])(processResult: Try[F] => A)
	                (implicit exc: ExecutionContext) =
		future.currentResult match {
			case Some(result) => Fixed(processResult(result))
			case None => ChangeFuture.merging(placeholder, future) { (_, result) => processResult(result) }
		}
	/**
	  * Wraps a future into a changing item
	  * @param placeholder A placeholder value to be returned until the future resolves
	  *                    (call-by-name, not called if the future has already resolved)
	  * @param future A future to wrap
	  * @param exc Implicit execution context
	  * @param log Implicit logging implementation to catch errors thrown by the future
	  * @tparam A Type of future result
	  * @return A changing item that acquires the value of a future once it successfully resolves.
	  *         Until this time (and if the future fails), this item will contain the specified placeholder value.
	  */
	def wrapFuture[A](placeholder: => A, future: Future[A])(implicit exc: ExecutionContext, log: Logger) =
		this.future[A, A](placeholder, future) {
			case Success(item) => item
			case Failure(error) =>
				log(error)
				placeholder
		}
	/**
	  * Wraps a future into a changing item
	  * that contains None while unresolved (or failed) and Some once (or if) successfully resolved
	  * @param future A future to wrap
	  * @param exc    Implicit execution context
	  * @param log    Implicit logging implementation to catch errors thrown by the future
	  * @tparam A Type of future result
	  * @return A changing item, based on the future
	  */
	def wrapFutureToOption[A](future: Future[A])(implicit exc: ExecutionContext, log: Logger) =
		this.future[Option[A], A](None, future) { _.toOption }
}

/**
  * A common trait for items which have the potential of changing their contents and generating change events, although
  * they may not utilize it.
  * @author Mikko Hilpinen
  * @since 26.5.2019, v1.9
  */
trait Changing[+A] extends Any with View[A]
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
	  * Removes a change dependency from being activated in the future
	  * @param dependency A dependency to remove from this item
	  */
	def removeDependency(dependency: Any): Unit
	
	
	// COMPUTED	--------------------
	
	/**
	  * @return Whether this item will <b>never</b> change its value
	  */
	def isFixed: Boolean = !isChanging
	/**
	  * @return The current fixed value of this pointer (will continue to remain the same)
	  */
	def fixedValue = if (isChanging) None else Some(value)
	
	
	// IMPLEMENTED  -----------------
	
	override def mapValue[B](f: A => B) = map(f)
	
	
	// OTHER	--------------------
	
	/**
	  * @param valueCondition A condition for finding a suitable future
	  * @return A future where this changing instance's value triggers the specified condition the first time
	  *         (immediately completed if current value already triggers the condition).
	  *         Please note that the resulting future might never complete.
	  */
	def futureWhere(valueCondition: A => Boolean) = _futureWhere(valueCondition)
	/**
	  * @param valueCondition A condition for finding a suitable future
	  * @return A future where this changing instance's value triggers the specified condition the first time
	  *         (but not with this item's current value).
	  *         Please note that the resulting future might never complete.
	  */
	def nextFutureWhere(valueCondition: A => Boolean) =
		_futureWhere(valueCondition, disableImmediateTrigger = true)
	/**
	  * @param f A mapping function that yields Some for the value that resolves the resulting future
	  * @tparam B Type of mapping result, when defined
	  * @return A future that completes once 'f' returns Some for a value in this changing item.
	  *         Resolves immediately if 'f' yields Some for the current value of this item.
	  */
	def findMapFuture[B](f: A => Option[B]) = _findMapFuture[B](f)
	/**
	  * @param f A mapping function that yields Some for the value that resolves the resulting future
	  * @tparam B Type of mapping result, when defined
	  * @return A future that completes once 'f' returns Some for a value in this changing item.
	  *         The current value in this item is not tested with 'f'.
	  */
	def findMapNextFuture[B](f: A => Option[B]) = _findMapFuture[B](f, disableImmediateTrigger = true)
	
	/**
	  * @param f A mapping function
	  * @tparam B Mapping result type
	  * @return A mirrored version of this item, using specified mapping function
	  */
	def map[B](f: A => B): Changing[B] = diverge { Mirror(this)(f) } { f(value) }
	/**
	  * Creates a new changing item that contains a mapped value of this item.
	  * The mapping function used acquires additional contextual information.
	  * @param initialMap A function to perform the initial mapping.
	  *                   Accepts the current value of this item.
	  * @param incrementMap A function to perform the consecutive mappings.
	  *                     Accepts the previous mapping result, as well as the change event that occurred in this item.
	  * @tparam B Type of mapping results
	  * @return A new changing item
	  */
	def incrementalMap[B](initialMap: A => B)(incrementMap: (B, ChangeEvent[A]) => B) =
		diverge { Mirror.incremental(this)(initialMap)(incrementMap) } { initialMap(value) }
	/**
	  * @param f A mapping function
	  * @tparam B Mapping result type
	  * @return A lazily mirrored version of this item that uses the specified mapping function
	  */
	def lazyMap[B](f: A => B): ListenableLazy[B] = lazyDiverge { LazyMirror(this)(f) } { f(value) }
	
	/**
	  * @param other Another changing item
	  * @param f A merge function
	  * @tparam B Type of the other changing item
	  * @tparam R Type of merge result
	  * @return A mirror that merges the values from both of these items
	  */
	def mergeWith[B, R](other: Changing[B])(f: (A, B) => R): Changing[R] =
		divergeMerge[B, Changing[R]](other) { MergeMirror(this, other)(f) } { v2 => map { f(_, v2) } } {
			Mirror(other) { v2 => f(value, v2) } }
	/**
	  * @param first Another changing item
	  * @param second Yet another changing item
	  * @param merge A merge function
	  * @tparam B Type of the second changing item
	  * @tparam C Type of the third changing item
	  * @tparam R Type of merge result
	  * @return A mirror that merges the values from all three of these items
	  */
	def mergeWith[B, C, R](first: Changing[B], second: Changing[C])(merge: (A, B, C) => R): Changing[R] =
		TripleMergeMirror.of(this, first, second)(merge)
	/**
	  * Creates a new changing item that combines the values of this and the other item.
	  * The merge function used acquires additional information.
	  * @param other Another changing item
	  * @param initialMerge A merge function used for acquiring the initial merge result.
	  *                     Accepts the values of this and the other item.
	  * @param incrementMerge A merge function used for acquiring consecutive merge results.
	  *                       Accepts:
	  *                         1) The previous merge result,
	  *                         2) The current value of this item,
	  *                         3) The current value of the other item,
	  *                         4) Either:
	  *                             Left) A change event that occurred in this item, or
	  *                             Right) A change event that occurred in the other item.
	  *                       Yields a merge result.
	  * @tparam B Type of value(s) in the other item
	  * @tparam R Type of merge results
	  * @return A new changing item
	  */
	def incrementalMergeWith[B, R](other: Changing[B])(initialMerge: (A, B) => R)
	                              (incrementMerge: (R, A, B, Either[ChangeEvent[A], ChangeEvent[B]]) => R) =
		divergeMerge[B, Changing[R]](other) {
			MergeMirror.incremental(this, other)(initialMerge)(incrementMerge) } { v2 =>
			incrementalMap { initialMerge(_, v2) } { (r, e1) => incrementMerge(r, e1.newValue, v2, Left(e1)) } } {
			Mirror.incremental(other) { initialMerge(value, _) } { (r, e2) =>
				incrementMerge(r, value, e2.newValue, Right(e2)) } }
	/**
	  * @param other Another changing item
	  * @param f A merge function
	  * @tparam B Type of the other item's value
	  * @tparam R Type of merge result
	  * @return A mirror that lazily merges the values from both of these items
	  */
	def lazyMergeWith[B, R](other: Changing[B])(f: (A, B) => R): ListenableLazy[R] =
		divergeMerge[B, ListenableLazy[R]](other) { LazyMergeMirror(this, other)(f) } { v2 => lazyMap { f(_, v2) } } {
			LazyMirror(other) { f(value, _) } }
	
	/**
	  * Maps this changing item with a function that yields changing items.
	  * The resulting changing will match the value of the most recent map result.
	  *
	  * Please note that listeners and dependencies attached to the map results, and not to the result of this function,
	  * will not be carried over to future map results.
	  *
	  * @param f A mapping function that yields changing items
	  * @tparam B Type of values in the resulting items
	  * @return A pointer to the current value of the last map result
	  */
	def flatMap[B](f: A => Changing[B]) = {
		if (isChanging)
			FlatteningMirror(this)(f)
		else
			f(value)
	}
	/**
	  * Maps this changing item with a function that yields other changing items.
	  * These are wrapped under a single "Changing" interface.
	  * The specified mapping function receives additional contextual (state) information.
	  * @param initialMap A mapping function that accepts the current value of this pointer and yields another pointer.
	  * @param incrementMap A mapping function used for mapping the consecutive values / changes.
	  *                     Accepts:
	  *                         1) The previous mapping result (a pointer), and
	  *                         2) The change event that occurred in this pointer
	  *                     Yields pointers.
	  * @tparam B Type of mapping result pointers' values
	  * @return A new pointer that wraps the mapping result pointers
	  */
	def incrementalFlatMap[B](initialMap: A => Changing[B])
	                         (incrementMap: (Changing[B], ChangeEvent[A]) => Changing[B]) =
	{
		if (isChanging)
			FlatteningMirror.incremental(this)(initialMap)(incrementMap)
		else
			initialMap(value)
	}
	
	/**
	  * @param threshold A required pause between changes in this pointer before the view fires a change event
	  * @param exc Implicit execution context
	  * @return A view into this pointer that only fires change events when there is a long enough pause in
	  *         this pointer's changes
	  */
	def delayedBy(threshold: => Duration)(implicit exc: ExecutionContext): Changing[A] = {
		// Case: This item may change
		if (isChanging)
			threshold.finite match {
				// Case: A finite delay has been defined
				case Some(duration) =>
					// Case: Zero delay = View of this item
					if (duration <= Duration.Zero)
						this
					// Case: Positive delay => Creates a delayed view
					else
						DelayedView(this, duration)
				// Case: Infinite delay = Fixed value
				case None => Fixed(value)
			}
		// Case: This item doesn't change anymore => No need for delay
		else
			this
	}
	
	/**
	  * @param condition A condition to test fixed values with
	  * @return Whether this changing item is fixed to a value that fulfils the specified condition
	  */
	def existsFixed(condition: A => Boolean) = if (isChanging) false else condition(value)
	/**
	  * @param condition A condition to test fixed values with
	  * @return This item if changing or not fixed to a value the specified condition returns true for
	  */
	def notFixedWhere(condition: A => Boolean) = {
		if (isChanging)
			Some(this)
		else if (condition(value))
			None
		else
			Some(this)
	}
	
	/**
	  * Adds a new listener that will be informed whenever this item changes
	  * @param listener A function called whenever this item changes. Accepts a change event.
	  * @tparam U Arbitrary result type
	  */
	def addContinuousListener[U](listener: ChangeEvent[A] => U) = addListener(ChangeListener.continuous(listener))
	/**
	  * Adds a new listener that will be informed whenever this item changes.
	  * May create and fire a simulated change event for that listener.
	  * @param simulatedOldValue A simulated "old value" for this item, compared to this item's current value
	  * @param listener A listener function to call on change events
	  * @tparam B Type of simulated value
	  * @tparam U Arbitrary function result type
	  */
	def addContinuousListenerAndSimulateEvent[B >: A, U](simulatedOldValue: B)(listener: ChangeEvent[B] => U) =
		addListenerAndSimulateEvent(simulatedOldValue)(ChangeListener.continuous(listener))
	
	/**
	  * Adds a new function to be called whenever this item's value changes
	  * @param onChange A function that will be called whenever this item's value changes but which won't receive the
	  *                 change event itself
	  */
	def addContinuousAnyChangeListener[U](onChange: => U) =
		addListener(ChangeListener.continuousOnAnyChange(onChange))
	/**
	  * Adds a new function to be called whenever this item's value changes
	  * @param onChange A function that will be called whenever this item's value changes but which won't receive the
	  *                 change event itself. Returns whether future change events should also trigger this function.
	  */
	def addAnyChangeListener(onChange: => DetachmentChoice) = addListener(ChangeListener.onAnyChange(onChange))
	
	/**
	  * Calls the specified function when this item changes the next time
	  * @param f A function that will be called when this item changes
	  * @tparam U Arbitrary function result type
	  */
	def onNextChange[U](f: ChangeEvent[A] => U) = addListener(ChangeListener.once(f))
	/**
	  * @param condition A condition that must be fulfilled for the specified function to be called
	  * @param f A function that is called once a change event satisfies the specified condition.
	  *          Will only be called up to once.
	  * @tparam U Arbitrary function result type
	  */
	def onNextChangeWhere[U](condition: ChangeEvent[A] => Boolean)(f: ChangeEvent[A] => U) = addListener { e =>
		if (condition(e)) {
			f(e)
			DetachmentChoice.detach
		}
		else
			DetachmentChoice.continue
	}
	/**
	  * Calls the specified function once the value of this item satisfies the specified condition.
	  * If the current item satisfies the condition, the function is called immediately.
	  * @param condition A condition that the item in this pointer must satisfy
	  * @param f A function that shall be called for the item that satisfies the specified condition.
	  *          Will be called only up to once.
	  * @tparam U Arbitrary function result type
	  */
	def once[U](condition: A => Boolean)(f: A => U): Unit = {
		// Case: Current values is accepted => Calls the specified function for the current value
		if (condition(value))
			f(value)
		// Case: Current value is not accepted => Waits for a suitable value
		else
			onNextChangeWhere { e => condition(e.newValue) } { e => f(e.newValue) }
	}
	
	/**
	  * Adds a new dependency to this changing item
	  * @param beforeChange A function called before each change event (accepts change event that will be fired)
	  * @param afterChange A function called after each change event (accepts change event that was fired)
	  */
	def addDependency[B](beforeChange: ChangeEvent[A] => B)(afterChange: B => Unit): Unit =
		addDependency(ChangeDependency.beforeAndAfter(beforeChange)(afterChange))
	
	/**
	  * Creates an asynchronously mapping view of this changing item
	  * @param placeHolderResult Value placed in the view before the map result has been calculated
	  * @param skipInitialMap Whether the initial mapping process (i.e. the mapping of this item's current value)
	  *                       should be skipped, and the placeholder be used instead.
	  *                       Suitable for situations where the placeholder is a proper mapping result.
	  *                       Default = false.
	  * @param f An asynchronous mapping function
	  * @param merge A function for handling possible error cases and merging received results with those
	  *              previously acquired
	  * @param exc Implicit execution context
	  * @tparam B Type of mapping result
	  * @tparam R Type of merged / reduced mapping results
	  * @return An asynchronously mapped view of this changing item
	  */
	def mapAsync[A2 >: A, B, R](placeHolderResult: R, skipInitialMap: Boolean = false)
	                           (f: A2 => Future[B])
	                           (merge: (R, Try[B]) => R)
	                           (implicit exc: ExecutionContext) =
		AsyncMirror[A2, B, R](this, placeHolderResult, skipInitialMap)(f)(merge)
	/**
	  * Creates an asynchronously mapping view of this changing item
	  * @param placeHolderResult Value placed in the view before the map result has been calculated
	  * @param skipInitialMap Whether the initial mapping process (i.e. the mapping of this item's current value)
	  *                       should be skipped, and the placeholder be used instead.
	  *                       Suitable for situations where the placeholder is a proper mapping result.
	  *                       Default = false.
	  * @param f An asynchronous mapping function that may fail
	  * @param merge A function for handling possible error cases and merging received results with those
	  *              previously acquired
	  * @param exc Implicit execution context
	  * @tparam B Type of mapping result
	  * @return An asynchronously mapped view of this changing item
	  */
	def tryMapAsync[B](placeHolderResult: B, skipInitialMap: Boolean = false)(f: A => Future[Try[B]])
	                  (merge: (B, Try[B]) => B)
	                  (implicit exc: ExecutionContext) =
		mapAsync(placeHolderResult, skipInitialMap)(f) { (previous, result) => merge(previous, result.flatten) }
	/**
	  * Creates an asynchronously mapping view of this changing item.
	  * In cases where the asynchronous mapping fails, errors are simply logged and treated as if no
	  * mapping was even requested / as if the value of this pointer didn't change.
	  * @param placeHolderResult Value placed in the view before the map result has been calculated
	  * @param skipInitialMap Whether the initial mapping process (i.e. the mapping of this item's current value)
	  *                       should be skipped, and the placeholder be used instead.
	  *                       Suitable for situations where the placeholder is a proper mapping result.
	  *                       Default = false.
	  * @param f An asynchronous mapping function
	  * @param exc Implicit execution context
	  * @param logger An implicit logger that will receive encountered errors
	  * @tparam B Type of mapping result
	  * @return An asynchronously mapped view of this changing item
	  */
	def mapAsyncCatching[B](placeHolderResult: B, skipInitialMap: Boolean = false)
	                       (f: A => Future[B])
	                       (implicit exc: ExecutionContext, logger: Logger) =
		AsyncMirror.catching(this, placeHolderResult, skipInitialMap)(f)
	/**
	  * Creates an asynchronously mapping view of this changing item.
	  * In cases where the asynchronous mapping fails, errors are simply logged and treated as if no
	  * mapping was even requested / as if the value of this pointer didn't change.
	  * @param placeHolderResult Value placed in the view before the map result has been calculated
	  * @param skipInitialMap Whether the initial mapping process (i.e. the mapping of this item's current value)
	  *                       should be skipped, and the placeholder be used instead.
	  *                       Suitable for situations where the placeholder is a proper mapping result.
	  *                       Default = false.
	  * @param f An asynchronous mapping function that may fail
	  * @param exc Implicit execution context
	  * @param logger An implicit logger that will receive encountered errors
	  * @tparam B Type of mapping result
	  * @return An asynchronously mapped view of this changing item
	  */
	def mapAsyncTryCatching[B](placeHolderResult: B, skipInitialMap: Boolean = false)(f: A => Future[Try[B]])
	                          (implicit exc: ExecutionContext, logger: Logger) =
		AsyncMirror.tryCatching(this, placeHolderResult, skipInitialMap)(f)
	
	/**
	  * Simulates a change event for the specified listener, if necessary
	  * @param listener A listener to inform
	  * @param simulatedOldValue A simulated old value for this item. A change event will be generated only if this
	  *                          value is different from this item's current value.
	  * @tparam B Type of the simulated value / listener
	  * @return Whether the listener should be informed of future change events
	  */
	protected def simulateChangeEventFor[B >: A](listener: => ChangeListener[B], simulatedOldValue: B) =
	{
		val current = value
		if (simulatedOldValue != current)
			listener.onChangeEvent(ChangeEvent(simulatedOldValue, current))
		else
			DetachmentChoice.continue
	}
	
	/**
	  * Creates a new changing item based on this item,
	  * returning a fixed pointer in case this item is no longer changing
	  * @param ifChanging    Result to give if this item is still changing (call-by-name)
	  * @param ifNotChanging A fixed value to return if this item is no longer changing (call-by-name)
	  * @tparam B Type of new changing or fixed value
	  * @return A new changing item
	  */
	protected def diverge[B](ifChanging: => Changing[B])(ifNotChanging: => B) = {
		if (isChanging)
			ifChanging
		else
			Fixed(ifNotChanging)
	}
	/**
	  * Creates a new lazy container based on this item.
	  * A more simple container is created if this item is no longer changing.
	  * @param ifChanging Result (lazy container) to give if this item is still changing (call-by-name)
	  * @param ifNotChanging A fixed value to return if this item is no longer changing (call-by-name, lazy)
	  * @tparam B Type of new changing or fixed value
	  * @return A new lazy container
	  */
	protected def lazyDiverge[B](ifChanging: => ListenableLazy[B])(ifNotChanging: => B) = {
		if (isChanging)
			ifChanging
		else
			Lazy.listenable(ifNotChanging)
	}
	/**
	  * Creates a new changing item by combining this item with another.
	  * Uses a more simple function in case the other item doesn't change anymore.
	  * @param other Another (possibly) changing item
	  * @param ifBothChange A result to yield in case both items may still change
	  * @param ifOtherIsFixed A function for producing the result in case the other item is no longer changing.
	  *                       Accepts the fixed value of the other item.
	  * @param ifOnlyThisIsFixed A function for producing the result in case this item is fixed and the other is not.
	  * @tparam B Type of values in the other item
	  * @tparam R Type of merge result
	  * @return A new changing item
	  */
	protected def divergeMerge[B, R](other: Changing[B])(ifBothChange: => R)
	                                (ifOtherIsFixed: B => R)
	                                (ifOnlyThisIsFixed: => R) =
	{
		if (other.isChanging) {
			if (isChanging)
				ifBothChange
			else
				ifOnlyThisIsFixed
		}
		else
			ifOtherIsFixed(other.value)
	}
	
	/**
	  * A default implementation of the 'futureWhere' function
	  * @param condition Condition to search for values with
	  * @return A value future where the specified condition returns true (may never complete)
	  */
	protected def _futureWhere(condition: A => Boolean, disableImmediateTrigger: Boolean = false) =
		_findMapFuture[A](a => Some(a).filter(condition), disableImmediateTrigger)
	/**
	  * A default implementation of the 'futureWhere' function
	  * @param condition Condition to search for values with
	  * @return A value future where the specified condition returns true (may never complete)
	  */
	protected def _findMapFuture[B](condition: A => Option[B], disableImmediateTrigger: Boolean = false) =
	{
		val initialCandidate = if (disableImmediateTrigger) None else Some(value)
		initialCandidate.flatMap(condition) match {
			// Case: Completes with the current value
			case Some(result) => Future.successful(result)
			case None =>
				// Case: May change => Listens to changes until the searched state is found
				if (isChanging) {
					val promise = Promise[B]()
					addListener { e =>
						condition(e.newValue) match {
							case Some(result) =>
								promise.trySuccess(result)
								DetachmentChoice.detach
							case None => DetachmentChoice.continue
						}
					}
					promise.future
				}
				// Case: Impossible to succeed
				else
					Future.never
		}
	}
}

