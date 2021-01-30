package utopia.flow.event

object TripleMergeMirror
{
	/**
	 * Creates a new merging of the three specified pointers. Uses the simples available merging method,
	 * based on whether the pointers will change.
	 * @param firstSource First input pointer
	 * @param secondSource Second input pointer
	 * @param thirdSource Third input pointer
	 * @param merge A function for merging pointer values
	 * @tparam O1 Type of first pointer value
	 * @tparam O2 Type of second pointer value
	 * @tparam O3 Type of third pointer value
	 * @tparam R Type of merge result
	 * @return A new pointer that will contain the merged value
	 */
	def of[O1, O2, O3, R](firstSource: ChangingLike[O1], secondSource: ChangingLike[O2], thirdSource: ChangingLike[O3])
	                     (merge: (O1, O2, O3) => R) =
	{
		// Uses mapping functions or even a fixed value if possible
		if (firstSource.isChanging)
		{
			if (secondSource.isChanging)
			{
				if (thirdSource.isChanging)
					new TripleMergeMirror(firstSource, secondSource, thirdSource)(merge)
				else
					new MergeMirror(firstSource, secondSource)({ (a, b) => merge(a, b, thirdSource.value) })
			}
			else if (thirdSource.isChanging)
				new MergeMirror(firstSource, thirdSource)({ (a, c) => merge(a, secondSource.value, c) })
			else
				firstSource.map { merge(_, secondSource.value, thirdSource.value) }
		}
		else if (secondSource.isChanging)
		{
			if (thirdSource.isChanging)
				new MergeMirror(secondSource, thirdSource)({ (b, c) => merge(firstSource.value, b, c) })
			else
				secondSource.map { merge(firstSource.value, _, thirdSource.value) }
		}
		else
			Fixed(merge(firstSource.value, secondSource.value, thirdSource.value))
	}
}

/**
 * A fusion of three different pointers
 * @author Mikko Hilpinen
 * @since 30.1.2021, v1.9
 */
class TripleMergeMirror[+O1, +O2, +O3, Reflection](firstSource: ChangingLike[O1], secondSource: ChangingLike[O2],
                                                thirdSource: ChangingLike[O3])(merge: (O1, O2, O3) => Reflection)
	extends Changing[Reflection]
{
	// ATTRIBUTES   -----------------------------
	
	var listeners = Vector[ChangeListener[Reflection]]()
	
	private var _value = merge(firstSource.value, secondSource.value, thirdSource.value)
	
	
	// INITIAL CODE -----------------------------
	
	firstSource.addListener { e => updateValue(merge(e.newValue, secondSource.value, thirdSource.value)) }
	secondSource.addListener { e => updateValue(merge(firstSource.value, e.newValue, thirdSource.value)) }
	thirdSource.addListener { e => updateValue(merge(firstSource.value, secondSource.value, e.newValue)) }
	
	
	// IMPLEMENTED  -----------------------------
	
	override def isChanging = firstSource.isChanging || secondSource.isChanging || thirdSource.isChanging
	
	override def value = _value
	
	
	// OTHER    ---------------------------------
	
	private def updateValue(newValue: Reflection) =
	{
		if (_value != newValue)
		{
			val old = _value
			_value = newValue
			fireChangeEvent(old)
		}
	}
}
