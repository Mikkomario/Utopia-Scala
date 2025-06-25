package utopia.scribe.core.controller.logging

import utopia.flow.view.immutable.caching.Lazy

import scala.language.implicitConversions

object Scribe
{
	// COMPUTED    -------------------------
	
	/**
	  * @return Access to lazy scribe constructors
	  */
	def lazily = LazyScribe
	
	
	// IMPLICIT --------------------------
	
	/**
	  * Converts a lazily initialized Scribe instance container into a direct Scribe instance
	  * @param lazyScribe A lazily initialized instance
	  * @return A lazily initialized Scribe based on that container
	  */
	implicit def wrapLazy(lazyScribe: Lazy[Scribe]): Scribe = LazyScribe.wrap(lazyScribe)
}

/**
  * Common trait for ScribeLike implementations where 'Repr' is hidden.
  * Useful for situations where one simply needs some ScribeLike implementation.
  * @author Mikko Hilpinen
  * @since 29.6.2023, v1.0
  */
trait Scribe extends ScribeLike[Scribe]
