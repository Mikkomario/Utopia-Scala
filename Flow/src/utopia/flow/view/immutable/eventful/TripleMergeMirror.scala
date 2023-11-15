package utopia.flow.view.immutable.eventful

import utopia.flow.event.model.Destiny
import utopia.flow.view.template.eventful.{AbstractMayStopChanging, Changing}

object TripleMergeMirror
{
	/**
	 * Creates a new merging of the three specified pointers. Uses the simples available merging method,
	 * based on whether the pointers will change.
	 * @param firstSource First input pointer
	 * @param secondSource Second input pointer
	 * @param thirdSource Third input pointer
	 * @param condition A condition that must be met for the listening / tracking to occur (default = always active)
	  * @param merge A function for merging pointer values
	 * @tparam O1 Type of first pointer value
	 * @tparam O2 Type of second pointer value
	 * @tparam O3 Type of third pointer value
	 * @tparam R Type of merge result
	 * @return A new pointer that will contain the merged value
	 */
	def of[O1, O2, O3, R](firstSource: Changing[O1], secondSource: Changing[O2], thirdSource: Changing[O3],
	                      condition: Changing[Boolean] = AlwaysTrue)
	                     (merge: (O1, O2, O3) => R) =
	{
		// Uses mapping functions or even a fixed value if possible
		if (firstSource.mayChange) {
			if (secondSource.mayChange) {
				// Case: All sources change => Uses triple merge mirror
				if (thirdSource.mayChange)
					apply(firstSource, secondSource, thirdSource, condition)(merge)
				// Case: Sources 1 & 2 change => Uses merge mirror
				else
					MergeMirror(firstSource, secondSource, condition)({ (a, b) => merge(a, b, thirdSource.value) })
			}
			// Case: Source 1 & 3 chage => Uses merge mirror
			else if (thirdSource.mayChange)
				MergeMirror(firstSource, thirdSource, condition)({ (a, c) => merge(a, secondSource.value, c) })
			// Case: Only source 1 changes => Uses mapping
			else
				firstSource.mapWhile(condition) { merge(_, secondSource.value, thirdSource.value) }
		}
		else if (secondSource.mayChange) {
			// Case: Sources 2 & 3 change => Merge
			if (thirdSource.mayChange)
				MergeMirror(secondSource, thirdSource, condition)({ (b, c) => merge(firstSource.value, b, c) })
			// Case: Source 2 only changes => Maps
			else
				secondSource.mapWhile(condition) { merge(firstSource.value, _, thirdSource.value) }
		}
		// Case: Source 3 changes only => Maps
		else if (thirdSource.mayChange)
			thirdSource.mapWhile(condition) { merge(firstSource.value, secondSource.value, _) }
		// Case: Nothing changes => Uses a fixed value
		else
			Fixed(merge(firstSource.value, secondSource.value, thirdSource.value))
	}
	
	/**
	  * Creates a new merge mirror that reflects the changes in three source pointers,
	  * merging their values into one cached value
	  * @param source1          First source item
	  * @param source2          Second source item
	  * @param source3          Third source item
	  * @param condition A condition that must be met for the listening / tracking to occur (default = always active)
	  * @param merge A merge function used for acquiring the consecutive values.
	  *              Accepts the values of the three source items and yields a merge result.
	  * @tparam O1 Type of first source values
	  * @tparam O2 Type of second source values
	  * @tparam O3 Type of third source values
	  * @tparam R  Type of merge results
	  * @return A new merge mirror
	  */
	def apply[O1, O2, O3, R](source1: Changing[O1], source2: Changing[O2], source3: Changing[O3],
	                         condition: Changing[Boolean] = AlwaysTrue)
	                        (merge: (O1, O2, O3) => R) =
		new TripleMergeMirror[O1, O2, O3, R](source1, source2, source3,
			merge(source1.value, source2.value, source3.value), condition)((_, v1, v2, v3) => merge(v1, v2, v3))
	
	/**
	  * Creates a new merge mirror that reflects the changes in three source pointers,
	  * merging their values into one cached value
	  * @param source1 First source item
	  * @param source2 Second source item
	  * @param source3 Third source item
	  * @param initialMerge A merge function used for acquiring the initially stored value
	  * @param incrementalMerge A merge function used for acquiring the consecutive values.
	  *                         Accepts the current value of this pointer, plus the values of the three source items.
	  * @tparam O1 Type of first source values
	  * @tparam O2 Type of second source values
	  * @tparam O3 Type of third source values
	  * @tparam R Type of merge results
	  * @return A new merge mirror
	  */
	def incremental[O1, O2, O3, R](source1: Changing[O1], source2: Changing[O2], source3: Changing[O3])
	                              (initialMerge: (O1, O2, O3) => R)(incrementalMerge: (R, O1, O2, O3) => R) =
		new TripleMergeMirror[O1, O2, O3, R](source1, source2, source3,
			initialMerge(source1.value, source2.value, source3.value))(incrementalMerge)
}

/**
 * A fusion of three different pointers
 * @author Mikko Hilpinen
 * @since 30.1.2021, v1.9
 */
class TripleMergeMirror[+O1, +O2, +O3, R](source1: Changing[O1], source2: Changing[O2], source3: Changing[O3],
                                          initialValue: R, condition: Changing[Boolean] = AlwaysTrue)
                                         (merge: (R, O1, O2, O3) => R)
	extends AbstractMayStopChanging[R]
{
	// ATTRIBUTES   -----------------------------
	
	private val sources = Vector(source1, source2, source3)
	
	private var _value = initialValue
	
	
	// INITIAL CODE -----------------------------
	
	// Mirrors all source pointers
	startMirroring(source1, condition) { (v, e1) => merge(v, e1.newValue, source2.value, source3.value) } { _value = _ }
	startMirroring(source2, condition) { (v, e2) => merge(v, source1.value, e2.newValue, source3.value) } { _value = _ }
	startMirroring(source3, condition) { (v, e3) => merge(v, source1.value, source2.value, e3.newValue) } { _value = _ }
	
	stopOnceAllSourcesStop(sources)
	
	
	// IMPLEMENTED  -----------------------------
	
	override def value = _value
	override def destiny: Destiny = sources.map { _.destiny }.reduce { _ + _ }
}
