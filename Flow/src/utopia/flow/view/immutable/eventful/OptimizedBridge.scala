package utopia.flow.view.immutable.eventful

import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.{ChangeEvent, ChangeResponse}
import utopia.flow.operator.Identity
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.template.eventful.Changing

object OptimizedBridge
{
	/**
	  * Creates a new bridge that transforms origin pointer values before relaying them
	  * @param origin            A pointer to follow, when appropriate
	  * @param trackActivelyFlag A flag that contains true while the origin pointer should be continuously tracked
	  * @param f                 A function for transforming the origin pointer value
	  * @param onUpdate          A function called whenever the origin pointer value changes during tracking or cache-reset.
	  *                          The specified change event is lazily generated, and will contain None in case there was no
	  *                          actual change in the transformed value.
	  *                          Returns after-effects to trigger once the origin pointer has resolved informing its listeners about
	  *                          the change.
	  * @param disableCaching   Whether mapped values should not be cached (unless strictly necessary)
	  *                          but calculated whenever a value is requested.
	  *
	  *                          The benefits of disabling caching is that less listeners will be attached to the
	  *                          origin pointer, which may reduce resource use for optimized pointers.
	  *                          The disadvantage is that 'f' will likely be called more frequently.
	  *
	  *                          Setting this to true is recommended in cases where 'f' is very cheap to compute
	  *                          (e.g. retrieving a value from a map or something).
	  * @tparam O Type of origin pointer values
	  * @tparam R Type of transformed values
	  * @return A new bridge
	  */
	def map[O, R](origin: Changing[O], trackActivelyFlag: Changing[Boolean], disableCaching: Boolean = false)
	             (f: O => R)(onUpdate: Lazy[Option[ChangeEvent[R]]] => Seq[() => Unit]) =
		new OptimizedBridge[O, R](origin, trackActivelyFlag, f, onUpdate, disableCaching)
	
	/**
	  * Creates a new bridge that relays origin pointer values as they appear
	  * @param origin            A pointer to follow, when appropriate
	  * @param trackActivelyFlag A flag that contains true while the origin pointer should be continuously tracked
	  * @param onUpdate          A function called whenever the origin pointer value changes during tracking or cache-reset.
	  *                          The specified change event is lazily generated, and will contain None in case there was no
	  *                          actual change in the transformed value (which is never the case in this approach).
	  *                          Returns after-effects to trigger once the origin pointer has resolved informing its listeners about
	  *                          the change.
	  * @tparam A Type of origin pointer values
	  * @return A new bridge
	  */
	def apply[A](origin: Changing[A], trackActivelyFlag: Changing[Boolean])
	            (onUpdate: Lazy[Option[ChangeEvent[A]]] => Seq[() => Unit]) =
		map[A, A](origin, trackActivelyFlag, disableCaching = true)(Identity)(onUpdate)
}

/**
  * A view into a pointer that manages updates lazily and listens the target (i.e. origin)
  * pointer only while it is needed.
  *
  * These bridges may be used in situations where keeping listeners attached is a bad idea
  * (e.g. when listeners are temporary and generated in abundance).
  *
  * @author Mikko Hilpinen
  * @since 24.7.2023, v2.2
  *
  * @tparam O Type of origin pointer values
  * @tparam R Type of transformed values
  *
  * @constructor Creates a new "bridge" for the specified origin pointer
  * @param origin A pointer to follow, when appropriate
  * @param trackActivelyFlag A flag that contains true while the origin pointer should be continuously tracked
  * @param f A function for transforming the origin pointer value
  * @param onUpdate A function called whenever the origin pointer value changes during tracking or cache-reset.
  *                 The specified change event is lazily generated, and will contain None in case there was no
  *                 actual change in the transformed value.
  *                 Returns after-effects to trigger once the origin pointer has resolved informing its listeners about
  *                 the change.
  * @param cachingDisabled Whether mapped values should not be cached (unless strictly necessary)
  *                        but calculated whenever a value is requested.
  *
  *                        The benefits of disabling caching is that less listeners will be attached to the
  *                        origin pointer, which may reduce resource use for optimized pointers.
  *                        The disadvantage is that 'f' will likely be called more frequently.
  *
  *                        Setting this to true is recommended in cases where 'f' is very cheap to compute
  *                        (e.g. retrieving a value from a map or something).
  */
class OptimizedBridge[-O, R](origin: Changing[O], trackActivelyFlag: Changing[Boolean], f: O => R,
                             onUpdate: Lazy[Option[ChangeEvent[R]]] => Seq[() => Unit], cachingDisabled: Boolean)
	extends View[R]
{
	// ATTRIBUTES   -------------------------
	
	// Set to true once this bridge is no longer allowed to track the origin
	private var terminated = false
	
	// Caches pre-calculated values on demand
	private var cachedValue: Option[R] = None
	
	private val originListener = ChangeListener[O] { event =>
		// Prepares the change event
		val oldCachedValue = cachedValue
		lazy val newValue = f(event.newValue)
		val secondaryEvent = Lazy {
			val oldValue = oldCachedValue.getOrElse { f(event.oldValue) }
			if (oldValue != newValue)
				Some(ChangeEvent(oldValue, newValue))
			else
				None
		}
		val afterEffects = {
			// Case: This pointer contains listeners =>
			// Keeps the cache up-to-date and keeps this listener attached.
			if (trackActivelyFlag.value) {
				// Updates the cache
				cachedValue = Some(newValue)
				// Informs the listener about this update
				val afterEffects = onUpdate(secondaryEvent)
				
				// Triggers the after-effects only after all other listeners have been informed as well
				// TODO: Consider whether this is the optimal approach
				// Case: Caching is disabled and all listeners detached themselves =>
				//       Clears the cache and detaches this listener immediately
				if (cachingDisabled && !trackActivelyFlag.value)
					cachedValue = None
				afterEffects
			}
			// Case: This pointer doesn't contain listeners =>
			// Detaches as early as possible, and only invalidates the cache on changes
			else {
				cachedValue = None
				onUpdate(secondaryEvent)
			}
		}
		DetachIfAppropriate(afterEffects)
	}
	private val activeTrackingListener = ChangeListener.continuous { e: ChangeEvent[Boolean] =>
		// Case: Should start tracking actively => Attaches itself to origin
		if (e.newValue)
			origin.addHighPriorityListener(originListener)
		// Case: Should stop tracking actively => Clears the cache and/or detaches from origin
		else {
			// May forcibly clear the cache
			if (cachingDisabled)
				cachedValue = None
			// If no cache-reset is required, detaches from origin
			if (cachedValue.isEmpty)
				origin.removeListener(originListener)
		}
		
		// If the origin stops changing, won't need to track the listening status anymore
		ChangeResponse.continueIf(origin.mayChange)
	}
	
	
	// INITIAL CODE -------------------------
	
	// Whenever listeners are assigned to this mirror, starts following the origin pointer more carefully.
	trackActivelyFlag.addListener(activeTrackingListener)
	
	
	// COMPUTED ----------------------------
	
	// Contains true while this bridge should remain attached to the origin pointer
	// This is true when cache needs to be cleared and/or when actively tracking the origin
	private def shouldListen = cachedValue.isDefined || trackActivelyFlag.value
	
	
	// IMPLEMENTED  ------------------------
	
	/**
	  * @return The current (transformed) value of the origin pointer.
	  */
	// Returns the cached value, if one is available
	override def value: R = cachedValue.getOrElse {
		// Case: Needs to calculate a new value
		val currentValue = f(origin.value)
		
		// Case: Mapping has terminated => Caches the value and won't schedule more updates
		if (terminated)
			cachedValue = Some(currentValue)
		// Case: Caching is enabled => Keeps the value cached
		// and assigns a listener in order to invalidate the cache once the origin pointer changes
		else if (!cachingDisabled) {
			cachedValue = Some(currentValue)
			// TODO: It may be better to check whether the origin already contains this listener
			origin.addHighPriorityListener(originListener)
		}
		
		currentValue
	}
	
	
	// OTHER    ----------------------------
	
	/**
	  * Terminates this bridge so that the origin value will no longer be tracked.
	  * Stores the origin's current value and will only continue to return that value.
	  */
	def detach() = {
		if (!terminated) {
			trackActivelyFlag.removeListener(activeTrackingListener)
			origin.removeListener(originListener)
			if (cachedValue.isEmpty)
				cachedValue = Some(f(origin.value))
			terminated = true
		}
	}
	
	
	// NESTED   ----------------------------
	
	private object DetachIfAppropriate extends ChangeResponse
	{
		// IMPLEMENTED  --------------------
		
		override def shouldContinueListening: Boolean = shouldListen
		override def afterEffects: Iterable[() => Unit] = Iterable.empty
		
		override def and[U](afterEffect: => U): ChangeResponse = new DetachIfAppropriate(Vector(() => afterEffect))
		
		
		// OTHER    ------------------------
		
		def apply(afterEffects: Seq[() => Unit]) =
			if (afterEffects.isEmpty) this else new DetachIfAppropriate(afterEffects)
	}
	
	private class DetachIfAppropriate(override val afterEffects: Seq[() => Unit]) extends ChangeResponse
	{
		override def shouldContinueListening: Boolean = shouldListen
		
		override def and[U](afterEffect: => U): ChangeResponse =
			new DetachIfAppropriate(afterEffects :+ { () => afterEffect })
	}
}
