package utopia.flow.event

import utopia.flow.datastructure.immutable.Lazy
import utopia.flow.datastructure.mutable.ResettableLazy
import utopia.flow.datastructure.template.LazyLike

object LazyMirror
{
	/**
	  * @param pointer Mirrored item
	  * @param f Mapping function
	  * @tparam O Type of item before mapping
	  * @tparam R Type of item after mapping
	  * @return A lazily mirrored view to specified pointer
	  */
	def of[O, R](pointer: ChangingLike[O])(f: O => R) =
	{
		if (pointer.isChanging)
			new LazyMirror(pointer)(f)
		else
			Lazy { f(pointer.value) }
	}
}

/**
  * Provides a read-only access to a changing item's mapped value. Performs the mapping operation only when required,
  * but doesn't provide listener interface because of that.
  * @author Mikko Hilpinen
  * @since 22.7.2020, v1.8
  * @param source The changing item being mirrored
  * @param f A mirroring / mapping function used
  * @tparam Origin Type of item before mirroring
  * @tparam Reflection Type of item after mirroring
  */
class LazyMirror[Origin, Reflection](source: ChangingLike[Origin])(f: Origin => Reflection)
	extends LazyLike[Reflection]
{
	// ATTRIBUTES	--------------------------
	
	private val cache = ResettableLazy { f(source.value) }
	
	
	// INITIAL CODE	--------------------------
	
	// Resets cache whenever original pointer changes
	source.addDependency(ChangeDependency.beforeAnyChange { cache.reset() })
	
	
	// IMPLEMENTED	--------------------------
	
	override def value = cache.value
	
	override def current = cache.current
}
