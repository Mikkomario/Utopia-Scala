package utopia.flow.event

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
	def of[O, R](source: ChangingLike[O])(f: O => R) =
	{
		if (source.isChanging)
			apply(source)(f)
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
case class Mirror[Origin, Reflection](source: ChangingLike[Origin])(f: Origin => Reflection) extends Changing[Reflection]
{
	// ATTRIBUTES   ------------------------------
	
	private var _value = f(source.value)
	
	var listeners = Vector[ChangeListener[Reflection]]()
	
	
	// INITIAL CODE ------------------------------
	
	// Updates value whenever original value changes. Also generates change events for the listeners
	source.addListener { e =>
		val newValue = f(e.newValue)
		if (newValue != _value)
		{
			val oldValue = _value
			_value = newValue
			fireChangeEvent(oldValue)
		}
	}
	
	
	// IMPLEMENTED  ------------------------------
	
	override def value = _value
	
	override def isChanging = source.isChanging
}
