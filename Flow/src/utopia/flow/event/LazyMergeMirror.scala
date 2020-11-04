package utopia.flow.event

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
	def of[O1, O2, Reflection](source1: Changing[O1], source2: Changing[O2])(merge: (O1, O2) => Reflection) =
		new LazyMergeMirror(source1, source2)(merge)
}

/**
  * A lazily calculating pointer that bases its value on two pointers and a merge function
  * @author Mikko Hilpinen
  * @since 24.10.2020, v1.9
  */
class LazyMergeMirror[O1, O2, Reflection](source1: Changing[O1], source2: Changing[O2])(merge: (O1, O2) => Reflection)
	extends LazyLike[Reflection]
{
	// ATTRIBUTES	-------------------------------
	
	private val cache = ResettableLazy { merge(source1.value, source2.value) }
	
	
	// INITIAL CODE	-------------------------------
	
	source1.addListener { _ => cache.reset() }
	source2.addListener { _ => cache.reset() }
	
	
	// IMPLEMENTED	-------------------------------
	
	override def value = cache.value
	
	override def current = cache.current
}
