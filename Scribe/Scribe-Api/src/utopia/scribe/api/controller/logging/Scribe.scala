package utopia.scribe.api.controller.logging

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.{Loop, Process}
import utopia.flow.collection.immutable.range.Span
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.Volatile
import utopia.scribe.api.database.access.single.logging.issue.DbIssue
import utopia.scribe.api.util.ScribeContext
import utopia.scribe.api.util.ScribeContext._
import utopia.scribe.core.controller.listener.MaximumLogLimitReachedListener
import utopia.scribe.core.controller.logging.ScribeLike
import utopia.scribe.core.model.cached.event.MaximumLogLimitReachedEvent
import utopia.scribe.core.model.cached.logging.RecordableError
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.enumeration.Severity.Critical
import utopia.scribe.core.model.post.logging.ClientIssue
import utopia.vault.database.Connection
import utopia.vault.util.DatabaseActionQueue

import scala.concurrent.duration.{Duration, FiniteDuration}

object Scribe
{
	// ATTRIBUTES   ---------------------
	
	private lazy val loggingQueue = {
		implicit val backupLogger: Logger = ScribeContext.backupLogger
		DatabaseActionQueue()
	}
	// Counts the number of logging entries in order to apply a maximum limit
	private val logCounter = Volatile(0)
	// Maximum allowed logCounter value. None if not limited.
	private var logLimit: Option[Int] = None
	// Process for resetting the logging counter regularly
	// None until initialized
	// Also contains the repeat interval, in case it needs to be modified
	private var counterResetLoop: Option[(FiniteDuration, Process)] = None
	
	private var limitListeners = Vector[MaximumLogLimitReachedListener]()
	
	// Contains the latest timestamp when the counter reached 0 or 1
	private val firstLogTimePointer = logCounter.incrementalMap { _ => Now.toInstant } { (previous, event) =>
		if (event.newValue <= 1) Now.toInstant else previous
	}
	
	
	// OTHER    -------------------------
	
	/**
	  * Records a client-side issue to the database (asynchronously)
	  * @param issue The issue to record
	  */
	def record(issue: ClientIssue) = {
		log[Unit] { implicit c => DbIssue.store(issue) } { loggingError =>
			backupLogger(loggingError, "Failed to document a client-originated issue")
			backupLogger(issue.toString)
		}
	}
	
	/**
	  * Adds a new listener to be informed in case of a log limit reached -event,
	  * i.e. when/if the maximum number of logging entries is reached within a specific time period.
	  * @param listener A listener to be informed in case of a limit reached -event
	  */
	def addLoggingLimitReachedListener(listener: MaximumLogLimitReachedListener) = {
		if (!limitListeners.contains(listener))
			limitListeners = limitListeners :+ listener
	}
	
	/**
	  * Sets up a limit on how many logging entries are allowed within a specific time period.
	  * If this limit is reached, no more logging is performed
	  * (except for one entry indicating that this limit was reached).
	  *
	  * This function is normally called at ScribeContext.setup(...), and is not necessary to call afterwards.
	  * However, if you wish to override the previously set limit, you may use this method for that.
	  *
	  * @param maxLogCount The maximum allowed number of logging entries within 'resetInterval'
	  * @param resetInterval Duration after which the counter will be reset (may be infinite)
	  * @param resetAfterReached Whether the counter should be reset even after the maximum limit has been reached.
	  *
	  *                          If false, this logging system will stay locked after the limit has been reached
	  *                          once.
	  *                          If true, logging will only be denied temporarily, and will be enabled once
	  *                          every 'resetInterval'.
	  *
	  *                          Default = false.
	  */
	def setupLoggingLimit(maxLogCount: Int, resetInterval: Duration, resetAfterReached: Boolean = false) = {
		logLimit = Some(maxLogCount)
		resetInterval.finite match {
			// Case: Reset interval specified => Starts a reset process (unless identical process is already running)
			case Some(interval) =>
				if (counterResetLoop.forall { _._1 != interval }) {
					// Cancels the previous reset process, if applicable
					counterResetLoop.foreach { _._2.stop() }
					// Starts the new process
					implicit val logger: Logger = backupLogger
					val loop = Loop.regularly(interval) {
						logCounter.setIf { _ < maxLogCount || resetAfterReached }(0)
					}
					counterResetLoop = Some(interval -> loop)
				}
			// Case: Counter should never be reset => Cancels any existing reset process
			case None =>
				counterResetLoop.foreach { _._2.stop() }
				counterResetLoop = None
		}
	}
	
	// Performs the specified operation in the logging queue.
	// Follows maximum logging count
	private def log[U](f: Connection => U)(handleFailure: Throwable => U) = {
		// Denies logging if the maximum limit is reached
		val (shouldLog, limitReachEvent) = logCounter.pop { count =>
			// Case: Int.MaxValue logging entries => Resets the counter or denies logging
			if (count == Int.MaxValue) {
				if (logLimit.isDefined)
					(false, None) -> count
				else
					(true, None) -> 0
			}
			else {
				// Increases the counter by 1
				val increasedCount = count + 1
				val result = logLimit match {
					case Some(limit) =>
						// Case: Limit not yet reached => Continues
						if (count < limit)
							(true, None)
						// Case: Limit just reached => Denies logging and logs a warning instead
						else if (count == limit) {
							val event = MaximumLogLimitReachedEvent(limit,
								Span(firstLogTimePointer.value, Now.toInstant))
							(false, Some(event))
						}
						// Case: Limit reached before => Denies logging
						else
							(false, None)
					case None => (true, None)
				}
				result -> increasedCount
			}
		}
		
		if (shouldLog || limitReachEvent.isDefined)
			loggingQueue.push { implicit c =>
				limitReachEvent match {
					// Case: Maximum reached => Logs the warning
					case Some(event) =>
						DbIssue.store("Scribe.maximumReached", None,
							"Maximum number of logging entries was reached. Logging will not be performed anymore.",
							Critical, Model.empty,
							Model.from("limit" -> event.limit, "since" -> event.timeSpan.start))
					// Case: Allowed to log => Logs
					case None => f(c)
				}
			}.foreachFailure(handleFailure)
		
		// Informs assigned listeners about the limit reach, when appropriate
		limitReachEvent.foreach { event => limitListeners.foreach { _.onLogLimitReached(event) } }
	}
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
	extends ScribeLike[Scribe] with utopia.scribe.core.controller.logging.Scribe
{
	// IMPLEMENTED  -------------------------
	
	override def self = this
	
	override def withContext(context: String): Scribe = copy(context = context)
	override def apply(details: Model, severity: Severity) = copy(details = details, defaultSeverity = severity)
	
	override protected def _apply(error: Option[Throwable], message: String, details: Model, severity: Severity,
	                              variantDetails: Model) =
	{
		Scribe.log[Unit] { implicit c =>
			DbIssue.store(context, error.flatMap(RecordableError.apply), message, severity, variantDetails, details)
		} { loggingError =>
			// If logging fails, logs the original error and the logging failure using the backup logger
			ScribeContext.backupLogger(error, message)
			ScribeContext.backupLogger(loggingError, s"Logging failed in $context")
		}
	}
}
