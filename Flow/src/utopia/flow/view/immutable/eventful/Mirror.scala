package utopia.flow.view.immutable.eventful

import utopia.flow.event.listener.{ChangeDependency, ChangeListener}
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
	def of[O, R](source: Changing[O])(f: O => R) =
	{
		if (source.isChanging)
			new Mirror(source)(f)
		else
			Fixed(f(source.value))
	}
}

/**
 * Used for mapping a changing value
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1.8
 * @param source The original item that is being mirrored
 * @param f A mapping function for the mirrored value
 * @tparam Origin Type of the mirror origin (value from source item)
 * @tparam Reflection Type of mirror reflection (value from this item)
 */
class Mirror[Origin, Reflection](source: Changing[Origin])(f: Origin => Reflection) extends AbstractChanging[Reflection]
{
	// ATTRIBUTES   ------------------------------
	
	private var _value = f(source.value)
	
	
	// INITIAL CODE ------------------------------
	
	// Mirrors the source pointer
	startMirroring(source)(f) { _value = _ }
	
	
	// IMPLEMENTED  ------------------------------
	
	override def value = _value
	
	override def isChanging = source.isChanging
}