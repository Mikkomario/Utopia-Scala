package utopia.flow.view.immutable.eventful

import utopia.flow.event.model.Destiny
import utopia.flow.view.template.eventful.{Changing, OptimizedChanging}

object OptimizedMirror
{
	/**
	  * Creates a new mirror that reflects the value of a pointer
	  * @param origin Origin pointer that is being reflected
	  * @param mirrorCondition A condition that must be met for the mirroring to continue / be active.
	  *                        Default = always keep the mirroring active.
	  * @param disableCaching Whether mapped values should not be cached (unless strictly necessary)
	  *                       but calculated whenever a value is requested.
	  *
	  *                       The benefits of disabling caching is that less listeners will be attached to the
	  *                       origin pointer, which may reduce resource use for optimized pointers.
	  *                       The disadvantage is that 'f' will likely be called more frequently.
	  *
	  *                       Setting this to true is recommended in cases where 'f' is very cheap to compute
	  *                       (e.g. retrieving a value from a map or something).
	  *
	  * @param f A function that accepts a value from
	  *          the origin pointer and converts it to a value to return via this pointer.
	  * @tparam O Type of origin pointer values
	  * @tparam R Type of mapping results
	  * @return A new pointer that returns mapped origin values
	  */
	def apply[O, R](origin: Changing[O], mirrorCondition: Changing[Boolean] = AlwaysTrue,
	                disableCaching: Boolean = false)
	               (f: O => R) =
		new OptimizedMirror[O, R](origin, f, mirrorCondition, cachingDisabled = disableCaching)
}

/**
  * A pointer that reflects mapped value of another pointer.
  * This implementation is optimized, so that pointer-listening is minimized.
  * This mirror implementation is more suitable in use-cases where dependencies are temporary,
  * as these instances won't remain as unnecessary change listeners once they're no longer used
  * (provided that their listeners are handled in a similar manner and detached once no longer used)
  * @author Mikko Hilpinen
  * @since 24.7.2023, v2.2
  */
class OptimizedMirror[O, R](origin: Changing[O], f: O => R, condition: Changing[Boolean] = AlwaysTrue,
                            cachingDisabled: Boolean = false)
	extends OptimizedChanging[R]
{
	// ATTRIBUTES   -------------------------
	
	private val bridge = OptimizedBridge.map(origin, hasListenersFlag && condition, cachingDisabled)(f)(fireEvent)
	
	
	// INITIAL CODE -------------------------
	
	onceSourceStops(origin) {
		declareChangingStopped()
		bridge.detach()
	}
	onceSourceStopsAt(condition, false) {
		declareChangingStopped()
		bridge.detach()
	}
	
	
	// IMPLEMENTED  -------------------------
	
	override def value: R = bridge.value
	override def destiny: Destiny = origin.destiny
	
	override def readOnly = this
}
