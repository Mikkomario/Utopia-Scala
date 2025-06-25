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
	  * @param subContext A sub-context within this Scribe instance's context
	  * @return Copy of this instance with a context modified so that it includes the specified sub-context
	  */
	def in(subContext: String): Repr
	/**
	  * @param severity Level of severity applicable to this issue
	  * @return Copy of this logger that uses the specified severity instead of the default severity
	  */
	def apply(severity: Severity): Repr
	/**
	  * Creates a new variant of this instance with additional details
	  * @param details Details that separate this issue variant from the others.
	  *                These details are appended to the existing details in this Scribe instance.
	  * @return Copy of this logger that logs the specified issue variant
	  */
	def variant(details: Model): Repr
	
	/**
	  * Logs an error
	  * @param error The error to log (optional)
	  * @param message Additional message to log (may be empty)
	  * @param details Additional details to log (may be empty)
	  */
	def apply(error: Option[Throwable], message: String, details: Model): Unit
	
	
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
	
	override def apply(error: Option[Throwable], message: String): Unit = apply(error, message, Model.empty)
	
	
	// OTHER    -----------------------------
	
	/**
	  * Alias for [[in]]
	  */
	def /(subContext: String) = in(subContext)
	/**
	  * Creates a new variant of this instance with an additional detail.
	  * Please note that different details result in different issue variants.
	  * @param key The detail key to assign
	  * @param detail Value to assign for that detail (key)
	  * @return Copy of this logger that includes the specified detail in logging entries
	  */
	def variant(key: String, detail: Value) = variant(Model.from(key -> detail))
	
	/**
	  * Logs an error
	  * @param error The error to log
	  * @param message Additional error message to record (optional)
	  * @param details Details about this specific issue occurrence (optional)
	  */
	def apply(error: Throwable, message: String = "", details: Model = Model.empty): Unit =
		apply(Some(error), message, details)
	/**
	  * Logs a message with details
	  * @param message Message to log
	  * @param details Details to include
	  */
	def apply(message: String, details: Model) = apply(None, message, details)
	/**
	  * Logs an entry with no message but some details instead
	  * @param details Details to log
	  */
	def apply(details: Model) = apply(None, "", details)
}
