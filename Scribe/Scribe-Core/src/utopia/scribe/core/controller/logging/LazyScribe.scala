package utopia.scribe.core.controller.logging

import utopia.flow.util.Mutate
import utopia.flow.view.immutable.caching.Lazy

object LazyScribe
{
	/**
	  * @param initialize A scribe instance constructor
	  * @return A lazily initialized wrapper for that scribe instance
	  */
	def apply(initialize: => Scribe) = new LazyScribe(Lazy(initialize))
	/**
	  * @param lazyScribe A lazy scribe container
	  * @return A wrapper for that container.
	  *         If the specified scribe was already initialized, returns that.
	  */
	def wrap(lazyScribe: Lazy[Scribe]) = lazyScribe.current.getOrElse { new LazyScribe(lazyScribe) }
}

/**
  * A lazily initialized scribe implementation wrapper
  * @author Mikko Hilpinen
  * @since 24.06.2025, v1.1.3
  */
class LazyScribe(scribe: Lazy[Scribe]) extends ScribeWrapper
{
	// IMPLEMENTED  -------------------------
	
	override protected def wrapped: Scribe = scribe.value
	
	override protected def wrap(scribe: Scribe): Scribe = scribe
	
	// If the wrapped instance has been initialized, won't perform the mapping lazily
	override protected def map(f: Mutate[Scribe]) = scribe.current match {
		case Some(scribe) => f(scribe)
		case None => new LazyScribe(scribe.map(f))
	}
}
