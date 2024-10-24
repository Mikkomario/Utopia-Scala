package utopia.flow.view.immutable.eventful

import utopia.flow.collection.immutable.Pair
import utopia.flow.event.model.Destiny.{ForeverFlux, MaySeal, Sealed}
import utopia.flow.event.model.{ChangeEvent, Destiny}
import utopia.flow.view.template.eventful.{AbstractMayStopChanging, Changing}

object MergeMirror
{
	/**
	  * Creates a new mirror that merges the values of two changing items and caches the result
	  * @param source1 First source item
	  * @param source2 Second source item
	  * @param condition A condition that must be met for the merging to occur (default = always listen & merge)
	  * @param merge A merge function
	  * @tparam O1 Type of the values in the first item
	  * @tparam O2 Type of the values in the second item
	  * @tparam R Type of merge results
	  * @return A new merge mirror
	  */
	def apply[O1, O2, R](source1: Changing[O1], source2: Changing[O2], condition: Changing[Boolean] = AlwaysTrue)
	                    (merge: (O1, O2) => R) =
		new MergeMirror[O1, O2, R](source1, source2, merge(source1.value, source2.value), condition)(
			(_, v1, v2, _) => merge(v1, v2))
	
	/**
	  * Creates a new mirror that merges the values of two changing items and caches the result.
	  * Uses a more detailed version of a merge function, which also accepts the current state of this pointer.
	  * @param source1 First source item
	  * @param source2 Second source item
	  * @param initialMerge A merge function used for acquiring the initially stored value
	  * @param incrementalMerge A merge function used for acquiring consecutive merge results.
	  *                         Accepts:
	  *                             1. Current value of this pointer,
	  *                             1. Current or new value of the first source item,
	  *                             1. Current or new value of the second source item,
	  *                             1. Either:
	  *                                 - Left: Change event that occurred in the first source item, or
	  *                                 - Right: Change event that occurred in the second source item.
	  *
	  *                         Yields a merge result.
	  * @tparam O1 Type of values in the first source item
	  * @tparam O2 Type of values in the second source item
	  * @tparam R Type of merge results
	  * @return A new merge mirror
	  */
	def incremental[O1, O2, R](source1: Changing[O1], source2: Changing[O2])
	                          (initialMerge: (O1, O2) => R)
	                          (incrementalMerge: (R, O1, O2, Either[ChangeEvent[O1], ChangeEvent[O2]]) => R) =
		new MergeMirror[O1, O2, R](source1, source2, initialMerge(source1.value, source2.value))(incrementalMerge)
}

/**
 * Used for combining two changing values into a single changing merge value
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1.8
 * @param firstSource The first original item that is being merged
 * @param secondSource The second original item that is being merged
 * @param condition A condition that must be met for the merging to occur (default = always listen & merge)
  * @param merge A merge function.
  *              Accepts 4 parameters:
  *                 1. Previously acquired result
  *                 1. Current source 1 value
  *                 1. Current source 2 value
  *                 1. Recent change event
 * @tparam O1 Type of the mirror origin (value from source item)
 * @tparam O2 Type of the second mirror origin (value from second source item)
 * @tparam R Type of mirror reflection (value from this item)
 */
class MergeMirror[+O1, +O2, R](firstSource: Changing[O1], secondSource: Changing[O2], initialValue: R,
                               condition: Changing[Boolean] = AlwaysTrue)
                              (merge: (R, O1, O2, Either[ChangeEvent[O1], ChangeEvent[O2]]) => R)
	extends AbstractMayStopChanging[R]()(firstSource.listenerLogger)
{
	// ATTRIBUTES   ------------------------------
	
	private val sources = Pair(firstSource, secondSource)
	
	private var _value = initialValue
	
	
	// INITIAL CODE ------------------------------
	
	// Updates value whenever original value changes. Also generates change events for the listeners
	startMirroring(firstSource, condition) { (v, e1) => merge(v, e1.newValue, secondSource.value, Left(e1)) } { _value = _ }
	startMirroring(secondSource, condition) { (v, e2) => merge(v, firstSource.value, e2.newValue, Right(e2)) } { _value = _ }
	
	// Handles the situation where the pointers stop changing
	stopOnceAllSourcesStop(sources)
	// Also handles the situation where the listen condition gets fixed to false
	onceSourceStopsAt(condition, false) { declareChangingStopped() }
	
	
	// COMPUTED ----------------------------------
	
	private def sourceDestiny = sources.mapAndMerge { _.destiny } { _ + _ }
	
	
	// IMPLEMENTED  ------------------------------
	
	override def value = _value
	
	override def destiny: Destiny = condition.destiny match {
		case Sealed => if (!condition.value) Sealed else sourceDestiny
		case MaySeal => sourceDestiny.possibleToSeal
		case ForeverFlux => sourceDestiny
	}
	
	override def readOnly = this
}
