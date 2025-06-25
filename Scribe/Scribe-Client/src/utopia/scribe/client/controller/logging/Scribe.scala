package utopia.scribe.client.controller.logging

import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.Version
import utopia.scribe.core.controller.logging.ConcreteScribeLike
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
case class Scribe(master: MasterScribe, context: String, version: Version, variantDetails: Model, severity: Severity)
	extends utopia.scribe.core.controller.logging.Scribe with ConcreteScribeLike[Scribe]
{
	// IMPLEMENTED  ------------------------
	
	override def self = this
	
	override def withContext(context: String): Scribe = copy(context = context)
	override def variant(details: Model): Scribe =
		if (details.isEmpty) this else copy(variantDetails = variantDetails ++ details)
	override def apply(severity: Severity): Scribe = if (this.severity == severity) this else copy(severity = severity)
	
	override def apply(error: Option[Throwable], message: String, details: Model): Unit =
		master.accept(ClientIssue(version, context, severity, variantDetails, error.flatMap { RecordableError(_) },
			message, details))
}