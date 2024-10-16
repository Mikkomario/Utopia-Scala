package utopia.flow.view.immutable.eventful

import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponse.Continue
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.flow.view.template.eventful.{Changing, ListenableLazyWrapper}

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
				new LazyMergeMirror(source1, source2)(merge)
			else
				source1.lazyMap { merge(_, source2.value) }
		}
		else if (source2.mayChange)
			source2.lazyMap { merge(source1.value, _) }
		else
			Lazy.listenable { merge(source1.value, source2.value) }(source1.listenerLogger)
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
class LazyMergeMirror[+O1, +O2, +Reflection](source1: Changing[O1], source2: Changing[O2])
                                            (merge: (O1, O2) => Reflection)
	extends ListenableLazyWrapper[Reflection]
{
	// ATTRIBUTES	-------------------------------
	
	private val cache = ResettableLazy.listenable { merge(source1.value, source2.value) }(source1.listenerLogger)
	private lazy val listener = ChangeListener.onAnyChange { cache.reset(); Continue }
	
	
	// INITIAL CODE	-------------------------------
	
	source1.addHighPriorityListener(listener)
	source2.addHighPriorityListener(listener)
	
	
	// IMPLEMENTED	-------------------------------
	
	override protected def wrapped = cache
}
