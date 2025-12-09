package utopia.flow.view.immutable.eventful

import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponse.Detach
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.Changing

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
  * but doesn't provide listener interface.
  * @author Mikko Hilpinen
  * @since 22.7.2020, v1.8
  * @param source The changing item being mirrored
  * @param f A mirroring / mapping function used
  * @tparam Origin Type of item before mirroring
  * @tparam Reflection Type of item after mirroring
  */
class LazyMirror[+Origin, Reflection](source: Changing[Origin])(f: Origin => Reflection) extends Lazy[Reflection]
{
	// ATTRIBUTES	--------------------------
	
	/**
	 * Contains the cached value
	 */
	private val cacheP = Pointer.empty[Reflection]
	/**
	 * A listener used for resetting the cached value.
	 * listens only while there's a value to reset.
	 */
	private lazy val resetCacheListener = ChangeListener.onAnyChange {
		cacheP.clear()
		Detach
	}
	
	
	// IMPLEMENTED	--------------------------
	
	override def current: Option[Reflection] = cacheP.value
	
	// Yields the cached value, if appropriate
	override def value: Reflection = cacheP.value.getOrElse {
		// Otherwise generates and caches the new value, and prepares a listener for resetting the cache
		val result = f(source.value)
		cacheP.setOne(result)
		source.addHighPriorityListener(resetCacheListener)
		result
	}
}
