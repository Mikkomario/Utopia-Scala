package utopia.scribe.core.controller.logging

import utopia.flow.operator.ScopeUsable
import utopia.flow.util.logging.Logger
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.enumeration.Severity.{Critical, Debug, Info, Recoverable, Unrecoverable}

/**
  * Common trait for loggers on both the client and the server side
  * @author Mikko Hilpinen
  * @since 22.5.2023, v0.1
  * @tparam Repr Type of this logging implementation
  */
// TODO: Add logToConsole and logToFile -options (to ScribeContext) once the basic features have been implemented
trait ScribeLike[+Repr] extends Logger with ScopeUsable[Repr]
{
	// ABSTRACT -----------------------------
	
	/**
	  * @return The context where this Scribe instance is applied.
	  *         Should be unique. E.g. The name of the feature this Scribe performs the logging for.
	  */
	protected def context: String
	/**
	  * @return The default issue severity level that is logged
	  */
	protected def defaultSeverity: Severity
	/**
	  * @return Details used for differentiating between issue variants,
	  *         such as the name of the specific location or function where the logging is performed.
	  */
	protected def details: String
	
	/**
	  * Creates a copy of this Scribe that serves in a specific sub-context
	  * @param details Details that differentiate the resulting Scribe instance from this one.
	  *                Different details result in different IssueVariants being created.
	  * @param severity Appropriate level of severity for this sub-context
	  * @return Copy of this instance that serves in the specified sub-context
	  */
	def apply(details: String, severity: Severity): Repr
	
	/**
	  * Logs an error
	  * @param error Error to log (optional)
	  * @param message Message to record (optional)
	  * @param severity Issue severity level
	  * @param variantDetails Details about this issue variant
	  */
	protected def _apply(error: Option[Throwable] = None, message: String = "",
	                     severity: Severity = defaultSeverity, variantDetails: String = details): Unit
	
	
	// COMPUTED -----------------------------
	
	/**
	  * @return Copy of this instance that performs debug logging by default
	  */
	def debug = apply(Debug)
	/**
	  * @return Copy of this instance that only records neutral information by default
	  */
	def info = apply(Info)
	/**
	  * @return Copy of this instance that records partial / recoverable failures by default
	  */
	def partialFailure = apply(Recoverable)
	/**
	  * @return Copy of this instance that records process failures by default
	  */
	def failure = apply(Unrecoverable)
	/**
	  * @return Copy of this instance that records critical system failures by default
	  */
	def critical = apply(Critical)
	
	
	// IMPLEMENTED  -------------------------
	
	override def apply(error: Option[Throwable], message: String) = _apply(error, message)
	
	
	// OTHER    -----------------------------
	
	/**
	  * Logs an error
	  * @param error The error to log
	  * @param message Additional error message to record (optional)
	  * @param severity Error severity (default = default severity of this instance)
	  */
	def apply(error: Throwable, message: String = "", severity: Severity = defaultSeverity) =
		_apply(Some(error), message, severity, details)
	/**
	  * Logs an error
	  * @param error Error to log
	  * @param severity Error severity
	  */
	def apply(error: Throwable, severity: Severity) = _apply(Some(error), severity = severity)
	
	/**
	  * @param severity Level of severity applicable to this issue
	  * @return Copy of this logger that uses the specified severity instead of the default severity
	  */
	def apply(severity: Severity): Repr = apply(details, severity)
	/**
	  * @param details Details that separate this issue variant from the others
	  * @return Copy of this logger that logs the specified issue variant
	  */
	def variant(details: String) = apply(details, defaultSeverity)
}
