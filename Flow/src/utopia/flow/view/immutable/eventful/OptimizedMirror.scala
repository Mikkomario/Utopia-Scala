package utopia.flow.view.immutable.eventful

import utopia.flow.event.model.{ChangeResponse, Destiny}
import utopia.flow.operator.Identity
import utopia.flow.util.logging.Logger
import utopia.flow.view.template.eventful.{Changing, FlagLike, OptimizedChanging}

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
	def apply[O, R](origin: Changing[O], mirrorCondition: FlagLike = AlwaysTrue,
	                disableCaching: Boolean = false)
	               (f: O => R) =
		new OptimizedMirror[O, R](origin, f, mirrorCondition, cachingDisabled = disableCaching)
	
	/**
	  * Creates a conditional view into another pointer
	  * @param origin The viewed pointer
	  * @param viewCondition A condition that must be met in order for the viewing to be active.
	  *                      If set to false, the last available value will be viewed instead.
	  *  @tparam O Type of the viewed values
	  * @return A new view that reflects the specified pointer's value, but only while the view condition is met
	  */
	def viewWhile[O](origin: Changing[O], viewCondition: FlagLike) =
		apply(origin, viewCondition, disableCaching = true)(Identity)
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
class OptimizedMirror[O, R](origin: Changing[O], f: O => R, condition: FlagLike = AlwaysTrue,
                            cachingDisabled: Boolean = false)
	extends OptimizedChanging[R]
{
	// ATTRIBUTES   -------------------------
	
	private val bridge = OptimizedBridge.map(origin, hasListenersFlag && condition, cachingDisabled)(f)(fireEvent)
	// A placeholder value returned while mirroring is not allowed
	private var placeholder: Option[R] = None
	
	
	// INITIAL CODE -------------------------
	
	// Whenever stops listening to the origin pointer,
	// stores the last known value, so that it may be used as a placeholder
	condition.addListenerAndSimulateEvent(true) { event =>
		if (event.newValue)
			placeholder = None
		else
			placeholder = Some(f(origin.value))
			
		// If the origin doesn't change anymore, it is not needful to track the listening condition
		ChangeResponse.continueIf(origin.mayChange)
	}
	
	onceSourceStops(origin) {
		declareChangingStopped()
		bridge.detach()
	}
	onceSourceStopsAt(condition, false) {
		declareChangingStopped()
		bridge.detach()
	}
	
	
	// IMPLEMENTED  -------------------------
	
	override implicit def listenerLogger: Logger = origin.listenerLogger
	
	override def value: R = placeholder.getOrElse(bridge.value)
	override def destiny: Destiny = origin.destiny.sealedIf { condition.isAlwaysFalse }
	
	override def readOnly = this
	
	override def toString = s"Mirroring $origin when $condition, caching = ${!cachingDisabled}, currently $value $destiny"
}
