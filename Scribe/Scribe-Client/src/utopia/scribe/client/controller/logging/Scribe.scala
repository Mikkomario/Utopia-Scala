package utopia.scribe.client.controller.logging

import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.Version
import utopia.scribe.core.controller.logging.ScribeLike
import utopia.scribe.core.model.cached.logging.RecordableError
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.post.logging.ClientIssue

object Scribe
{
	/**
	  * Creates a new Scribe instance for logging
	  * @param context The context in which this instance serves
	  * @param defaultSeverity Default severity used when logging (default = unrecoverable failure)
	  * @param details Issue variant details to add to each recorded case (default = empty)
	  * @param master The primary Scribe logging & delivery system (implicit)
	  * @param version Implicit current software version
	  * @return A new Scribe instance
	  */
	def apply(context: String, defaultSeverity: Severity = Severity.default, details: Model = Model.empty)
	         (implicit master: MasterScribe, version: Version): Scribe =
		apply(master, context, version, details, defaultSeverity)
}

/**
  * Used for logging (error) information and sending it over to the server
  * @author Mikko Hilpinen
  * @since 24.5.2023, v0.1
  */
case class Scribe(master: MasterScribe, context: String, version: Version, details: Model, defaultSeverity: Severity)
	extends ScribeLike[Scribe]
{
	// IMPLEMENTED  ------------------------
	
	override def self = this
	
	def apply(details: Model, severity: Severity) = copy(details = details, defaultSeverity = severity)
	
	override protected def _apply(error: Option[Throwable], message: String, severity: Severity, variantDetails: Model) =
		master.accept(ClientIssue(version, context, severity, variantDetails, error.flatMap { RecordableError(_) },
			message))
}