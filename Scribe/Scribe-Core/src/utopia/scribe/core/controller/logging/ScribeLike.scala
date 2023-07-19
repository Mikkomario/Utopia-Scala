package utopia.scribe.core.controller.logging

import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.operator.ScopeUsable
import utopia.flow.util.logging.Logger
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.enumeration.Severity.{Critical, Debug, Info, Recoverable, Unrecoverable, Warning}

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
	protected def details: Model
	
	/**
	  * @param context New context to assign
	  * @return Copy of this Scribe with the new context -property
	  */
	def withContext(context: String): Repr
	/**
	  * Creates a copy of this Scribe that serves in a specific sub-context
	  * @param details Details that differentiate the resulting Scribe instance from this one.
	  *                Different details result in different IssueVariants being created.
	  * @param severity Appropriate level of severity for this sub-context
	  * @return Copy of this instance that serves in the specified sub-context
	  */
	def apply(details: Model, severity: Severity): Repr
	
	/**
	  * Logs an error
	  * @param error Error to log (optional)
	  * @param message Message to record (optional)
	  * @param occurrenceDetails Details about this issue occurrence (optional)
	  * @param severity Issue severity level
	  * @param variantDetails Details about this issue variant
	  */
	protected def _apply(error: Option[Throwable] = None, message: String = "", occurrenceDetails: Model = Model.empty,
	                     severity: Severity = defaultSeverity, variantDetails: Model = details): Unit
	
	
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
	  * @return Copy of this instance that records warnings by default
	  */
	def warning = apply(Warning)
	/**
	  * @return Copy of this instance that records partial / recoverable failures by default
	  */
	def partialFailure = recoverable
	/**
	  * @return Copy of this instance that records partial / recoverable failures by default
	  */
	def recoverable = apply(Recoverable)
	/**
	  * @return Copy of this instance that records process unrecoverable failures by default
	  */
	def failure = unrecoverable
	/**
	  * @return Copy of this instance that records process unrecoverable failures by default
	  */
	def unrecoverable = apply(Unrecoverable)
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
	  * @param details Details about this specific issue occurrence (optional)
	  * @param severity Error severity (default = default severity of this instance)
	  * @param variantDetails Details about this issue variant / issue type (optional).
	  *                       Please note that different values will be recorded as different issue variants.
	  */
	def apply(error: Throwable, message: String = "", details: Model = Model.empty,
	          severity: Severity = defaultSeverity, variantDetails: Model = Model.empty) =
		_apply(Some(error), message, details, severity, this.details ++ variantDetails)
	/**
	  * Logs an error
	  * @param error Error to log
	  * @param severity Error severity
	  */
	def apply(error: Throwable, severity: Severity) = _apply(Some(error), severity = severity)
	/**
	  * Logs an error
	  * @param error The error to log (optional)
	  * @param message Additional message to log (may be empty)
	  * @param details Additional details to log (may be empty)
	  */
	def apply(error: Option[Throwable], message: String, details: Model) = _apply(error, message, details)
	/**
	  * Logs a message with details
	  * @param message Message to log
	  * @param details Details to include
	  */
	def apply(message: String, details: Model) = _apply(message = message, occurrenceDetails = details)
	/**
	  * Logs an entry with no message but some details instead
	  * @param details Details to log
	  */
	def apply(details: Model) = _apply(occurrenceDetails = details)
	
	/**
	  * @param subContext A sub-context within this Scribe instance's context
	  * @return Copy of this instance with a context modified so that it includes the specified sub-context
	  */
	def in(subContext: String): Repr = {
		if (subContext.isEmpty)
			self
		else {
			val c = context
			if (c.isEmpty)
				withContext(subContext)
			else {
				// Selects a separator appropriate for the current context
				val separator = {
					if (c.contains(' '))
						" "
					else if (c.contains('_'))
						"_"
					else
						"."
				}
				withContext(s"$c$separator$subContext")
			}
		}
	}
	/**
	  * Alias for [[in]]
	  */
	def /(subContext: String) = in(subContext)
	
	/**
	  * @param severity Level of severity applicable to this issue
	  * @return Copy of this logger that uses the specified severity instead of the default severity
	  */
	def apply(severity: Severity): Repr = if (severity == defaultSeverity) self else apply(details, severity)
	/**
	  * Creates a new variant of this instance with additional details
	  * @param details Details that separate this issue variant from the others.
	  *                These details are appended to the existing details in this Scribe instance.
	  * @return Copy of this logger that logs the specified issue variant
	  */
	def variant(details: Model) = if (details.isEmpty) self else apply(this.details ++ details, defaultSeverity)
	/**
	  * Creates a new variant of this instance with an additional detail.
	  * Please note that different details result in different issue variants.
	  * @param key The detail key to assign
	  * @param detail Value to assign for that detail (key)
	  * @return Copy of this logger that includes the specified detail in logging entries
	  */
	def variant(key: String, detail: Value) = apply(details + (key -> detail), defaultSeverity)
}
