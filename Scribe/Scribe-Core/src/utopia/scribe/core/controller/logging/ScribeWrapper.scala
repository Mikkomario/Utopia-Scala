package utopia.scribe.core.controller.logging

import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.Mutate
import utopia.scribe.core.model.enumeration.Severity

/**
  * Common trait for Scribe implementations, which wrap another implementation
  * @author Mikko Hilpinen
  * @since 25.06.2025, v1.2
  */
trait ScribeWrapper extends Scribe
{
	// ABSTRACT ----------------------
	
	/**
	  * @return The wrapped Scribe implementation
	  */
	protected def wrapped: Scribe
	/**
	  * @param scribe A scribe instance to wrap
	  * @return A new wrapper, wrapping the specified instance
	  */
	protected def wrap(scribe: Scribe): Scribe
	
	
	// IMPLEMENTED  ------------------
	
	override def self: Scribe = this
	
	override def in(subContext: String): Scribe = map { _.in(subContext) }
	override def apply(severity: Severity): Scribe = map { _(severity) }
	override def variant(details: Model): Scribe = map { _.variant(details) }
	
	override def apply(error: Option[Throwable], message: String, details: Model): Unit =
		wrapped(error, message, details)
	
	
	// OTHER    ----------------------
	
	protected def map(f: Mutate[Scribe]) = wrap(f(wrapped))
}
