package utopia.flow.view.immutable.eventful

import utopia.flow.collection.immutable.Empty
import utopia.flow.event.model.Destiny.{ForeverFlux, MaySeal, Sealed}
import utopia.flow.event.model.{ChangeResponse, Destiny}
import utopia.flow.operator.Identity
import utopia.flow.util.logging.Logger
import utopia.flow.util.TryExtensions._
import utopia.flow.view.template.eventful.{Changing, Flag, OptimizedChanging}

import scala.util.Try

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
	def apply[O, R](origin: Changing[O], mirrorCondition: Flag = AlwaysTrue,
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
	def viewWhile[O](origin: Changing[O], viewCondition: Flag) =
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
class OptimizedMirror[O, R](origin: Changing[O], f: O => R, condition: Flag = AlwaysTrue,
                            cachingDisabled: Boolean = false)
	extends OptimizedChanging[R]
{
	// ATTRIBUTES   -------------------------
	
	private val bridge = OptimizedBridge.map(origin, hasListenersFlag && condition, cachingDisabled)(f) { event =>
		// Only fires change events while allowed to mirror
		if (condition.value)
			fireEvent(event)
		else
			Empty
	}
	// A placeholder value returned while mirroring is not allowed
	private var placeholder: Option[R] = None
	
	
	// INITIAL CODE -------------------------
	
	// Whenever stops listening to the origin pointer,
	// stores the last known value, so that it may be used as a placeholder
	condition.addListenerAndSimulateEvent(true) { event =>
		// Case: Mirroring continues => Clears the placeholder value and fires a change event, if necessary
		if (event.newValue) {
			val oldPlaceholder = placeholder
			placeholder = None
			oldPlaceholder.foreach { p => fireEventIfNecessary(oldValue = p).foreach { a => Try { a() }.log } }
		}
		// Case: Mirroring stops => Prepares to return the last value until mirroring is enabled again
		else
			placeholder = Some(bridge.value)
			
		// If the origin doesn't change anymore, it is not needful to track the listening condition
		ChangeResponse.continueIf(origin.mayChange)
	}
	
	onceSourceStops(origin) {
		declareChangingStopped()
		bridge.detach()
	}
	// Checks if mirroring should be permanently disabled
	onceSourceStopsAt(condition, false) {
		declareChangingStopped()
		bridge.detach()
	}
	
	
	// IMPLEMENTED  -------------------------
	
	override implicit def listenerLogger: Logger = origin.listenerLogger
	
	override def value: R = placeholder.getOrElse(bridge.value)
	// Modifies the origin "destiny" to take into account the mirroring condition
	override def destiny: Destiny = condition.destiny match {
		// Case: Fixed condition => Sealed if mirroring is disabled, otherwise always same as origin
		case Sealed => if (condition.value) origin.destiny else Sealed
		// Case: Possible to seal => This mirror may always seal, if condition seals to false
		case MaySeal => origin.destiny.possibleToSeal
		// Case: Impossible to seal => Only seals if origin seals
		case ForeverFlux => origin.destiny
	}
	
	override def readOnly = this
	
	override def toString = fixedValue match {
		case Some(fixed) => s"Reflecting.always($fixed)"
		case None =>
			condition.fixedValue match {
				case Some(isMirroring) =>
					if (isMirroring)
						s"Mirroring($origin)"
					else
						s"Reflecting.always($value)"
				case None => s"Mirroring($origin).while($condition)"
			}
	}
	
	override def lockWhile[B](operation: => B): B =
		if (placeholder.isDefined) condition.lockWhile(operation) else origin.lockWhile(operation)
	override def viewLocked[B](operation: R => B) = placeholder match {
		case Some(value) => condition.lockWhile { operation(value) }
		case None => origin.lockWhile { operation(value) }
	}
}
