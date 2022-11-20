package utopia.flow.view.immutable.eventful

import utopia.flow.event.model.ChangeEvent
import utopia.flow.view.template.eventful.{AbstractChanging, Changing}

object Mirror
{
	/**
	 * Creates a new mirror that reflects another changing item
	 * @param source Changing item that generates the values in this mirror
	 * @param f A mapping function for generating the mirrored value
	 * @tparam O Original item type
	 * @tparam R Reflected / mapped item type
	 * @return A new mirror
	 */
	@deprecated("Please use source.map(f) instead", "v2.0")
	def of[O, R](source: Changing[O])(f: O => R) =
	{
		if (source.isChanging)
			apply(source)(f)
		else
			Fixed(f(source.value))
	}
	
	/**
	  * Creates a new mirror that reflects changes in another item
	  * @param source A source item
	  * @param f A mapping function for source item values
	  * @tparam O Type of source values
	  * @tparam R Type of map results
	  * @return A new mirror that contains the map results
	  */
	def apply[O, R](source: Changing[O])(f: O => R) = new Mirror[O, R](source, f(source.value))((_, e) => f(e.newValue))
	
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
 * @param f A mapping function for the mirrored value
 * @tparam O Type of the mirror origin (value from source item)
 * @tparam R Type of mirror reflection (value from this item)
 */
class Mirror[+O, R](source: Changing[O], initialValue: R)(f: (R, ChangeEvent[O]) => R) extends AbstractChanging[R]
{
	// ATTRIBUTES   ------------------------------
	
	private var _value = initialValue
	
	
	// INITIAL CODE ------------------------------
	
	// Mirrors the source pointer
	startMirroring(source)(f) { _value = _ }
	
	
	// IMPLEMENTED  ------------------------------
	
	override def value = _value
	
	override def isChanging = source.isChanging
}
