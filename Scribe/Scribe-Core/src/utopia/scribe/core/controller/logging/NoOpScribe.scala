package utopia.scribe.core.controller.logging

import utopia.flow.generic.model.immutable.Model
import utopia.scribe.core.model.enumeration.Severity

/**
  * A Scribe implementation that doesn't perform any logging
  * @author Mikko Hilpinen
  * @since 24/03/2024, v1.0.2
  */
object NoOpScribe extends Scribe
{
	// IMPLEMENTED  ----------------------
	
	override def self = this
	
	override def in(subContext: String): Scribe = this
	override def apply(severity: Severity): Scribe = this
	override def variant(details: Model): Scribe = this
	
	override def apply(error: Option[Throwable], message: String, details: Model): Unit = ()
}
