package utopia.scribe.api.controller.logging

import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.scribe.api.controller.logging.Scribe.loggingQueue
import utopia.scribe.api.database.access.single.logging.issue.DbIssue
import utopia.scribe.api.util.ScribeContext._
import utopia.scribe.core.controller.logging.ScribeLike
import utopia.scribe.core.model.cached.logging.RecordableError
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.post.logging.ClientIssue
import utopia.vault.util.DatabaseActionQueue

object Scribe
{
	// ATTRIBUTES   ---------------------
	
	private lazy val loggingQueue = {
		// TODO: Use a more sophisticated backup logger
		implicit val backupLogger: Logger = SysErrLogger
		DatabaseActionQueue()
	}
	
	
	// OTHER    -------------------------
	
	/**
	  * Records a client-side issue to the database (asynchronously)
	  * @param issue The issue to record
	  * @return Future resolving into the recorded issue
	  */
	def record(issue: ClientIssue) = loggingQueue.push { implicit c => DbIssue.store(issue) }
}

/**
  * A logging implementation that utilizes the Scribe features
  * @author Mikko Hilpinen
  * @since 22.5.2023, v0.1
  * @param context A string representation of the context in which this logger serves
  * @param defaultSeverity The default level of [[Severity]] recorded by this logger (default = Unrecoverable)
  * @param details Details that are included in the logging entries. Result in different issue variants.
  */
// TODO: Add logToConsole and logToFile -options (to ScribeContext) once the basic features have been implemented
// TODO: Once basic features have been added, consider adding an email integration or other trigger-actions
case class Scribe(context: String, defaultSeverity: Severity = Severity.default, details: Model = Model.empty)
	extends ScribeLike[Scribe]
{
	// IMPLEMENTED  -------------------------
	
	override def self = this
	
	override def apply(details: Model, severity: Severity) =
		copy(details = details, defaultSeverity = severity)
	
	override protected def _apply(error: Option[Throwable], message: String, severity: Severity, variantDetails: Model) =
		loggingQueue.push { implicit c =>
			DbIssue.store(context, error.flatMap(RecordableError.apply), message, severity, variantDetails)
		}
}
