package utopia.scribe.core.controller.logging

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.operator.Identity
import utopia.flow.util.logging.Logger
import utopia.flow.util.StringExtensions._
import utopia.scribe.core.model.enumeration.Severity

import scala.util.{Failure, Success, Try}

/**
  * A mutable (!) interface that delegates logging to different Scribe or logging implementations.
  * Useful in situations where you would like to use a Scribe-based logging system in one of the components
  * used by the Scribe system, such as an ExecutionContext or Api (Annex, client side).
  *
  * @author Mikko Hilpinen
  * @since 11.7.2023, v1.0
  */
class Synagogue(defaultContext: String,
                override protected val defaultSeverity: Severity = Severity.default,
                defaultVariantDetails: Model = Model.empty, logLoggingFailures: Boolean = true)
	extends Scribe
{
	// ATTRIBUTES   --------------------------
	
	private var scribes = Vector[Scribe]()
	private var loggers = Vector[Logger]()
	
	
	// IMPLEMENTED  --------------------------
	
	override def self = this
	
	override protected def context = defaultContext
	override protected def details = defaultVariantDetails
	
	override def withContext(context: String): Scribe = ScribeDelegate(context, details, defaultSeverity)
	override def apply(details: Model, severity: Severity): Scribe = ScribeDelegate(context, details, severity)
	
	override protected def _apply(error: Option[Throwable], message: String, occurrenceDetails: Model,
	                              severity: Severity, variantDetails: Model): Unit =
		_apply(context, error, message, occurrenceDetails, severity, variantDetails)
	
	
	// OTHER    ----------------------------
	
	/**
	  * Registers a new Scribe implementation to be used in logging
	  * @param scribe The scribe implementation to use
	  * @param priority Whether this should be the top priority Scribe from now on.
	  *                   If false, this will be the lowest priority Scribe.
	  */
	def register(scribe: Scribe, priority: Boolean) = {
		if (priority)
			scribes = scribe +: scribes
		else
			scribes = scribes :+ scribe
	}
	/**
	  * Registers a new logger implementation to be used in logging
	  * @param logger   The logger implementation to use
	  * @param priority Whether this should be the top priority logger from now on.
	  *                 If false, this will be the lowest priority Scribe.
	  *
	  *                 Please note that Scribe implementations will always be prioritized
	  *                 over standard Logger implementations, regardless of this parameter.
	  */
	// WET WET
	def register(logger: Logger, priority: Boolean) = {
		if (priority)
			loggers = logger +: loggers
		else
			loggers = loggers :+ logger
	}
	
	private def _apply(context: String, error: Option[Throwable], message: String, occurrenceDetails: Model,
	                   severity: Severity, variantDetails: Model) =
	{
		// Delegates the logging to a Scribe instance, if possible
		// Catches any thrown errors
		val (scribeLoggingErrors, shouldUseStandardLogger) = logUsingAnyOf(scribes) { (scribe, isDelegated) =>
			// Appends a "delegated" detail in case this is not the primary logging implementation to use
			val actualDetails: Model = {
				if (isDelegated)
					occurrenceDetails + ("delegated" -> (true: Value))
				else
					occurrenceDetails
			}
			scribe.in(context)(variantDetails, severity)(error, message, actualDetails)
			
		} { _.in("Synagogue.log").info }
		
		// Case: Scribe-based logging was not possible => Delegates to a standard logger implementation
		if (shouldUseStandardLogger) {
			val (loggerLoggingErrors, useSysErr) = logUsingAnyOf(loggers) { (logger, isDelegated) =>
				val appliedMessage = {
					if (isDelegated)
						s"${message.mapIfNotEmpty { m => s"$m " }}(primary logging failed)"
					else
						message
				}
				logger(error, appliedMessage)
			}(Identity)
			
			// If even this failed, prints to System.error
			if (useSysErr) {
				System.err.println(s"$severity failure in $context")
				message.notEmpty.foreach { m => System.err.println(s"Message: $m") }
				val fullDetails = variantDetails ++ occurrenceDetails
				fullDetails.notEmpty.foreach { d => System.err.println(s"Details: $d") }
				error.foreach { _.printStackTrace() }
			}
			if (logLoggingFailures && loggerLoggingErrors.nonEmpty) {
				System.err.println(
					s"${loggerLoggingErrors.size} failures while attempting to log the error above (stack traces below)")
				loggerLoggingErrors.foreach { _.printStackTrace() }
			}
		}
		// Case: Scribe-based logging succeeded => May still log logging failures
		else if (logLoggingFailures && scribeLoggingErrors.nonEmpty) {
			val remainingErrors = loggers.foldLeft(scribeLoggingErrors) { (errors, logger) =>
				if (errors.nonEmpty)
					logLoggingFailures(logger, errors)
				else
					errors
			}
			if (remainingErrors.nonEmpty) {
				System.err.println(s"${remainingErrors.size} logging failures encountered (see stack traces below)")
				remainingErrors.foreach { _.printStackTrace() }
			}
		}
	}
	
	private def logUsingAnyOf[L](loggers: Iterable[L])
	                            (logPrimary: (L, Boolean) => Unit)
	                            (modifyForLoggingFailures: L => Logger) =
	{
		// Delegates further and further, until there is nothing more to log
		loggers.foldLeft(Vector[Throwable]() -> true) { case ((errors, shouldLog), logger) =>
			// Case: Logging is still to be done successfully
			if (shouldLog) {
				Try { logPrimary(logger, errors.nonEmpty) } match {
					// Case: Logging succeeded
					case Success(_) =>
						// Logs the previously encountered logging errors (optional feature)
						if (logLoggingFailures && errors.nonEmpty)
							logLoggingFailures(modifyForLoggingFailures(logger), errors) -> false
						else
							errors -> false
					// Case: Logging failed => Delegates further
					case Failure(error) => (errors :+ error) -> true
				}
			}
			// Case: There are still some logging errors to record
			else if (logLoggingFailures && errors.nonEmpty)
				logLoggingFailures(modifyForLoggingFailures(logger), errors) -> false
			// Case: Nothing more to log
			else
				errors -> false
		}
	}
	
	// Assumes a non-empty set of errors
	// Returns remaining logging failures
	private def logLoggingFailures(logger: Logger, errors: Vector[Throwable]) = {
		// Logs until logging fails
		val errorsIter = errors.iterator
		val (lastLogError, lastLogResult) = errorsIter
			.map { e => e -> Try { logger(e, "Failure during logging (recovered)") } }
			.collectTo { _._2.isFailure }.last
		lastLogResult match {
			// Case: All failures were logged successfully
			case Success(_) => Vector.empty
			// Case: Some or all failures couldn't be logged (delegates further)
			case Failure(loggingError) => lastLogError +: errorsIter.toVector :+ loggingError
		}
	}
	
	
	// NESTED   ----------------------------
	
	private case class ScribeDelegate(context: String, details: Model, defaultSeverity: Severity) extends Scribe
	{
		override def self = this
		override def withContext(context: String) = copy(context = context)
		
		override def apply(details: Model, severity: Severity) =
			copy(details = details, defaultSeverity = severity)
		override protected def _apply(error: Option[Throwable], message: String, occurrenceDetails: Model,
		                              severity: Severity, variantDetails: Model): Unit =
			Synagogue.this._apply(context, error, message, occurrenceDetails, severity, variantDetails)
	}
}
