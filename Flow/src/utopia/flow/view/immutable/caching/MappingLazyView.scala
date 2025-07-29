package utopia.flow.view.immutable.caching

import utopia.flow.view.mutable.Pointer

import scala.annotation.unchecked.uncheckedVariance

/**
  * A lazily initialized container that maps the contents of another container.
  * The mapping itself is not performed lazily.
  * @author Mikko Hilpinen
  * @since 29.07.2025, v2.7
  */
class MappingLazyView[-O, +R](origin: Lazy[O])(f: O => R) extends Lazy[R]
{
	// ATTRIBUTES   ------------------------
	
	// Caches the map result once it is acquired
	private val valueP: Pointer[Option[R @uncheckedVariance]] = Pointer.empty
	
	
	// IMPLEMENTED  ------------------------
	
	override def current: Option[R] = valueP.value.orElse {
		origin.current.map { original => valueP.setOne(f(original)) }
	}
	override def value: R = valueP.setOneIfEmpty { f(origin.value) }
}
