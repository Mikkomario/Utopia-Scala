package utopia.scribe.api.controller.logging

import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.scribe.api.controller.logging.Scribe.loggingQueue
import utopia.scribe.api.database.access.single.logging.error_record.DbErrorRecord
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.enumeration.Severity.Unrecoverable
import utopia.vault.util.DatabaseActionQueue

object Scribe
{
	// ATTRIBUTES   ---------------------
	
	private lazy val loggingQueue = {
		import utopia.scribe.api.util.ScribeContext._
		// TODO: Use a more sophisticated backup logger
		implicit val backupLogger: Logger = SysErrLogger
		DatabaseActionQueue()
	}
}

/**
  * A logging implementation that utilizes the Scribe features
  * @author Mikko Hilpinen
  * @since 22.5.2023, v0.1
  * @param context A string representation of the context in which this logger serves
  * @param defaultSeverity The default level of [[Severity]] recorded by this logger (default = Unrecoverable)
  */
// TODO: Add logToConsole and logToFile -options (to ScribeContext) once the basic features have been implemented
// TODO: Once basic features have been added, consider adding an email integration or other trigger-actions
case class Scribe(context: String, defaultSeverity: Severity = Unrecoverable) extends Logger
{
	// IMPLEMENTED  -------------------------
	
	// TODO: Add parameter for additional details (String)
	def apply(error: Option[Throwable], message: String) = loggingQueue.push { implicit c =>
		// TODO: Implement by
		//  4) Pulling for existing issue + variant (make combined model),
		//  5) Inserting issues and variants where necessary,
		//  6) Recording a new occurrence
		// Extracts the stack trace elements from the error, if applicable,
		// and saves them (and the error) to the database
		val storedError = error.flatMap { DbErrorRecord.store(_) }
		
	}
}
