package utopia.scribe.core.controller.logging

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.AutoClose._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.file.KeptOpenWriter
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.{Now, Today}
import utopia.flow.util.StringExtensions._
import utopia.flow.util.Use
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.caching.DeprecatingLazy
import utopia.scribe.core.controller.logging.ConsoleScribe.timeFormat
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.enumeration.Severity.Warning

import java.io.PrintWriter
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.io.Codec

object ConsoleScribe
{
	// ATTRIBUTES   -------------------------
	
	private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss")
	
	
	// OTHER    -----------------------------
	
	/**
	  * Creates a new Scribe instance that writes issue data to the console
	  * (System.err for warnings and errors and System.out for info and debug information)
	  * @param context Root context for logging
	  * @param bundleDuration Duration during which recorded events should be bundled together (default = 5 seconds)
	  * @param logDirectory Directory where additional log files should be written (optional)
	  * @param backupLogger A logger implementation for handling possible file-write problems.
	  *                     Only necessary if 'logDirectory' has been specified.
	  *                     Default = System.err
	  * @param defaultSeverity Severity to assign to issues by default (default = Recoverable)
	  * @param defaultDetails Details to assign to issues by default (default = empty)
	  * @param exc Implicit execution context (only used for file operations)
	  * @return A new Scribe instance
	  */
	def apply(context: String, bundleDuration: Duration = 5.seconds, logDirectory: Option[Path] = None,
	          backupLogger: Logger = SysErrLogger.includingTime,
	          defaultSeverity: Severity = Severity.default, defaultDetails: Model = Model.empty)
	         (implicit exc: ExecutionContext) =
		new ConsoleScribe(context, bundleDuration, logDirectory, backupLogger, defaultSeverity, defaultDetails)
	
	/**
	  * Creates a new Scribe instance that writes issue data to the console
	  * (System.err for warnings and errors and System.out for info and debug information)
	  * and also copies entries to an external log file.
	  * @param logDirectory Directory where additional log files should be written
	  * @param context Root context for logging
	  * @param bundleDuration Duration during which recorded events should be bundled together (default = 5 seconds)
	  * @param backupLogger A logger implementation for handling possible file-write problems.
	  *                     Default = System.err
	  * @param defaultSeverity Severity to assign to issues by default (default = Recoverable)
	  * @param defaultDetails Details to assign to issues by default (default = empty)
	  * @param exc Implicit execution context
	  * @return A new Scribe instance
	  */
	def copyingToFile(logDirectory: Path, context: String, backupLogger: Logger = SysErrLogger.includingTime,
	                  bundleDuration: Duration = 5.seconds, defaultSeverity: Severity = Severity.default,
	                  defaultDetails: Model = Model.empty)(implicit exc: ExecutionContext) =
		new ConsoleScribe(context, bundleDuration, Some(logDirectory), backupLogger, defaultSeverity, defaultDetails)
}

/**
  * A Scribe implementation that simply logs entries to System.err or System.out
  * @author Mikko Hilpinen
  * @since 03/01/2024, v1.0.1
  */
class ConsoleScribe(override val context: String, bundleDuration: Duration = 5.seconds,
                    logDirectory: Option[Path] = None, backupLogger: Logger = SysErrLogger.includingTime,
                    override val defaultSeverity: Severity = Severity.default,
                    override val details: Model = Model.empty)
                   (implicit exc: ExecutionContext)
	extends Scribe
{
	// ATTRIBUTES   -------------------
	
	private val lastLogTimePointer = Volatile(Now.toLocalDateTime)
	private val fileWriter = logDirectory.map { dir =>
		implicit val codec: Codec = Codec.UTF8
		DeprecatingLazy {
			val date = Today.toLocalDate
			Use(backupLogger) { implicit l => KeptOpenWriter(dir/s"$date.txt", 10.seconds) -> date }
		} { _._2 == Today.toLocalDate }
	}
	
	
	// IMPLEMENTED  -------------------
	
	override def self = this
	
	override def withContext(context: String): Scribe = new Delegate(context, defaultSeverity, details)
	override def apply(details: Model, severity: Severity): Scribe = new Delegate(context, severity, details)
	
	override protected def _apply(error: Option[Throwable], message: String, occurrenceDetails: Model,
	                              severity: Severity, variantDetails: Model): Unit =
		_apply(context, error, message, occurrenceDetails, severity, variantDetails)
	
	
	// OTHER    ----------------------
	
	private def _apply(context: String, error: Option[Throwable], message: String, occurrenceDetails: Model,
	                   severity: Severity, variantDetails: Model) =
	{
		// Updates the last log time
		val (t, d) = lastLogTimePointer.mutate { last =>
			val t = Now.toLocalDateTime
			(last, t - last) -> t
		}
		
		// Logs to the console
		val out = if (severity >= Warning) System.err else System.out
		new PrintWriter(out).consume {
			writeWith(_, t, d, context, error, message, occurrenceDetails, severity, variantDetails)
		}
		
		// May also log to a file
		fileWriter.foreach { lazyWriter =>
			lazyWriter.value._1 { writer =>
				writeWith(writer, t, d, context, error, message, occurrenceDetails, severity, variantDetails)
			}.logFailure(backupLogger)
		}
	}
	
	private def writeWith(out: PrintWriter, time: LocalDateTime, duration: Duration, context: String,
	                      error: Option[Throwable], message: String,
	                      occurrenceDetails: Model, severity: Severity, variantDetails: Model) =
	{
		val baseHeader = s"$severity @$context${ message.prependIfNotEmpty(": ") }"
		
		// Checks whether to bundle the entries together
		if (error.isDefined || bundleDuration.finite.exists { duration > _ })
			out.println(s"\n${ timeFormat.format(time) }: $baseHeader")
		else
			out.println(s"$baseHeader (${ duration.description })")
		
		// Writes details, if applicable
		(variantDetails.properties.iterator ++ occurrenceDetails.properties).foreach { prop =>
			out.println(s"\t- ${prop.name}: ${prop.value}")
		}
		
		// Writes the stack trace, if applicable
		error.foreach { _.printStackTrace(out) }
	}
	
	
	// NESTED   ----------------------
	
	private class Delegate(override val context: String, override val defaultSeverity: Severity,
	                       override val details: Model)
		extends Scribe
	{
		override def self = this
		
		override def withContext(context: String) = new Delegate(context, defaultSeverity, details)
		override def apply(details: Model, severity: Severity) = new Delegate(context, severity, details)
		
		override protected def _apply(error: Option[Throwable], message: String, occurrenceDetails: Model,
		                              severity: Severity, variantDetails: Model) =
			ConsoleScribe.this._apply(context, error, message, occurrenceDetails, severity, variantDetails)
	}
}
