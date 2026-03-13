package utopia.flow.view.immutable.eventful

import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponse.Continue
import utopia.flow.event.model.{AfterEffect, ChangeEvent}
import utopia.flow.operator.Identity
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.template.eventful.Changing

object OptimizedBridge
{
	/**
	  * Creates a new bridge that transforms origin pointer values before relaying them
	  * @param origin            A pointer to follow, when appropriate
	  * @param trackActivelyFlag A flag that contains true while the origin pointer should be continuously tracked
	  * @param f                 A function for transforming the origin pointer value
	  * @param onUpdate A function called whenever the origin pointer value changes during tracking or cache-reset.
	 *                  Receives either:
	 *                      - Right: A generated change event, if 'trackActivelyFlag' is set to true.
	 *                      - Left: Lazily initialized old & new mapped values,
	 *                              if 'trackActivelyFlag' is set false and only a cache-clearing was performed.
	 *
	 *                  Returns after-effects to trigger, which should include the informing of possible listeners
	 *                  (in case of Right input).
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
	             (f: O => R)(onUpdate: Either[Pair[Lazy[R]], ChangeEvent[R]] => IterableOnce[AfterEffect]) =
		new OptimizedBridge[O, R](origin, trackActivelyFlag, f, onUpdate, disableCaching)
	
	/**
	  * Creates a new bridge that relays origin pointer values as they appear
	  * @param origin            A pointer to follow, when appropriate
	  * @param trackActivelyFlag A flag that contains true while the origin pointer should be continuously tracked
	  * @param onUpdate A function called whenever the origin pointer value changes during tracking or cache-reset.
	 *                  Receives either:
	 *                      - Right: A generated change event, if 'trackActivelyFlag' is set to true.
	 *                      - Left: Lazily initialized old & new values,
	 *                              if 'trackActivelyFlag' is set false and only a cache-clearing was performed.
	 *
	 *                  Returns after-effects to trigger, which should include the informing of possible listeners
	 *                  (in case of Right input).
	  * @tparam A Type of origin pointer values
	  * @return A new bridge
	  */
	def apply[A](origin: Changing[A], trackActivelyFlag: Changing[Boolean])
	            (onUpdate: Either[Pair[Lazy[A]], ChangeEvent[A]] => IterableOnce[AfterEffect]) =
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
  *                 Receives either:
 *                      - Right: A generated change event, if 'trackActivelyFlag' is set to true.
 *                      - Left: Lazily initialized old & new mapped values,
 *                              if 'trackActivelyFlag' is set false and only a cache-clearing was performed.
  *
 *                 Returns after-effects to trigger, which should include the informing of possible listeners
 *                 (in case of Right input).
  *
 * @param cachingDisabled Whether mapped values should not be cached (unless strictly necessary)
  *                        but calculated whenever a value is requested.
  *
  *                        The benefits of disabling caching is that fewer listeners will be attached to the
  *                        origin pointer, which may reduce resource use for optimized pointers.
  *                        The disadvantage is that 'f' will likely be called more frequently.
  *
  *                        Setting this to true is recommended in cases where 'f' is very cheap to compute
  *                        (e.g. retrieving a value from a map or something).
  */
class OptimizedBridge[-O, R](origin: Changing[O], trackActivelyFlag: Changing[Boolean], f: O => R,
                             onUpdate: Either[Pair[Lazy[R]], ChangeEvent[R]] => IterableOnce[AfterEffect],
                             cachingDisabled: Boolean)
	extends View[R]
{
	// ATTRIBUTES   -------------------------
	
	/**
	  * Set to true once this bridge is no longer allowed to track the origin
	  */
	private var terminated = false
	/**
	 * Caches a calculated result
	 */
	private val cache = Volatile.empty[R]
	/**
	 * An after-effect for clearing the cache, if necessary
	 */
	private val clearCacheLast = AfterEffect {
		if (!trackActivelyFlag.value)
			cache.clear()
		()
	}
	
	private val originListener = ChangeListener[O] { event =>
		// Prepares to call onUpdate(...) with either a change event (Right), or just a status update (Left)
		val updateResult = {
			// Case: Tracking actively => Updates the cache & prepares a change event
			if (trackActivelyFlag.value) {
				val newValue = f(event.newValue)
				val oldValue = cache.getAndSet(Some(newValue)).getOrElse { f(event.oldValue) }
				
				if (newValue == oldValue)
					Empty
				else
					onUpdate(Right(ChangeEvent(oldValue, newValue)))
			}
			// Case: Not actively tracking => Resets the cache and informs the onUpdate
			else {
				// The old and the new value are calculated lazily
				val lazyOldValue = cache.pop() match {
					case Some(oldValue) => Lazy.initialized(oldValue)
					case None => Lazy { f(event.oldValue) }
				}
				onUpdate(Left(Pair(lazyOldValue, Lazy { f(event.newValue) })))
			}
		}
		// Informs the listener about this update
		// Also, clears the cache after the other effects have resolved, unless still tracking the origin
		Continue.andAll(updateResult.iterator ++ Single(clearCacheLast)).onlyIf(trackActivelyFlag)
	}
	private val activeTrackingListener = ChangeListener { e: ChangeEvent[Boolean] =>
		// Case: Should start tracking actively => Attaches itself to origin
		if (e.newValue)
			origin.addHighPriorityListener(originListener)
		// Case: Should stop tracking actively => Clears the cache and/or detaches from origin
		else {
			val shouldRemoveOriginListener = {
				// Case: Further caching is not allowed => Clears cache & detaches the origin listener immediately
				if (cachingDisabled) {
					cache.clear()
					true
				}
				// Case: Caching may be continued until invalidated
				//       => Continues listening to the origin, if the cache still needs to be cleared
				else
					cache.isEmpty
			}
			if (shouldRemoveOriginListener)
				origin.removeListener(originListener)
		}
		
		// If the origin stops changing, won't need to track the listening status anymore
		Continue.onlyIf(origin.mayChange)
	}
	
	
	// INITIAL CODE -------------------------
	
	// Whenever listeners are assigned to this mirror, starts following the origin pointer more carefully.
	trackActivelyFlag.addHighPriorityListener(activeTrackingListener)
	
	
	// IMPLEMENTED  ------------------------
	
	/**
	  * @return The current (transformed) value of the origin pointer.
	  */
	// Returns the cached value, if one is available
	override def value: R = cache.mutate {
		// Case: A value has been cached => Yields the cached value
		case cache @ Some(cached) => cached -> cache
		// Case: No value has been cached => Calculates a new value
		case None =>
			val value = f(origin.value)
			// Case: Already stopped, or already tracking the origin pointer => Caches the generated value
			if (terminated || trackActivelyFlag.value)
				value -> Some(value)
			// Case: Caching is not allowed => Won't cache the new value
			else if (cachingDisabled)
				value -> None
			// Case: Caching is allowed => Caches the value and registers the origin listener for cache-reset
			else {
				origin.addHighPriorityListener(originListener)
				value -> Some(value)
			}
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
			cache.setOneIfEmpty { f(origin.value) }
			terminated = true
		}
	}
}
