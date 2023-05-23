package utopia.scribe.api.controller.logging

import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.scribe.api.controller.logging.Scribe.loggingQueue
import utopia.scribe.api.database.access.single.logging.issue.DbIssue
import utopia.scribe.api.util.ScribeContext._
import utopia.scribe.core.model.enumeration.Severity
import utopia.vault.util.DatabaseActionQueue

object Scribe
{
	// ATTRIBUTES   ---------------------
	
	private lazy val loggingQueue = {
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
case class Scribe(context: String, defaultSeverity: Severity = Severity.default, defaultDetails: String = "")
	extends Logger
{
	// IMPLEMENTED  -------------------------
	
	override def apply(error: Option[Throwable], message: String) = _apply(error, message)
	
	
	// OTHER    -----------------------------
	
	private def _apply(error: Option[Throwable], message: String,
	                   severity: Severity = defaultSeverity, variantDetails: String = defaultDetails) =
		loggingQueue.push { implicit c => DbIssue.store(context, error, message, severity, variantDetails) }
	
	/**
	  * @param severity Level of severity applicable to this issue
	  * @return Copy of this logger that uses the specified severity instead of the default severity
	  */
	def apply(severity: Severity) = VariantLogger(severity)
	/**
	  * @param severity Level of severity applicable to this issue
	  * @param details Details about this variant of issue
	  * @return Copy of this logger with the specified information
	  */
	def apply(severity: Severity, details: String) = VariantLogger(severity, details)
	
	/**
	  * @param details Details that separate this issue variant from the others
	  * @return Copy of this logger that logs the specified issue variant
	  */
	def variant(details: String) = VariantLogger(details = details)
	
	
	// NESTED   -----------------------------
	
	case class VariantLogger(severity: Severity = defaultSeverity, details: String = defaultDetails) extends Logger
	{
		// IMPLEMENTED  ---------------------
		
		override def apply(error: Option[Throwable], message: String) = _apply(error, message, severity, details)
		
		
		// OTHER    -------------------------
		
		def withSeverity(severity: Severity) = copy(severity = severity)
		def withDetails(details: String) = copy(details = details)
	}
}
