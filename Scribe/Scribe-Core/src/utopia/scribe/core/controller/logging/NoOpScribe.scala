package utopia.scribe.core.controller.logging
import utopia.flow.generic.model.immutable.Model
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.enumeration.Severity.Debug

/**
  * A Scribe implementation that doesn't perform any logging
  * @author Mikko Hilpinen
  * @since 24/03/2024, v1.0.2
  */
object NoOpScribe extends Scribe
{
	// IMPLEMENTED  ----------------------
	
	override def self = this
	
	override protected def context = ""
	override protected def defaultSeverity = Debug
	override protected def details = Model.empty
	
	override def withContext(context: String) = this
	override def apply(details: Model, severity: Severity) = this
	
	override protected def _apply(error: Option[Throwable], message: String, occurrenceDetails: Model,
	                              severity: Severity, variantDetails: Model) = ()
}
