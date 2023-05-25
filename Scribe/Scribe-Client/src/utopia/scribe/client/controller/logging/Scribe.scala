package utopia.scribe.client.controller.logging

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
	  * @param master The primary Scribe logging & delivery system (implicit)
	  * @param version Implicit current software version
	  * @return A new Scribe instance
	  */
	def apply(context: String, defaultSeverity: Severity = Severity.default)
	         (implicit master: MasterScribe, version: Version): Scribe =
		apply(master, context, version, "", defaultSeverity)
}

/**
  * Used for logging (error) information and sending it over to the server
  * @author Mikko Hilpinen
  * @since 24.5.2023, v0.1
  */
case class Scribe(master: MasterScribe, context: String, version: Version, details: String, defaultSeverity: Severity)
	extends ScribeLike[Scribe]
{
	// IMPLEMENTED  ------------------------
	
	override def self = this
	
	def apply(details: String, severity: Severity) = copy(details = details, defaultSeverity = severity)
	
	override protected def _apply(error: Option[Throwable], message: String, severity: Severity, variantDetails: String) =
		master.accept(ClientIssue(version, context, severity, variantDetails, error.flatMap { RecordableError(_) },
			message))
}