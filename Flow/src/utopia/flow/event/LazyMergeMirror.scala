package utopia.flow.event

import utopia.flow.datastructure.immutable.Lazy
import utopia.flow.datastructure.mutable.ResettableLazy
import utopia.flow.datastructure.template.LazyLike

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
	def of[O1, O2, Reflection](source1: ChangingLike[O1], source2: ChangingLike[O2])(merge: (O1, O2) => Reflection) =
	{
		// Uses lazy mapping or even lazy wrapping if possible
		if (source1.isChanging)
		{
			if (source2.isChanging)
				new LazyMergeMirror(source1, source2)(merge)
			else
				source1.lazyMap { merge(_, source2.value) }
		}
		else if (source2.isChanging)
			source2.lazyMap { merge(source1.value, _) }
		else
			Lazy { merge(source1.value, source2.value) }
	}
}

/**
  * A lazily calculating pointer that bases its value on two pointers and a merge function
  * @author Mikko Hilpinen
  * @since 24.10.2020, v1.9
  */
class LazyMergeMirror[O1, O2, Reflection](source1: ChangingLike[O1], source2: ChangingLike[O2])
                                         (merge: (O1, O2) => Reflection)
	extends LazyLike[Reflection]
{
	// ATTRIBUTES	-------------------------------
	
	private val cache = ResettableLazy { merge(source1.value, source2.value) }
	private lazy val listener = ChangeDependency.beforeAnyChange { cache.reset() }
	
	
	// INITIAL CODE	-------------------------------
	
	source1.addDependency(listener)
	source2.addDependency(listener)
	
	
	// IMPLEMENTED	-------------------------------
	
	override def value = cache.value
	
	override def current = cache.current
}
