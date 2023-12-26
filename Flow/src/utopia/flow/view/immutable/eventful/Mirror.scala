package utopia.flow.view.immutable.eventful

import utopia.flow.event.model.Destiny.{ForeverFlux, MaySeal, Sealed}
import utopia.flow.event.model.{ChangeEvent, Destiny}
import utopia.flow.view.template.eventful.{AbstractMayStopChanging, Changing}

object Mirror
{
	/**
	  * Creates a new mirror that reflects changes in another item
	  * @param source A source item
	  * @param condition Condition that must be met for the mirroring to occur (default = always active)
	  * @param f A mapping function for source item values
	  * @tparam O Type of source values
	  * @tparam R Type of map results
	  * @return A new mirror that contains the map results
	  */
	def apply[O, R](source: Changing[O], condition: Changing[Boolean] = AlwaysTrue)(f: O => R) =
		new Mirror[O, R](source, f(source.value), condition)((_, e) => f(e.newValue))
	
	/**
	  * Creates a new mirror that reflects changes in another item.
	  * Uses a more detailed mapping function, when comparing to .apply(...)
	  * @param source A source item
	  * @param initialMap Mapping function applied in order to acquire the initially held value.
	  *                   Accepts the current source item value.
	  * @param incrementMap A mapping function used for acquiring consecutive values.
	  *                     Accepts:
	  *                     1) Currently held value of this item, and
	  *                     2) Change event that occurred in the source item
	  * @tparam O Type of source item values
	  * @tparam R Type of map results
	  * @return A new mirror
	  */
	def incremental[O, R](source: Changing[O])(initialMap: O => R)(incrementMap: (R, ChangeEvent[O]) => R) =
		new Mirror[O, R](source, initialMap(source.value))(incrementMap)
}

/**
 * Used for mapping a changing value
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1.8
 * @param source The original item that is being mirrored
 * @param condition Condition that must be met for the mirroring to occur (default = always active)
  * @param f A mapping function for the mirrored value
 * @tparam O Type of the mirror origin (value from source item)
 * @tparam R Type of mirror reflection (value from this item)
 */
class Mirror[+O, R](source: Changing[O], initialValue: R, condition: Changing[Boolean] = AlwaysTrue)
                   (f: (R, ChangeEvent[O]) => R)
	extends AbstractMayStopChanging[R]
{
	// ATTRIBUTES   ------------------------------
	
	private var _value = initialValue
	
	
	// INITIAL CODE ------------------------------
	
	// Mirrors the source pointer
	startMirroring(source, condition)(f) { _value = _ }
	
	stopOnceSourceStops(source)
	// If the condition becomes fixed to false, fires a stopped changing -event
	onceSourceStopsAt(condition, false) { declareChangingStopped() }
	
	
	// IMPLEMENTED  ------------------------------
	
	override def value = _value
	override def destiny: Destiny = condition.destiny match {
		// Case: Mirror condition is fixed => Considers this pointer as fixed if the condition is fixed to false
		case Sealed => if (condition.value) source.destiny else Sealed
		// Case: Mirror condition may become fixed =>
		// Considers it possible that this mirror becomes fixed at some point, regardless of source status
		case MaySeal => source.destiny.possibleToSeal
		// Case: Condition is variable => Only becomes fixed if the source does
		case ForeverFlux => source.destiny
	}
	
	override def readOnly = this
}
