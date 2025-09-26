package utopia.flow.view.immutable.eventful

import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponse.Detach
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.Changing

object LazyTripleMergeMirror
{
	/**
	  * Creates a new mirror which lazily merges three sources.
	  * Uses a simpler mirror implementation if the sources involve static values.
	  * @param source1 First source pointer
	  * @param source2 Second source pointer
	  * @param source3 Third source pointer
	  * @param merge A function for merging the values of these sources
	  * @tparam O1 Type of the values in the first source
	  * @tparam O2 Type of the values in the second source
	  * @tparam O3 Type of the values in the third source
	  * @tparam R Type of merge results
	  * @return A lazily merging mirror
	  */
	def of[O1, O2, O3, R](source1: Changing[O1], source2: Changing[O2], source3: Changing[O3])
	                     (merge: (O1, O2, O3) => R): Lazy[R] =
	{
		// Optimizes the listener-usage
		if (source1.mayChange) {
			if (source2.mayChange) {
				if (source3.mayChange)
					apply(source1, source2, source3)(merge)
				else
					source1.lazyMergeWith(source2) { merge(_, _, source3.value) }
			}
			else if (source3.mayChange)
				source1.lazyMergeWith(source3) { merge(_, source2.value, _) }
			else
				source1.lazyMap { merge(_, source2.value, source3.value) }
		}
		else if (source2.mayChange) {
			if (source3.mayChange)
				source2.lazyMergeWith(source3) { merge(source1.value, _, _) }
			else
				source2.lazyMap { merge(source1.value, _, source3.value) }
		}
		else if (source3.mayChange)
			source3.lazyMap { merge(source1.value, source2.value, _) }
		else
			Lazy { merge(source1.value, source2.value, source3.value) }
	}
	
	/**
	  * Creates a new mirror which lazily merges three sources
	  * @param source1 First source pointer
	  * @param source2 Second source pointer
	  * @param source3 Third source pointer
	  * @param merge A function for merging the values of these sources
	  * @tparam O1 Type of the values in the first source
	  * @tparam O2 Type of the values in the second source
	  * @tparam O3 Type of the values in the third source
	  * @tparam R Type of merge results
	  * @return A lazily merging mirror
	  */
	def apply[O1, O2, O3, R](source1: Changing[O1], source2: Changing[O2], source3: Changing[O3])
	                        (merge: (O1, O2, O3) => R) =
		new LazyTripleMergeMirror(source1, source2, source3)(merge)
}

/**
  * A lazily calculating pointer that bases its value on three pointers and a merge function
  * @author Mikko Hilpinen
  * @since 16.8.2024, v2.5
  */
class LazyTripleMergeMirror[+O1, +O2, +O3, Reflection](source1: Changing[O1], source2: Changing[O2],
                                                       source3: Changing[O3])
                                                      (merge: (O1, O2, O3) => Reflection)
	extends Lazy[Reflection]
{
	// ATTRIBUTES	-------------------------------
	
	private lazy val sources = Vector(source1, source2, source3)
	
	private val cacheP = Pointer.empty[Reflection]
	private val resetCacheListeners: Seq[ChangeListener[Any]] = sources.indices.map { i =>
		ChangeListener.onAnyChange {
			cacheP.clear()
			detachListeners(i)
			Detach
		}
	}
	
	
	// IMPLEMENTED	-------------------------------
	
	override def current: Option[Reflection] = cacheP.value
	
	override def value: Reflection = cacheP.value.getOrElse {
		val result = merge(source1.value, source2.value, source3.value)
		cacheP.setOne(result)
		sources.iterator.zip(resetCacheListeners).foreach { case (source, listener) =>
			source.addHighPriorityListener(listener)
		}
		result
	}
	
	
	// OTHER    ------------------------------------
	
	private def detachListeners(excludingIndex: Int): Unit = {
		sources.iterator.zipWithIndex.zip(resetCacheListeners).foreach { case ((source, i), listener) =>
			if (i != excludingIndex)
				source.removeListener(listener)
		}
	}
}
