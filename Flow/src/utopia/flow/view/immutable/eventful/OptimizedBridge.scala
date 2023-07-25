package utopia.flow.view.immutable.eventful

import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeEvent
import utopia.flow.event.model.ChangeResponse.{Continue, ContinueAnd, Detach, DetachAnd}
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
	             (f: O => R)(onUpdate: Lazy[Option[ChangeEvent[R]]] => Iterable[() => Unit]) =
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
	            (onUpdate: Lazy[Option[ChangeEvent[A]]] => Iterable[() => Unit]) =
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
                             onUpdate: Lazy[Option[ChangeEvent[R]]] => Iterable[() => Unit], cachingDisabled: Boolean)
	extends View[R]
{
	// ATTRIBUTES   -------------------------
	
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
			if (cachingDisabled && !trackActivelyFlag.value) {
				cachedValue = None
				if (afterEffects.isEmpty) Detach else DetachAnd(afterEffects)
			}
			else if (afterEffects.isEmpty)
				Continue
			else
				ContinueAnd(afterEffects)
		}
		// Case: This pointer doesn't contain listeners =>
		// Detaches as early as possible, and only invalidates the cache on changes
		else {
			cachedValue = None
			val afterEffects = onUpdate(secondaryEvent)
			if (afterEffects.isEmpty) Detach else DetachAnd(afterEffects)
		}
	}
	
	
	// INITIAL CODE -------------------------
	
	// Whenever listeners are assigned to this mirror, starts following the origin pointer more carefully.
	trackActivelyFlag.addContinuousListener { e =>
		if (e.newValue)
			origin.addHighPriorityListener(originListener)
	}
	
	
	// IMPLEMENTED  ------------------------
	
	/**
	  * @return The current (transformed) value of the origin pointer.
	  */
	// Returns the cached value, if one is available
	override def value: R = cachedValue.getOrElse {
		// Case: Needs to calculate a new value
		val currentValue = f(origin.value)
		
		// Case: Caching is enabled => Keeps the value cached
		// and assigns a listener in order to invalidate the cache once the origin pointer changes
		if (!cachingDisabled) {
			cachedValue = Some(currentValue)
			origin.addHighPriorityListener(originListener)
		}
		
		currentValue
	}
}
