package utopia.flow.view.template.eventful

import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.event.listener.{ChangeDependency, ChangeListener, ChangingStoppedListener, ConditionalChangeReaction}
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.event.model.Destiny.{ForeverFlux, MaySeal, Sealed}
import utopia.flow.event.model.{ChangeEvent, ChangeResponse, ChangeResult, Destiny}
import utopia.flow.operator.enumeration.End
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.operator.{Identity, MaybeEmpty}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful._
import utopia.flow.view.template.eventful.Flag.wrap

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

object Changing
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * A limit to place on the number of listeners that may be assigned to an individual pointer.
	  * Setting this value to >= 0 will cause this software to throw errors when too many listeners are assigned.
	  *
	  * Please note that this feature is NOT INDENTED FOR USE IN PRODUCTION
	  * and is merely for testing and debugging purposes.
	  */
	var listenerDebuggingLimit = -1
	
	
	// OTHER    --------------------------
	
	/**
	  * Creates a changing item that changes its value once a future resolves (successfully or unsuccessfully)
	  * @param placeholder A placeholder value returned until the future resolves
	  * @param future A future
	  * @param processResult A function to process the successful or failed future result once it arrives
	  * @param exc Implicit execution context
	  * @param log Implicit logging implementation for handling failures thrown by assigned listeners
	  * @tparam A Type of processed future result, as well as the placeholder value
	  *           (i.e. the type of value stored in the changing item)
	  * @tparam F Type of value yielded by the future, when successful
	  * @return A changing item, based on the future
	  */
	def future[A, F](placeholder: => A, future: Future[F])(processResult: Try[F] => A)
	                (implicit exc: ExecutionContext, log: Logger) =
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
	/**
	 * Creates a pointer that tracks the completion of a future, whether successful or failed
	 * @param future A future to track
	 * @param exc    Implicit execution context
	 * @param log Implicit logging implementation for handling failures thrown by assigned listeners
	  * @return A pointer that contains true when the specified future has completed
	 */
	def completionOf(future: Future[Any])(implicit exc: ExecutionContext, log: Logger): Flag = {
		if (future.isCompleted)
			AlwaysTrue
		else
			ChangeFuture.merging[Boolean, Any](false, future) { (_, _) => true }
	}
	
	
	// NESTED   ----------------------------
	
	/**
	 * This trait provides utility functions for Changing items that wrap items which may be empty
	 * @tparam A Type of the changing values
	 */
	trait MayBeEmptyChangingWrapper[A] extends Any
	{
		// ABSTRACT -----------------------
		
		/**
		 * @return The wrapped changing item
		 */
		protected def wrapped: Changing[A]
		
		/**
		 * Tests if a value is empty
		 * @param value A value
		 * @return Whether the specified value is empty
		 */
		protected def _isEmpty(value: A): Boolean
		
		
		// COMPUTED -----------------------
		
		/**
		 * @return Whether the current value of this item is empty
		 */
		def isCurrentlyEmpty = _isEmpty(wrapped.value)
		/**
		 * @return Whether the current value of this item is not empty
		 */
		def isCurrentlyNonEmpty = !isCurrentlyEmpty
		
		/**
		 * @return Whether this item is and will remain empty forever
		 */
		def isAlwaysEmpty = wrapped.existsFixed(_isEmpty)
		/**
		 * @return Whether this item is or may become non-empty
		 */
		def mayBeNonEmpty = !isAlwaysEmpty
		
		/**
		 * @return Whether this item is and will remain non-empty forever
		 */
		def isAlwaysNonEmpty = wrapped.existsFixed { !_isEmpty(_) }
		/**
		 * @return Whether this item is or may become empty
		 */
		def mayBeEmpty = !isAlwaysNonEmpty
	}
	
	
	// EXTENSIONS   ------------------------
	
	implicit class AsMayBeEmptyChanging[A <: MaybeEmpty[A]](val c: Changing[A])
		extends AnyVal with MayBeEmptyChangingWrapper[A]
	{
		override protected def wrapped: Changing[A] = c
		override protected def _isEmpty(value: A): Boolean = value.isEmpty
	}
	
	implicit class ChangingCollection[C <: Iterable[_]](val c: Changing[C])
		extends AnyVal with MayBeEmptyChangingWrapper[C]
	{
		override protected def wrapped: Changing[C] = c
		override protected def _isEmpty(value: C): Boolean = value.isEmpty
	}
	
	implicit class DeepChanging[A](val c: Changing[Changing[A]]) extends AnyVal
	{
		/**
		  * @return Copy of this pointer that reflects the value of the wrapped pointer(s)
		  */
		def flatten = c.flatMap(Identity)
	}
}

/**
  * A common trait for items which have the potential of changing their contents and generating change events,
  * although they might choose not utilize this potential.
  * @author Mikko Hilpinen
  * @since 26.5.2019, v1.9
  */
trait Changing[+A] extends View[A]
{
	// ABSTRACT	---------------------
	
	/**
	  * @return A logging implementation used for handling errors thrown
	  *         by the listeners assigned to this changing item (and possibly to the derived items as well)
	  */
	implicit def listenerLogger: Logger
	
	/**
	  * @return The "destiny" if this item.
	  *         Contains information on whether this item is still able to change,
	  *         and/or whether the value of this item may become fixed at some point.
	  */
	def destiny: Destiny
	
	/*
	/**
	  * @return Whether this item will change at some point in the future.
	  *         True (certain) if it is known for certain that there will be at least one more change,
	  *         False (certain) if it is known for certain that there will never be any further changes to this item,
	  *         UncertainBoolean if it is possible that this item may still change, although uncertain whether it will
	  */
	def willChange: UncertainBoolean
	/**
	  * @return Whether this item will stop changing or has stopped changing at some point.
	  *
	  *         True (certain) if this item has already stopped changing,
	  *         or if it is known for certain that it will stop changing in the future
	  *
	  *         False (certain) if it is impossible for this item to stop changing for certain;
	  *         E.g. if the value of this item is based on user interaction or some other outside force,
	  *         as is the value for mutable pointers.
	  *
	  *         UncertainBoolean if it is possible that this item will stop changing
	  *         (i.e. functionality for that exists), but it is still uncertain whether that functionality
	  *         is or will be actuated.
	  */
	def willStopChanging: UncertainBoolean
	*/
	
	/**
	  * @return Whether this pointer is being listened to at the moment
	  */
	def hasListeners: Boolean
	/**
	  * @return Number of listeners registered for this pointer at the moment
	  */
	def numberOfListeners: Int
	
	/**
	  * @return A read-only view into this item
	  */
	def readOnly: Changing[A]
	
	/**
	  * Assigns a new change listener to this item.
	  * The implementation may assume that mayChange has been tested to be true, as no listener should be applied after
	  * changing has stopped.
	  * @param priority Priority assigned to the specified listener, where
	  *                 First is high priority (called first) and Last is standard priority (called afterwards)
	  * @param lazyListener A listener to assign to this item,
	  *                     if appropriate (i.e. if this item doesn't contain that listener already)
	  *                     (specified as a lazily initialized view)
	  */
	protected def _addListenerOfPriority(priority: End, lazyListener: View[ChangeListener[A]]): Unit
	/**
	  * Makes sure the specified change listener won't be informed of possible future change events
	  * @param changeListener A listener to no longer be informed
	  */
	def removeListener(changeListener: Any): Unit
	
	/**
	  * Assigns a new listener to be informed in case this pointer stops from changing
	  * (i.e. isChanging becomes false).
	  *
	  * This method is only called for pointers where isChanging is true and mayStopChanging is true.
	  *
	  * @param listener A listener to assign, if appropriate (call-by-name)
	  */
	protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit
	
	
	// COMPUTED	--------------------
	
	/**
	  * @return Whether this item might ever change its value in the future.
	  */
	def mayChange: Boolean = destiny.hasNotBeenSealed
	/**
	  * @return Whether this item will ever change its value.
	  */
	@deprecated("Please use mayChange instead", "v2.3")
	def isChanging: Boolean = mayChange
	/**
	  * @return Whether this item might stop changing at some point in the future.
	  *         Also returns true if this item has already stopped changing.
	  */
	@deprecated("Please use destiny.isPossibleToSeal instead", "v2.3")
	def mayStopChanging: Boolean = destiny.isPossibleToSeal
	
	/**
	  * @return Whether this item will not change its value anymore
	  */
	def isFixed: Boolean = destiny.hasBeenSealed
	/**
	  * @return The current fixed/static value of this pointer.
	  *         None if it is still possible for this item to change.
	  *         If Some, that will be the final value of this item, and will not change.
	  */
	def fixedValue = if (isFixed) Some(value) else None
	
	/**
	  * @return Whether this pointer is not currently being listened to
	  */
	def hasNoListeners = !hasListeners
	
	/**
	  * @return A future that contains the next change event that occurs in this changing item.
	  *         Please note that the returned future might never resolve.
	  */
	def nextChangeFuture = {
		// Case: There is a possibility that this pointer will mutate => Starts waiting for the next change event
		if (mayChange) {
			val promise = Promise[ChangeEvent[A]]()
			onNextChange(promise.success)
			promise.future
		}
		// Case: It's impossible for this pointer to change anymore => Returns a future that never resolves
		else
			Future.never
	}
	
	/**
	  * @return Copy of this pointer that attaches the "isChanging" state to the value of this pointer
	  */
	def withState = destiny match {
		case Sealed => Fixed(ChangeResult.finalValue(value))
		case MaySeal => StatefulValueView(this)
		case ForeverFlux => lightMap(ChangeResult.temporal)
	}
	
	
	// IMPLEMENTED  -----------------
	
	override def mapValue[B](f: A => B) = map(f)
	
	
	// OTHER	--------------------
	
	/**
	  * @param condition A condition to test fixed values with
	  * @return Whether this changing item is fixed to a value that fulfils the specified condition
	  */
	def existsFixed(condition: A => Boolean) = isFixed && condition(value)
	/**
	  * @param condition A condition to test fixed values with
	  * @return This item if changing or not fixed to a value for which the specified condition returns true
	  */
	def notFixedWhere(condition: A => Boolean) = if (existsFixed(condition)) None else Some(this)
	
	/**
	  * @param priority Priority assigned to the specified listener, where
	  *                 First is high priority (called first) and Last is standard priority (called afterwards)
	  * @param listener A listener to assign to this item,
	  *                 if appropriate (i.e. this item will still change) (call-by-name)
	  */
	def addListenerOfPriority(priority: End)(listener: => ChangeListener[A]): Unit = {
		// Debugging feature: May follow the maximum number of listeners
		if (Changing.listenerDebuggingLimit >= 0 && numberOfListeners >= Changing.listenerDebuggingLimit)
			throw new IllegalStateException(s"Maximum (debugging) limit (${Changing.listenerDebuggingLimit}) of change event listeners reached.")
		if (mayChange)
			_addListenerOfPriority(priority, Lazy(listener))
	}
	
	/**
	  * Registers a new listener to be informed whenever this item's value changes
	  * @param changeListener A listener that should be informed (call by name)
	  */
	def addListener(changeListener: => ChangeListener[A]): Unit = addListenerOfPriority(Last)(changeListener)
	/**
	  * Register a new listener to be informed whenever this item's value changes.
	  * This listener should be considered high priority, and called before the normal priority listeners are
	  * called.
	  * @param listener A listener to inform of future change events (call-by-name)
	  */
	def addHighPriorityListener(listener: => ChangeListener[A]): Unit = addListenerOfPriority(First)(listener)
	/**
	  * Registers a new listener to be informed whenever this item's value changes
	  * @param simulatedOldValue A simulated 'old' value for this changing item to inform the listener of
	  *                          the initial state of this item. Won't inform the listener
	  *                          if equal to this item's current value.
	  * @param isHighPriority    Whether the specified listener should be considered to be
	  *                          of a high priority, meaning that it should be called before the standard
	  *                          priority listeners are.
	  *                          Default = false.
	  * @param changeListener    A listener that should be informed (call by name)
	  */
	def addListenerAndSimulateEvent[B >: A](simulatedOldValue: B, isHighPriority: Boolean = false)
	                                       (changeListener: => ChangeListener[B]): Unit =
	{
		val lazyNewListener = Lazy(changeListener)
		// Performs the simulation
		val simulationResult = simulateChangeEventFor(lazyNewListener.value, simulatedOldValue)
		// Adds the listener, if appropriate (i.e. listener didn't detach itself during the simulation)
		if (simulationResult.shouldContinueListening)
			addListenerOfPriority(if (isHighPriority) First else Last)(lazyNewListener.value)
		// Triggers the caused after-effects
		simulationResult.afterEffects.foreach { _() }
	}
	
	/**
	  * Assigns a new listener to be informed in case this pointer stops from changing
	  * (i.e. isChanging becomes false).
	  *
	  * The listener won't be called if this pointer had already stopped from changing.
	  * If you wish to receive events in these situations as well, please use
	  * [[addChangingStoppedListenerAndSimulateEvent]] instead.
	  *
	  * @param listener A listener to assign, if appropriate (call-by-name)
	  */
	def addChangingStoppedListener(listener: => ChangingStoppedListener): Unit = {
		if (destiny == MaySeal)
			_addChangingStoppedListener(listener)
	}
	/**
	  * Assigns a new listener to be informed in case this pointer stops from changing
	  * (i.e. isChanging becomes false).
	  * If this pointer had already stopped from changing, calls the listener immediately.
	  * @param listener A listener to assign, if appropriate (call-by-name)
	  */
	def addChangingStoppedListenerAndSimulateEvent(listener: => ChangingStoppedListener): Unit = {
		destiny match {
			// Case: Already stopped changing before => Informs the listener immediately
			case Sealed => listener.onChangingStopped()
			// Case: May stop changing in the future => Adds the listener
			case MaySeal => _addChangingStoppedListener(listener)
			// Case: Cannot stop changing => No point to attach a listener
			case ForeverFlux => ()
		}
	}
	/**
	  * Runs the specified function once/if this pointer stops from changing
	  * (i.e. isChanging becomes false).
	  * If this pointer had already stopped from changing, calls the function immediately.
	  * @param f A function to be performed, when/if appropriate
	  */
	def onceChangingStops[U](f: => U): Unit = addChangingStoppedListenerAndSimulateEvent(ChangingStoppedListener(f))
	/**
	  * Runs the specified function once/if this pointer stops from changing
	  * (i.e. when/if [[mayChange]] becomes false), but only if the changing stops at a specific value
	  * @param value Targeted value
	  * @param f Function to run once/if this item reaches the specified final value
	  * @tparam B Type of the targeted value
	  * @tparam U Arbitrary function result type
	  */
	def onceFixedAt[B >: A, U](value: B)(f: => U): Unit = onceChangingStops {
		if (this.value == value)
			f
	}
	
	/**
	  * Registers a new high-priority listener to be informed whenever this item's value changes.
	  * These dependencies must be informed first before triggering any normal listeners.
	  * @param dependency A dependency to add (call by name)
	  */
	@deprecated("Deprecated for removal. Change dependencies have been replaced with high-priority change listeners. Please use .addHighPriorityListener(ChangeListener) instead", "v2.2")
	def addDependency(dependency: => ChangeDependency[A]): Unit = addHighPriorityListener(dependency)
	/**
	  * Adds a new dependency to this changing item
	  * @param beforeChange A function called before each change event (accepts change event that will be fired)
	  * @param afterChange  A function called after each change event (accepts change event that was fired)
	  */
	@deprecated("Deprecated for removal. Please use .addHighPriorityListener(ChangeListener) instead", "v2.2")
	def addDependency[B](beforeChange: ChangeEvent[A] => B)(afterChange: B => Unit): Unit =
		addDependency(ChangeDependency.beforeAndAfter(beforeChange)(afterChange))
	/**
	  * Removes a change dependency from being activated in the future
	  * @param dependency A dependency to remove from this item
	  */
	@deprecated("Deprecated for removal. Change dependencies have been replaced with high-priority change listeners. Please use .removeListener(Any) instead", "v2.2")
	def removeDependency(dependency: Any): Unit = removeListener(dependency)
	
	/**
	  * Functions like [[addListenerAndSimulateEvent]],
	  * except that the value simulation is applied only if such value is defined.
	  *
	  * When the simulated value is not defined, functions like [[addListener]]
	  *
	  * @param simulatedOldValue A value to simulate for the purposes of generating an immediate change event.
	  *                          See [[addListenerAndSimulateEvent]] for more details.
	  *                          Set to None in cases where you don't want the initial event to be fired.
	  *
	  * @param listener          A listener to add to this pointer (call-by-name).
	  *                          May not be called in cases where this pointer won't change anymore (even via simulation).
	  *
	  * @tparam B Type of the simulated/listened value
	  */
	def addListenerAndPossiblySimulateEvent[B >: A](simulatedOldValue: Option[B])(listener: => ChangeListener[B]) =
		simulatedOldValue match {
			case Some(v) => addListenerAndSimulateEvent(v)(listener)
			case None => addListener(listener)
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
	  * @param listener          A listener function to call on change events
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
	  *                 change event itself.
	  *                 Returns whether future change events should also trigger this function,
	  *                 and whether any after effects should be performed.
	  */
	def addAnyChangeListener(onChange: => ChangeResponse) = addListener(ChangeListener.onAnyChange(onChange))
	
	/**
	  * Assigns a listener to this changing item, which is active only while the specified
	  * external condition is met
	  * @param condition A pointer that contains true while listening should occur
	  * @param priority The priority given to this listener/reaction in the origin pointer.
	  *                 Default = Last = standard priority.
	  * @param listener A change listener that should be assigned
	  */
	def addListenerWhile(condition: Changing[Boolean], priority: End = Last)(listener: => ChangeListener[A]) = {
		// Only assigns listeners to changing items, as they are useless in other situations
		if (mayChange) {
			// Case: Variable condition => Utilizes a conditional change reaction
			if (condition.mayChange)
				ConditionalChangeReaction(this, condition, priority)(listener)
			// Case: Always listening => Assigns a continuous listener
			else if (condition.value)
				addListenerOfPriority(priority)(listener)
			
			// In case where the listening condition never actuates, no listener-assignment is needed
		}
	}
	/**
	  * Assigns a listener to this changing item, which is active only while the specified
	  * external condition is met.
	  * May also generate an initial simulated change event for the listener, based on the specified "old" value.
	  * @param condition A pointer that contains true while listening (or simulated event-receiving) should occur
	  * @param simulatedOldValue Value used as the "old value" of this pointer for change event simulation.
	  *                          If this is equal to the current/new value of this pointer,
	  *                          no change event is simulated.
	  * @param priority The priority given to this listener/reaction in the origin pointer.
	  *                 Default = Last = standard priority.
	  * @param listener A listener to assign
	  * @tparam B Type of the listener and the simulated value
	  */
	def addListenerWhileAndSimulateEvent[B >: A](condition: Changing[Boolean], simulatedOldValue: => B,
	                                             priority: End = Last)
	                                            (listener: => ChangeListener[B]): Unit =
	{
		// Case: Variable condition
		if (condition.mayChange) {
			// Case: This item is still changing => Utilizes a conditional change reaction
			if (mayChange)
				ConditionalChangeReaction
					.simulatingInitialValue[B](this, condition, simulatedOldValue, priority)(listener)
			// Case: This item is fixed => Generates a simulated change event, if appropriate,
			// either immediately or when the condition allows it
			else {
				val oldValue = simulatedOldValue
				val currentValue = value
				if (currentValue != oldValue)
					condition.once(Identity) { _ => listener.onChangeEvent(ChangeEvent(oldValue, currentValue)) }
			}
		}
		// Case: Always listening => Assigns a normal, continuous, listener
		else if (condition.value)
			addListenerAndSimulateEvent(simulatedOldValue, isHighPriority = priority == First)(listener)
		
		// In cases where the condition never allows listening, no action is required
	}
	
	/**
	  * Calls the specified function when this item changes the next time
	  * @param f A function that will be called when this item changes
	  * @tparam U Arbitrary function result type
	  */
	def onNextChange[U](f: ChangeEvent[A] => U) = addListener(ChangeListener.once(f))
	/**
	  * @param condition A condition that must be fulfilled for the specified function to be called
	  * @param f         A function that is called once a change event satisfies the specified condition.
	  *                  Will only be called up to once.
	  * @tparam U Arbitrary function result type
	  */
	def onNextChangeWhere[U](condition: ChangeEvent[A] => Boolean)(f: ChangeEvent[A] => U) = addListener { e =>
		if (condition(e)) {
			f(e)
			Detach
		}
		else
			Continue
	}
	/**
	  * Calls the specified function once the value of this item satisfies the specified condition.
	  * If the current item satisfies the condition, the function is called immediately.
	  * @param condition A condition that the item in this pointer must satisfy
	  * @param f         A function that shall be called for the item that satisfies the specified condition.
	  *                  Will be called only up to once.
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
	  * @param f A mapping function that yields Some for the value that resolves the resulting future.
	  *          Allowed to throw.
	  * @tparam B Type of mapping result, when defined
	  * @return A future that completes once 'f' returns Some for a value in this changing item.
	  *         Resolves immediately if 'f' yields Some for the current value of this item.
	  *         Fails if the specified function 'f' throws.
	  */
	def findMapFuture[B](f: A => Option[B]) = _findMapFuture[B](f)
	/**
	  * @param f A mapping function that yields Some for the value that resolves the resulting future.
	  *          Allowed to throw.
	  * @tparam B Type of mapping result, when defined
	  * @return A future that completes once 'f' returns Some for a value in this changing item.
	  *         The current value in this item is not tested with 'f'.
	  *         Fails if the specified function 'f' throws.
	  */
	def findMapNextFuture[B](f: A => Option[B]) = _findMapFuture[B](f, disableImmediateTrigger = true)
	
	/**
	  * Creates a conditional view into this pointer
	  * @param condition A condition that must be met
	  *                  in order for the resulting pointer to reflect the value of this pointer.
	  *                  When this condition is not met, the resulting pointer will contain
	  *                  the last value of this pointer before the viewing stopped.
	  *
	  *                  Call-by-name. Not called if this item won't change anymore.
	  *
	  * @return A pointer that conditionally contains either the value of this pointer or a previous value,
	  *         if active viewing is not allowed.
	  */
	def viewWhile(condition: => Flag) = {
		if (mayChange) {
			val c = condition
			c.fixedValue match {
				// Case: Condition is always true or always false => No advanced listening is needed
				case Some(shouldView) => if (shouldView) this else Fixed(value)
				case None => OptimizedMirror.viewWhile(this, c)
			}
		}
		// Case: This pointer won't change anymore => Conditional listening makes no difference
		else
			this
	}
	
	/**
	  * @param f A mapping function
	  * @tparam B Mapping result type
	  * @return A mirrored version of this item, using specified mapping function
	  */
	def map[B](f: A => B): Changing[B] = diverge { OptimizedMirror(this)(f) } { f(value) }
	/**
	  * Creates a mirrored version of this changing item by assigning a listener to continuously update
	  * the mirror.
	  *
	  * This map method version is most appropriate for closely related pointers
	  * in a continuous (permanent) relationship.
	  * If the relationship is merely temporary, one should consider other mapping options such as map or lightMap.
	  *
	  * @param f A mapping function
	  * @tparam B Mapping result type
	  * @return A (strongly) mirrored version of this item, using specified mapping function
	  */
	def strongMap[B](f: A => B): Changing[B] = diverge { Mirror(this)(f) } { f(value) }
	/**
	  * Creates a mirrored version of this changing item by assigning a listener to update
	  * the mirror, but only while the specified condition holds true.
	  *
	  * This map method is suitable in situations where a clear stop condition may be placed,
	  * and the cost of listening to that condition is smaller than the cost of performing the mapping
	  * while that condition does not hold.
	  *
	  * @param condition Condition for mapping / viewing to take place.
	  *                  Call-by-name. Not called if this item won't change anymore.
	  * @param f A mapping function
	  * @tparam B Mapping result type
	  * @return A (strongly) mirrored version of this item, using specified mapping function
	  */
	def mapWhile[B](condition: => Flag)(f: A => B): Changing[B] = diverge {
		val conditionFlag: Flag = condition
		// Case: Mirroring is never actually allowed => Uses a fixed value instead
		if (conditionFlag.isAlwaysFalse)
			Fixed(f(value))
		// Case: Mirroring is allowed
		else
			OptimizedMirror(this, conditionFlag)(f)
	} { f(value) }
	/**
	  * Creates a mirrored view into this changing item.
	  * Values of this view are calculated on demand without caching, except when this mirror is being listened to.
	  *
	  * This map method version is most appropriate for situations where 'f' is very cheap to compute,
	  * and the relationship between these two pointers is merely temporary.
	  *
	  * @param f A mapping function
	  * @tparam B Mapping result type
	  * @return A (lightly) mirrored version of this item, using the specified mapping function
	  */
	def lightMap[B](f: A => B): Changing[B] =
		diverge { OptimizedMirror(this, disableCaching = true)(f) } { f(value) }
	/**
	  * Creates a new mapped view into this changing pointer.
	  * The mapping / viewing will be terminated once the specified condition is met.
	  * @param f A mapping function to apply
	  * @param stopCondition A condition that, once met, terminates the mapping / viewing between these pointers
	  * @tparam B Type of mapping results
	  * @return A new pointer
	  */
	def mapUntil[B](f: A => B)(stopCondition: B => Boolean) = {
		val initialMap = f(value)
		diverge {
			if (stopCondition(initialMap))
				Fixed(initialMap)
			else
				ChangingUntil.map(this)(f)(stopCondition)
		} { initialMap }
	}
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
	def lazyMap[B](f: A => B): ListenableLazy[B] =
		lazyDiverge { LazyMirror(this)(f) } { f(value) }
	
	/**
	  * @param f A mapping function
	  * @tparam B Mapping result type
	  * @return A pointer that contains mapped values merged with this pointer's "isChanging" status
	  */
	def mapWithState[B](f: A => B) = _mapWithState(f)
	/**
	  * A variant of [[mapWithState()]]. Use this version in case 'f' is a very simple/cheap function.
	  * @param f A mapping function
	  * @tparam B Mapping result type
	  * @return A pointer that contains mapped values merged with this pointer's "isChanging" status
	  */
	def lightMapWithState[B](f: A => B) =
		_mapWithState(f, disableCaching = true)
	/**
	  * @param f A mapping function
	  * @param stopCondition A condition that, if met, will end mirroring of this pointer and mark the resulting value
	  *                      as final.
	  *                      Accepts:
	  *                         1. The current value of this pointer (map input)
	  *                         1. Mapping function output for that value
	  * @tparam B Type of mapping result
	  * @return A new pointer that stops changing if the specified condition is met,
	  *         until which it maps the values of this pointer
	  */
	def mapWithStateUntil[B](f: A => B)(stopCondition: (A, B) => Boolean) = {
		val initialMap = f(value)
		diverge {
			if (stopCondition(value, initialMap))
				Fixed(ChangeResult.finalValue(initialMap))
			else
				StatefulValueView.mapAndStopIf(this)(f)(stopCondition)
		} { ChangeResult.finalValue(initialMap) }
	}
	
	/**
	  * @param f A function that returns true for the first value that should terminate the following changes
	  * @return A view to this changing item that may terminate the linking
	  */
	def viewUntil(f: A => Boolean) = {
		if (mayChange) {
			// Case: Immediately stops viewing => Wraps the current value in a fixed pointer
			if (f(value))
				Fixed(value)
			// Case: Stops viewing later
			else
				ChangingUntil(this)(f)
		}
		// Case: Won't change => No need to terminate viewing
		else
			this
	}
	/**
	  * @param f A function that returns true once the resulting pointer should stop mirroring values of this pointer
	  * @return A pointer that, while active, mirrors the values of this pointer, adding a "isChanging" status
	  *         to each value.
	  */
	def viewWithStateUntil(f: A => Boolean) =
		diverge {
			if (f(value))
				Fixed(ChangeResult.finalValue(value))
			else
				StatefulValueView.stopIf(this)(f)
		} { ChangeResult.finalValue(value) }
	
	/**
	  * Divides this changing item into 2
	  * @param initialLeft Initially assigned left side value. Called if this is right.
	  * @param initialRight Initially assigned right side value. Called if this is left.
	  * @param f A function which divides this item's value into either a left side, or a right side value.
	  * @tparam L Type of left side results
	  * @tparam R Type of right side results
	  * @return A mirror which provides access to the divided pointers
	  */
	def divide[L, R](initialLeft: => L, initialRight: => R)(f: A => Either[L, R]) =
		DividingMirror[A, L, R](this, initialLeft, initialRight)(f)
	
	/**
	  * Merges this item with another. Optimizes listening.
	  * @param other Another changing item
	  * @param f A merge function
	  * @tparam B Type of the other changing item
	  * @tparam R Type of merge result
	  * @return A mirror that merges the values from both of these items
	  */
	def mergeWith[B, R](other: Changing[B])(f: (A, B) => R): Changing[R] =
		divergeMerge[B, Changing[R]](other) {
			OptimizedMultiMergeMirror(this, other) { f(value, other.value) } } {
			v2 => map { f(_, v2) } } {
			OptimizedMirror(other) { v2 => f(value, v2) } }
	/**
	  * Merges this item with two others. Optimizes listening.
	  * @param first Another changing item
	  * @param second Yet another changing item
	  * @param merge A merge function
	  * @tparam B Type of the second changing item
	  * @tparam C Type of the third changing item
	  * @tparam R Type of merge result
	  * @return A mirror that merges the values from all three of these items
	  */
	def mergeWith[B, C, R](first: Changing[B], second: Changing[C])(merge: (A, B, C) => R): Changing[R] =
		mergeWith(Pair(first, second)) { merge(_, first.value, second.value) }
	/**
	  * Merges this item with 0-n others. Optimizes listening.
	  * @param others Other changing items
	  * @param mergeResult A function that accepts the value of this item and merges it with values from the other items
	  * @tparam R Merge result type
	  * @return A mirror that merges the values of all these items
	  */
	def mergeWith[R](others: IterableOnce[Changing[_]])(mergeResult: A => R): Changing[R] = {
		(Single(this) ++ others).filter { _.mayChange }.emptyOneOrMany match {
			case None => Fixed(mergeResult(value))
			case Some(Left(only)) => OptimizedMirror(only) { _ => mergeResult(value) }
			case Some(Right(many)) => OptimizedMultiMergeMirror(many) { mergeResult(value) }
		}
	}
	/**
	  * @param other Another changing item
	  * @param f A merge function
	  * @tparam B Type of the other changing item
	  * @tparam R Type of merge result
	  * @return A mirror that merges the values from both of these items
	  */
	def strongMergeWith[B, R](other: Changing[B])(f: (A, B) => R): Changing[R] =
		divergeMerge[B, Changing[R]](other) { MergeMirror(this, other)(f) } {
			v2 => strongMap { f(_, v2) } } {
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
	def strongMergeWith[B, C, R](first: Changing[B], second: Changing[C])(merge: (A, B, C) => R): Changing[R] =
		TripleMergeMirror.of(this, first, second)(merge)
	/**
	  * Creates a mirror that reflects the merged value of this and another pointer.
	  * However, this mirror is updated only while the specified condition allows it.
	  * @param other Another changing item
	  * @param condition Condition that must be met in order for the merging to occur.
	  * @param f     A merge function
	  * @tparam B Type of the other changing item
	  * @tparam R Type of merge result
	  * @return A mirror that merges the values from both of these items
	  */
	def mergeWithWhile[B, R](other: Changing[B], condition: Flag)(f: (A, B) => R): Changing[R] =
		mergeWithWhile(Single(other), condition) { f(_, other.value) }
	/**
	  * Creates a mirror that reflects the merged value of this and two other pointers.
	  * However, this mirror is updated only while the specified condition allows it.
	  * @param first  Another changing item
	  * @param second Yet another changing item
	  * @param condition Condition that must be met in order for the merging to occur.
	  * @param merge  A merge function
	  * @tparam B Type of the second changing item
	  * @tparam C Type of the third changing item
	  * @tparam R Type of merge result
	  * @return A mirror that merges the values from all three of these items
	  */
	def mergeWithWhile[B, C, R](first: Changing[B], second: Changing[C], condition: Flag)
	                           (merge: (A, B, C) => R): Changing[R] =
		mergeWithWhile(Pair(first, second), condition) { merge(_, first.value, second.value) }
	/**
	  * Creates a mirror that reflects the merged value of this and n other pointers.
	  * However, this mirror is updated only while the specified condition allows it.
	  * @param others Other mirrored items
	  * @param condition Condition that must be met in order for the merging to occur.
	  * @param mergeResult A function that accepts the value of this item and yields a merge result of all these items
	  * @tparam R Type of merge result
	  * @return A mirror that merges the values from all these items
	  */
	def mergeWithWhile[R](others: IterableOnce[Changing[_]], condition: Flag)(mergeResult: A => R): Changing[R] = {
		if (condition.isAlwaysFalse)
			Fixed(mergeResult(value))
		else
			(Single(this) ++ others).filter { _.mayChange }.emptyOneOrMany match {
				case None => Fixed(mergeResult(value))
				case Some(Left(only)) =>
					if (only == this)
						mapWhile(condition)(mergeResult)
					else
						OptimizedMirror(only, condition) { _ => mergeResult(value) }
				case Some(Right(many)) => OptimizedMultiMergeMirror(many, condition) { mergeResult(value) }
			}
	}
	/**
	  * Creates a mirror that reflects the merged value of this and another pointer.
	  * However, this mirror is updated only while the specified condition allows it.
	  * @param other Another changing item
	  * @param condition Condition that must be met in order for the merging to occur.
	  * @param f     A merge function
	  * @tparam B Type of the other changing item
	  * @tparam R Type of merge result
	  * @return A mirror that merges the values from both of these items
	  */
	def strongMergeWithWhile[B, R](other: Changing[B], condition: Flag)(f: (A, B) => R): Changing[R] =
	{
		if (condition.isAlwaysFalse)
			Fixed(f(value, other.value))
		else
			divergeMerge[B, Changing[R]](other) { MergeMirror(this, other, condition)(f) } { v2 =>
				mapWhile(condition) { f(_, v2) } } { Mirror(other, condition) { v2 => f(value, v2) } }
	}
	/**
	  * Creates a mirror that reflects the merged value of this and two other pointers.
	  * However, this mirror is updated only while the specified condition allows it.
	  * @param first  Another changing item
	  * @param second Yet another changing item
	  * @param condition Condition that must be met in order for the merging to occur.
	  * @param merge  A merge function
	  * @tparam B Type of the second changing item
	  * @tparam C Type of the third changing item
	  * @tparam R Type of merge result
	  * @return A mirror that merges the values from all three of these items
	  */
	def strongMergeWithWhile[B, C, R](first: Changing[B], second: Changing[C], condition: Flag)
	                           (merge: (A, B, C) => R): Changing[R] =
	{
		if (condition.isAlwaysFalse)
			Fixed(merge(value, first.value, second.value))
		else
			TripleMergeMirror.of(this, first, second, condition)(merge)
	}
	/**
	  * This merge function variant is appropriate for use-cases where 'f' is very light to compute,
	  * and the relationship between these pointers is temporary.
	  *
	  * @param other Another changing item
	  * @param f A merge function
	  * @tparam B Type of values in the other changing item
	  * @tparam R Type of merge results
	  * @return A mirror that merges the values from both of these items on demand
	  */
	def lightMergeWith[B, R](other: Changing[B])(f: (A, B) => R): Changing[R] =
		divergeMerge[B, Changing[R]](other) { LightMergeMirror(this, other)(f) } { v2 => lightMap { f(_, v2) } } {
			OptimizedMirror(other, disableCaching = true) { v2 => f(value, v2) } }
	/**
	  * Creates a pointer that merges the value of this pointer, plus the other pointer.
	  * The merging results are not cached. Therefore this function is not well applicable to costly merging functions.
	  * The merging process will be terminated altogether once the specified condition is met.
	  * @param other Another pointer
	  * @param merge A merge function that accepts the current values from both of these pointers
	  * @param stopCondition A condition that, once met, stops the resulting pointer from reflecting changes from
	  *                      these pointers.
	  *                      Accepts: 1) Current value of this pointer, 2) Current value of the other pointer, and
	  *                      3) Merge result
	  * @tparam B Type of the values in the other pointer
	  * @tparam R Type of merge results
	  * @return A new pointer
	  */
	def lightMergeWithUntil[B, R](other: Changing[B])(merge: (A, B) => R)(stopCondition: (A, B, R) => Boolean)
	                              =
	{
		if (mayChange) {
			if (other.mayChange) {
				val initialMerge = merge(value, other.value)
				if (stopCondition(value, other.value, initialMerge))
					Fixed(initialMerge)
				else
					LightMergeMirror.until(this, other)(merge)(stopCondition)
			}
			else {
				val otherValue = other.value
				mapUntil { v => merge(v, otherValue) } { stopCondition(value, otherValue, _) }
			}
		}
		else
			other.mapUntil { v2 => merge(value, v2) } { stopCondition(value, other.value, _) }
	}
	
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
	                              (incrementMerge: (R, A, B, Either[ChangeEvent[A], ChangeEvent[B]]) => R)
	                               =
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
	  * @param second A second changing item
	  * @param third A third changing item
	  * @param merge Merge function for the values of these items
	  * @tparam B Type of the second item values
	  * @tparam C Type of the third item values
	  * @tparam R Type of merge results
	  * @return A lazy view into the merge results from these pointers
	  */
	def lazyMergeWith[B, C, R](second: Changing[B], third: Changing[C])(merge: (A, B, C) => R) =
		LazyTripleMergeMirror.of(this, second, third)(merge)
	
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
	def flatMap[B](f: A => Changing[B]) =
		if (mayChange) FlatteningMirror(this)(f) else f(value)
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
	                         (incrementMap: (Changing[B], ChangeEvent[A]) => Changing[B])
	                          =
	{
		if (mayChange)
			FlatteningMirror.incremental(this)(initialMap)(incrementMap)
		else
			initialMap(value)
	}
	
	/**
	  * @param threshold A required pause between changes in this pointer before the view fires a change event
	  * @param viewCondition A condition that must be met in order for the delayed view to get updated at all
	  *                      (default = always view)
	  * @param exc Implicit execution context
	  * @return A view into this pointer that only fires change events when there is a long enough pause in
	  *         this pointer's changes
	  */
	def delayedBy(threshold: => Duration, viewCondition: Flag = AlwaysTrue)
	             (implicit exc: ExecutionContext): Changing[A] =
	{
		// Case: The view is not allowed to update => Returns a fixed value
		if (viewCondition.isAlwaysFalse)
			Fixed(value)
		// Case: This item may change
		else if (mayChange)
			threshold.finite match {
				// Case: A finite delay has been defined
				case Some(duration) =>
					// Case: Zero delay = View of this item
					if (duration <= Duration.Zero)
						this
					// Case: Positive delay => Creates a delayed view
					else
						DelayedView(this, duration, viewCondition)
				// Case: Infinite delay = Fixed value
				case None => Fixed(value)
			}
		// Case: This item doesn't change anymore => No need for delay
		else
			this
	}
	
	/**
	  * Creates an asynchronously mapping view of this changing item
	  * @param placeHolderResult Value placed in the view before the map result has been calculated
	  * @param skipInitialMap    Whether the initial mapping process (i.e. the mapping of this item's current value)
	  *                          should be skipped, and the placeholder be used instead.
	  *                          Suitable for situations where the placeholder is a proper mapping result.
	  *                          Default = false.
	  * @param f                 A synchronous mapping function
	  * @param merge             A function which accepts the previously held value and the new map result and
	  *                          produces a new pointer value.
	  * @param exc               Implicit execution context
	  * @tparam B Type of mapping result
	  * @tparam R Type of merged / reduced mapping results
	  * @return An asynchronously mapped view of this changing item
	  */
	def incrementalMapAsync[A2 >: A, B, R](placeHolderResult: R, skipInitialMap: Boolean = false)
	                                      (f: A2 => B)(merge: (R, B) => R)
	                                      (implicit exc: ExecutionContext) =
		AsyncProcessMirror.merging[A2, B, R](this, placeHolderResult, skipInitialMap)(f)(merge)
	/**
	  * Creates an asynchronously mapping view of this changing item
	  * @param placeHolderResult Value placed in the view before the map result has been calculated
	  * @param skipInitialMap    Whether the initial mapping process (i.e. the mapping of this item's current value)
	  *                          should be skipped, and the placeholder be used instead.
	  *                          Suitable for situations where the placeholder is a proper mapping result.
	  *                          Default = false.
	  * @param f                 A synchronous mapping function
	  * @param exc               Implicit execution context
	  * @tparam B Type of mapping result
	  * @return An asynchronously mapped view of this changing item
	  */
	def mapAsync[A2 >: A, B](placeHolderResult: B, skipInitialMap: Boolean = false)(f: A2 => B)
	                         (implicit exc: ExecutionContext) =
		incrementalMapAsync[A2, B, B](placeHolderResult, skipInitialMap)(f) { (_, res) => res }
	/**
	  * Creates an asynchronously mapping view of this changing item
	  * @param placeHolderResult Value placed in the view before the map result has been calculated
	  * @param mapCondition A condition that must be met in order for the mapping process to initiate.
	  *                     Default = always map.
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
	def incrementalMapToFuture[A2 >: A, B, R](placeHolderResult: R, mapCondition: Changing[Boolean] = AlwaysTrue,
	                                          skipInitialMap: Boolean = false)
	                                         (f: A2 => Future[B])(merge: (R, Try[B]) => R)
	                                         (implicit exc: ExecutionContext) =
		AsyncMirror[A2, B, R](this, placeHolderResult, mapCondition, skipInitialProcess = skipInitialMap)(f)(merge)
	/**
	  * Creates an asynchronously mapping view of this changing item
	  * @param placeHolderResult Value placed in the view before the map result has been calculated
	  * @param mapCondition A condition that must be met in order for the mapping process to initiate.
	  *                     Default = always map.
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
	def incrementalMapToTryFuture[B](placeHolderResult: B, mapCondition: Changing[Boolean] = AlwaysTrue,
	                                 skipInitialMap: Boolean = false)
	                                (f: A => Future[Try[B]])(merge: (B, Try[B]) => B)
	                                (implicit exc: ExecutionContext) =
		incrementalMapToFuture(placeHolderResult, mapCondition, skipInitialMap)(f) { (previous, result) =>
			merge(previous, result.flatten) }
	/**
	  * Creates an asynchronously mapping view of this changing item.
	  * In cases where the asynchronous mapping fails, errors are simply logged and treated as if no
	  * mapping was even requested / as if the value of this pointer didn't change.
	  * @param placeHolderResult Value placed in the view before the map result has been calculated
	  * @param mapCondition A condition that must be met in order for the mapping process to initiate.
	  *                     Default = always map.
	  * @param skipInitialMap Whether the initial mapping process (i.e. the mapping of this item's current value)
	  *                       should be skipped, and the placeholder be used instead.
	  *                       Suitable for situations where the placeholder is a proper mapping result.
	  *                       Default = false.
	  * @param f An asynchronous mapping function
	  * @param exc Implicit execution context
	  * @tparam B Type of mapping result
	  * @return An asynchronously mapped view of this changing item
	  */
	def mapToFuture[B](placeHolderResult: B, mapCondition: Changing[Boolean] = AlwaysTrue,
	                   skipInitialMap: Boolean = false)(f: A => Future[B])
	                  (implicit exc: ExecutionContext) =
		AsyncMirror.catching(this, placeHolderResult, mapCondition, skipInitialProcess = skipInitialMap)(f)
	/**
	  * Creates an asynchronously mapping view of this changing item.
	  * In cases where the asynchronous mapping fails, errors are simply logged and treated as if no
	  * mapping was even requested / as if the value of this pointer didn't change.
	  * @param placeHolderResult Value placed in the view before the map result has been calculated
	  * @param mapCondition A condition that must be met in order for the mapping process to initiate.
	  *                     Default = always map.
	  * @param skipInitialMap Whether the initial mapping process (i.e. the mapping of this item's current value)
	  *                       should be skipped, and the placeholder be used instead.
	  *                       Suitable for situations where the placeholder is a proper mapping result.
	  *                       Default = false.
	  * @param f An asynchronous mapping function that may fail
	  * @param exc Implicit execution context
	  * @tparam B Type of mapping result
	  * @return An asynchronously mapped view of this changing item
	  */
	def mapToTryFuture[B](placeHolderResult: B, mapCondition: Changing[Boolean] = AlwaysTrue,
	                      skipInitialMap: Boolean = false)(f: A => Future[Try[B]])
	                     (implicit exc: ExecutionContext) =
		AsyncMirror.tryCatching(this, placeHolderResult, mapCondition, skipInitialProcess = skipInitialMap)(f)
	
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
			Continue
	}
	
	/**
	  * Creates a new changing item based on this item,
	  * returning a fixed pointer in case this item is no longer changing
	  * @param ifChanging    Result to give if this item is still changing (call-by-name)
	  * @param ifNotChanging A fixed value to return if this item is no longer changing (call-by-name)
	  * @tparam B Type of new changing or fixed value
	  * @return A new changing item
	  */
	protected def diverge[B](ifChanging: => Changing[B])(ifNotChanging: => B) =
		if (mayChange) ifChanging else Fixed(ifNotChanging)
	/**
	  * Creates a new lazy container based on this item.
	  * A more simple container is created if this item is no longer changing.
	  * @param ifChanging Result (lazy container) to give if this item is still changing (call-by-name)
	  * @param ifNotChanging A fixed value to return if this item is no longer changing (call-by-name, lazy)
	  * @tparam B Type of new changing or fixed value
	  * @return A new lazy container
	  */
	protected def lazyDiverge[B](ifChanging: => ListenableLazy[B])(ifNotChanging: => B) =
		if (mayChange) ifChanging else Lazy.listenable(ifNotChanging)
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
		if (other.mayChange) {
			if (mayChange) ifBothChange else ifOnlyThisIsFixed
		}
		else
			ifOtherIsFixed(other.value)
	}
	
	/**
	  * Informs all listeners about a possible change event.
	  * Updates the list of active listeners accordingly.
	  *
	  * Won't generate any event or action in cases where the old and the new value would resolve in the same value.
	  *
	  * @param oldValue The previous value of this changing item
	  *                 (call-by-name, called only if there are listeners assigned to this item)
	  * @param currentValue Current value of this changing item
	  *                     (call-by-name, called only if there are listeners assigned to this item)
	  * @param acquireListeners A function that acquires the listeners of this item, based on their priority
	  * @param detachListeners  A function that removes a set of listeners from this item.
	  *                         Accepts:
	  *                         1) Priority to target, and
	  *                         2) Listeners to remove (never empty)
	  * @tparam B Type of events and listeners applied here
	  * @return The effects to trigger afterwards
	  */
	protected def fireEventIfNecessary[B >: A](oldValue: => B, currentValue: => B)
	                                          (acquireListeners: End => Iterable[ChangeListener[B]])
	                                          (detachListeners: (End, Iterable[ChangeListener[B]]) => Unit)
	                                           =
	{
		// Calculates the event lazily
		// In case where the current and old value are equal, won't generate an event
		val eventView = Lazy {
			val o = oldValue
			val n = currentValue
			if (o == n)
				None
			else
				Some(ChangeEvent(o, n))
		}
		fireEvent[B](eventView)(acquireListeners)(detachListeners)
	}
	/**
	  * Informs all listeners about a possible change event.
	  * Updates the list of active listeners accordingly.
	  * @param lazyEvent A (lazily initialized) pointer/view to the change event that occurred.
	  *                  Contains None in case there was no change after all.
	  *                  Won't be viewed in cases where there are no listeners assigned to this item.
	  * @param acquireListeners A function that acquires the listeners of this item, based on their priority
	  * @param detachListeners A function that removes a set of listeners from this item.
	  *                        Accepts:
	  *                             1) Priority to target, and
	  *                             2) Listeners to remove (never empty)
	  * @tparam B Type of events and listeners applied here
	  * @return The effects to trigger afterwards
	  */
	protected def fireEvent[B >: A](lazyEvent: View[Option[ChangeEvent[B]]])
	                               (acquireListeners: End => Iterable[ChangeListener[B]])
	                               (detachListeners: (End, Iterable[ChangeListener[B]]) => Unit)
	                                =
	{
		// Informs the listeners in order or priority
		End.values.flatMap { priority =>
			val responses = fireEventFor(acquireListeners(priority), lazyEvent.value)
			// Immediately detaches the listeners that are no longer needed
			val listenersToRemove = responses
				.flatMap { case (l, response) => if (response.shouldDetach) Some(l) else None }
			if (listenersToRemove.nonEmpty)
				detachListeners(priority, listenersToRemove)
			// Returns the scheduled after-effects
			responses.flatMap { _._2.afterEffects }
		}
	}
	/**
	  * Informs a specific set of change listeners of a new change event.
	  * Collects the actions to perform, based on their responses.
	  * @param listeners listeners to inform of a new change
	  * @param event A view to the generated change event (call-by-name, not called if there are no listeners).
	  *              Contains None if no change occurred after all.
	  * @tparam B Type of change events and listeners applied
	  * @return The specified listeners, coupled with their responses to the change events
	  */
	protected def fireEventFor[B >: A](listeners: Iterable[ChangeListener[B]], event: => Option[ChangeEvent[B]]) = {
		// Case: No listeners => No events required
		if (listeners.isEmpty)
			Empty
		// Case: Listeners present => Informs them and collects the after effects to trigger later
		//       (may schedule some listeners to be removed, based on their change responses)
		else
			event match {
				// Case: Event is real => Relays if to the listeners
				case Some(event) =>
					listeners.flatMap { listener =>
						// Catches and logs failures, in case the listener throws
						Try { listener.onChangeEvent(event) }
							.logWithMessage(s"Failure while processing $event")
							.map { listener -> _ }
					}
				// Case: There wasn't a change event after all => Skips the process
				case None => Empty
			}
	}
	
	/**
	  * A default implementation of the 'futureWhere' function
	  * @param condition Condition to search for values with. Allowed to throw.
	  * @return A value future where the specified condition returns true (may never complete)
	  */
	protected def _futureWhere(condition: A => Boolean, disableImmediateTrigger: Boolean = false) =
		_findMapFuture[A](a => Some(a).filter(condition), disableImmediateTrigger)
	/**
	  * A default implementation of the 'futureWhere' function
	  * @param condition Condition to search for values with. Allowed to throw. Returns Some for the result to return.
	  * @return A value future where the specified condition returns true (may never complete)
	  */
	protected def _findMapFuture[B](condition: A => Option[B], disableImmediateTrigger: Boolean = false) = {
		// Tests with the current value first, unless disabled
		val initialCandidate = if (disableImmediateTrigger) None else Some(value)
		initialCandidate.flatMap { c =>
			// Handles possible errors thrown by the test function
			Try { condition(c) } match {
				case Success(result) => result.map(Success.apply)
				// Case: Testing failed => Immediately returns as a failure
				case Failure(error) => Some(Failure(error))
			}
		} match {
			// Case: Completes with the current value
			case Some(result) => Future.fromTry(result)
			case None =>
				// Case: May change => Listens to changes until the searched state is found
				if (mayChange) {
					val promise = Promise[B]()
					addListener { e =>
						// Handles possible failures thrown by the test function
						Try { condition(e.newValue) } match {
							case Success(result) =>
								result match {
									// Case: Result found => Completes the future
									case Some(result) =>
										promise.trySuccess(result)
										Detach
									// Case: No result found => Waits for the next change event
									case None => Continue
								}
							// Case: Test function threw => Propagates the failure to the resulting Future
							case Failure(error) =>
								promise.tryFailure(error)
								Detach
						}
					}
					promise.future
				}
				// Case: Impossible to succeed
				else
					Future.never
		}
	}
	
	private def _mapWithState[B](f: A => B, disableCaching: Boolean = false) =
		destiny match {
			case Sealed => Fixed(ChangeResult.finalValue(f(value)))
			case MaySeal => StatefulValueView.map(this, cachingDisabled = disableCaching)(f)
			case ForeverFlux => map { v => ChangeResult.temporal(f(v)) }
		}
}

