package utopia.flow.view.immutable.eventful

import utopia.flow.collection.immutable.Pair
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponse.Detach
import utopia.flow.operator.enumeration.End
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.Changing

object LazyMergeMirror
{
	/**
	  * Creates a new lazily merging mirror
	  * @param source1 First mirrored item
	  * @param source2 Second mirrored item
	  * @param merge Merge function
	  * @tparam O1 Type of first mirrored value
	  * @tparam O2 Type of second mirrored value
	  * @tparam Reflection Type of merge function result
	  * @return A new lazily merging mirror
	  */
	def of[O1, O2, Reflection](source1: Changing[O1], source2: Changing[O2])(merge: (O1, O2) => Reflection) =
	{
		// Uses lazy mapping or even lazy wrapping if possible
		if (source1.mayChange) {
			if (source2.mayChange)
				apply(source1, source2)(merge)
			else
				source1.lazyMap { merge(_, source2.value) }
		}
		else if (source2.mayChange)
			source2.lazyMap { merge(source1.value, _) }
		else
			Lazy { merge(source1.value, source2.value) }
	}
	
	/**
	  * Creates a new mirror that lazily merges the values of two changing items,
	  * updating the merge result when necessary.
	  * @param source1 The first source item
	  * @param source2 The second source item
	  * @param merge A merge function
	  * @tparam O1 Type of values in the first item
	  * @tparam O2 Type of values in the second item
	  * @tparam R Type of merge result
	  * @return A new lazily merging mirror
	  */
	def apply[O1, O2, R](source1: Changing[O1], source2: Changing[O2])(merge: (O1, O2) => R) =
		new LazyMergeMirror[O1, O2, R](source1, source2)(merge)
}

/**
  * A lazily calculating pointer that bases its value on two pointers and a merge function
  * @author Mikko Hilpinen
  * @since 24.10.2020, v1.9
  */
class LazyMergeMirror[+O1, +O2, Reflection](source1: Changing[O1], source2: Changing[O2])
                                            (merge: (O1, O2) => Reflection)
	extends Lazy[Reflection]
{
	// ATTRIBUTES	-------------------------------
	
	private val sources = Pair(source1, source2)
	
	private val cacheP = Pointer.empty[Reflection]
	private lazy val resetCacheListeners: Pair[ChangeListener[Any]] = End.values.map { listenedSide =>
		ChangeListener.onAnyChange {
			cacheP.clear()
			detachListener(listenedSide.opposite)
			Detach
		}
	}
	
	
	// IMPLEMENTED	-------------------------------
	
	override def current: Option[Reflection] = cacheP.value
	
	override def value: Reflection = cacheP.value.getOrElse {
		val result = merge(source1.value, source2.value)
		cacheP.setOne(result)
		sources.mergeWith(resetCacheListeners) { _.addHighPriorityListener(_) }
		result
	}
	
	
	// OTHER    ---------------------------------
	
	private def detachListener(side: End): Unit = sources(side).removeListener(resetCacheListeners(side))
}
