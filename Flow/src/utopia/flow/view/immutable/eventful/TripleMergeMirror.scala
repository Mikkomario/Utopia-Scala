package utopia.flow.view.immutable.eventful

import utopia.flow.view.template.eventful.{AbstractChanging, Changing}

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
	def of[O1, O2, O3, R](firstSource: Changing[O1], secondSource: Changing[O2], thirdSource: Changing[O3])
	                     (merge: (O1, O2, O3) => R) =
	{
		// Uses mapping functions or even a fixed value if possible
		if (firstSource.isChanging)
		{
			if (secondSource.isChanging)
			{
				if (thirdSource.isChanging)
					apply(firstSource, secondSource, thirdSource)(merge)
				else
					MergeMirror(firstSource, secondSource)({ (a, b) => merge(a, b, thirdSource.value) })
			}
			else if (thirdSource.isChanging)
				MergeMirror(firstSource, thirdSource)({ (a, c) => merge(a, secondSource.value, c) })
			else
				firstSource.map { merge(_, secondSource.value, thirdSource.value) }
		}
		else if (secondSource.isChanging)
		{
			if (thirdSource.isChanging)
				MergeMirror(secondSource, thirdSource)({ (b, c) => merge(firstSource.value, b, c) })
			else
				secondSource.map { merge(firstSource.value, _, thirdSource.value) }
		}
		else
			Fixed(merge(firstSource.value, secondSource.value, thirdSource.value))
	}
	
	/**
	  * Creates a new merge mirror that reflects the changes in three source pointers,
	  * merging their values into one cached value
	  * @param source1          First source item
	  * @param source2          Second source item
	  * @param source3          Third source item
	  * @param merge A merge function used for acquiring the consecutive values.
	  *              Accepts the values of the three source items and yields a merge result.
	  * @tparam O1 Type of first source values
	  * @tparam O2 Type of second source values
	  * @tparam O3 Type of third source values
	  * @tparam R  Type of merge results
	  * @return A new merge mirror
	  */
	def apply[O1, O2, O3, R](source1: Changing[O1], source2: Changing[O2], source3: Changing[O3])
	                        (merge: (O1, O2, O3) => R) =
		new TripleMergeMirror[O1, O2, O3, R](source1, source2, source3,
			merge(source1.value, source2.value, source3.value))((_, v1, v2, v3) => merge(v1, v2, v3))
	
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
                                          initialValue: R)
                                         (merge: (R, O1, O2, O3) => R)
	extends AbstractChanging[R]
{
	// ATTRIBUTES   -----------------------------
	
	private var _value = initialValue
	
	
	// INITIAL CODE -----------------------------
	
	// Mirrors all source pointers
	startMirroring(source1) { (v, e1) => merge(v, e1.newValue, source2.value, source3.value) } { _value = _ }
	startMirroring(source2) { (v, e2) => merge(v, source1.value, e2.newValue, source3.value) } { _value = _ }
	startMirroring(source3) { (v, e3) => merge(v, source1.value, source2.value, e3.newValue) } { _value = _ }
	
	
	// IMPLEMENTED  -----------------------------
	
	override def isChanging = source1.isChanging || source2.isChanging || source3.isChanging
	
	override def value = _value
}
