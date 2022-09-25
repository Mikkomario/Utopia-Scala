package utopia.flow.view.immutable.eventful

import utopia.flow.event.listener.{ChangeDependency, ChangeListener}
import utopia.flow.view.template.eventful.{AbstractChanging, Changing}

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
	{
		// Uses mapping functions or even a fixed value if possible
		if (firstSource.isChanging) {
			if (secondSource.isChanging)
				new MergeMirror(firstSource, secondSource)(f)
			else
				firstSource.map { f(_, secondSource.value) }
		}
		else if (secondSource.isChanging)
			secondSource.map { f(firstSource.value, _) }
		else
			Fixed(f(firstSource.value, secondSource.value))
	}
}

/**
 * Used for combining two changing values into a single changing merge value
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1.8
 * @param firstSource The first original item that is being merged
 * @param secondSource The second original item that is being merged
 * @param merge A mapping function for the mirrored value
 * @tparam O1 Type of the mirror origin (value from source item)
 * @tparam O2 Type of the second mirror origin (value from second source item)
 * @tparam Reflection Type of mirror reflection (value from this item)
 */
class MergeMirror[+O1, +O2, Reflection](firstSource: Changing[O1], secondSource: Changing[O2])
                                       (merge: (O1, O2) => Reflection)
	extends AbstractChanging[Reflection]
{
	// ATTRIBUTES   ------------------------------
	
	private var _value = merge(firstSource.value, secondSource.value)
	
	
	// INITIAL CODE ------------------------------
	
	// Updates value whenever original value changes. Also generates change events for the listeners
	startMirroring(firstSource) { merge(_, secondSource.value) } { _value = _ }
	startMirroring(secondSource) { merge(firstSource.value, _) } { _value = _ }
	
	
	// IMPLEMENTED  ------------------------------
	
	override def value = _value
	
	override def isChanging = firstSource.isChanging || secondSource.isChanging
}
