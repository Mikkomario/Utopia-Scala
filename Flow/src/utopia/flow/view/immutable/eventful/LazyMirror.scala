package utopia.flow.view.immutable.eventful

import utopia.flow.event.model.ChangeResponse.Continue
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.flow.view.template.eventful.{Changing, ListenableLazyWrapper}

object LazyMirror
{
	/**
	  * Creates a new lazy mirror, which lazily reflects the value of a source item
	  * @param source A source item
	  * @param f A mapping function
	  * @tparam O Type of source values
	  * @tparam R Type of mapping results
	  * @return A new mirror
	  */
	def apply[O, R](source: Changing[O])(f: O => R) = new LazyMirror[O, R](source)(f)
}

/**
  * Provides read-only access to a changing item's mapped value. Performs the mapping operation only when required,
  * but doesn't provide listener interface because of that.
  * @author Mikko Hilpinen
  * @since 22.7.2020, v1.8
  * @param source The changing item being mirrored
  * @param f A mirroring / mapping function used
  * @tparam Origin Type of item before mirroring
  * @tparam Reflection Type of item after mirroring
  */
class LazyMirror[+Origin, Reflection](source: Changing[Origin])(f: Origin => Reflection)
	extends ListenableLazyWrapper[Reflection]
{
	// ATTRIBUTES	--------------------------
	
	private val cache = ResettableLazy.listenable { f(source.value) }(source.listenerLogger)
	
	
	// INITIAL CODE	--------------------------
	
	// Resets cache whenever original pointer changes
	source.addHighPriorityListener { _ => cache.reset(); Continue }
	
	
	// IMPLEMENTED	--------------------------
	
	override protected def wrapped = cache
}
