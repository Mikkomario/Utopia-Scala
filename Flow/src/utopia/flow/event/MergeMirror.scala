package utopia.flow.event

object MergeMirror
{
	/**
	 * Creates a new mirror that reflects another changing item
	 * @param firstSource Changing item that generates the values in this mirror
	 * @param secondSource Another changing item that generates the values in this mirror
	 * @param f A mapping function for generating the mirrored value
	 * @tparam O1 Original item type
	 * @tparam O2 Another original item type
	 * @tparam R Merged / mapped item type
	 * @return A new mirror
	 */
	def of[O1, O2, R](firstSource: Changing[O1], secondSource: Changing[O2])(f: (O1, O2) => R) =
		new MergeMirror(firstSource, secondSource)(f)
}

/**
 * Used for combining two changing values into a single changing merge value
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1.8
 * @param firstSource The first original item that is being merged
 * @param secondSource The second original item that is being merged
 * @param f A mapping function for the mirrored value
 * @tparam O1 Type of the mirror origin (value from source item)
 * @tparam O2 Type of the second mirror origin (value from second source item)
 * @tparam Reflection Type of mirror reflection (value from this item)
 */
class MergeMirror[O1, O2, Reflection](val firstSource: Changing[O1], val secondSource: Changing[O2])(
	f: (O1, O2) => Reflection) extends Changing[Reflection]
{
	// ATTRIBUTES   ------------------------------
	
	private var _value = f(firstSource.value, secondSource.value)
	
	
	// INITIAL CODE ------------------------------
	
	// Updates value whenever original value changes. Also generates change events for the listeners
	firstSource.addListener { e => updateValue(f(e.newValue, secondSource.value)) }
	secondSource.addListener { e => updateValue(f(firstSource.value, e.newValue)) }
	
	
	// IMPLEMENTED  ------------------------------
	
	override def value = _value
	
	
	// OTHER    ----------------------------------
	
	private def updateValue(newValue: Reflection) =
	{
		if (newValue != _value)
		{
			val oldValue = _value
			_value = newValue
			fireChangeEvent(oldValue)
		}
	}
}
