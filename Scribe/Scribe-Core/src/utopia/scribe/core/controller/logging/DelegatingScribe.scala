package utopia.scribe.core.controller.logging

import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.Mutate
import utopia.flow.view.template.eventful.Changing
import utopia.scribe.core.model.enumeration.Severity

object DelegatingScribe
{
	/**
	 * @param delegatePointer A pointer that contains the wrapped scribe implementation
	 * @return A scribe that delegates logging to the current contents of the specified pointer
	 */
	def apply(delegatePointer: Changing[Scribe]) =
		delegatePointer.fixedValue.getOrElse { new DelegatingScribe(delegatePointer) }
}

/**
 * A pointer-based [[Scribe]] implementation
 * @author Mikko Hilpinen
 * @since 14.05.2026, v1.3
 */
class DelegatingScribe(delegatePointer: Changing[Scribe]) extends Scribe
{
	// IMPLEMENTED  ------------------------
	
	override def self: Scribe = this
	
	override def in(subContext: String): Scribe = map { _.in(subContext) }
	override def apply(severity: Severity): Scribe = map { _(severity) }
	override def variant(details: Model): Scribe = map { _.variant(details) }
	
	override def apply(error: Option[Throwable], message: String, details: Model): Unit =
		delegatePointer.value(error, message, details)
	
	
	// OTHER    ----------------------------
	
	private def map(f: Mutate[Scribe]) = DelegatingScribe(delegatePointer.map(f))
}
