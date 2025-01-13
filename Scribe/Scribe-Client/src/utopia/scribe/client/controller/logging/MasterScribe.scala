package utopia.scribe.client.controller.logging

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.annex.controller.{ApiClient, PersistedRequestHandler, PersistingRequestQueue, QueueSystem, RequestQueue}
import utopia.annex.model.request.{ApiRequest, Persisting}
import utopia.annex.model.response.RequestNotSent.RequestWasDeprecated
import utopia.annex.model.response.{RequestFailure, RequestResult}
import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.async.context.CloseHook
import utopia.flow.async.process.{Loop, PostponingProcess}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.{HasEnds, Span}
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model}
import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.flow.operator.Identity
import utopia.flow.parse.file.container.SaveTiming.OnlyOnTrigger
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.util.{Mutate, NotEmpty}
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.Changing
import utopia.scribe.core.controller.listener.MaximumLogLimitReachedListener
import utopia.scribe.core.model.cached.event.MaximumLogLimitReachedEvent
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.enumeration.Severity.Unrecoverable
import utopia.scribe.core.model.post.logging.ClientIssue

import java.nio.file.Path
import java.time.Instant
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

object MasterScribe
{
	// OTHER    --------------------------------
	
	/**
	  * Creates a new master scribe interface that periodically sends recorded issues to a server
	  * @param queueSystem               The request queue system used for sending the logging entries
	  * @param loggingEndpointPath       Path on the server to the logging end-point (for POST requests)
	  * @param backupLogger              Logging implementation to use when this scribe logging system fails
	  *                                  (default = print to sys-err)
	  * @param issueBundleDuration       The minimum and the maximum duration,
	  *                                  how long issues should be collected before sending them all to the server at once.
	  *                                  Default = 0s = No bundling will be performed.
	  * @param requestStoreLocation      Path to a json file where queued requests should be stored in case of a
	  *                                  continuous offline event.
	  *                                  None if requests shouldn't be persisted on the file system.
	  *                                  None will cause the logging data to be lost in case the software is closed during
	  *                                  an offline period.
	  *                                  Default = None.
	  * @param issueDeprecationDurations Durations that determine the time period after which an issue is no longer
	  *                                  necessary to send to the server.
	  *                                  A different value may be configured for each severity level.
	  *                                  By default, the issues will not deprecate.
	  * @param maxLogVelocity            Maximum number of issues logged and the time in which to reset the
	  *                                  issue counter.
	  *
	  *                                  E.g. if defined as (1000, 1.hours), the maximum is reached if over 1000
	  *                                  issues are recorded within the duration of a single hour.
	  *
	  *                                  After the maximum has been reached, no issues will be logged at all.
	  *
	  *                                  None if no maximum should be applied (default).
	  *
	  * @param modifyIssue               A function called for each recorded issue,
	  *                                  which may modify them before logging.
	  *                                  The function may, for example, append client environment information to
	  *                                  issue variant details.
	  *                                  By default, no modifications are made.
	  * @param exc Implicit execution system for asynchronous processes
	  *            (such as request sending and local file handling)
	  * @return A new master scribe interface instance
	  */
	def apply(queueSystem: QueueSystem, loggingEndpointPath: String, backupLogger: Logger = SysErrLogger,
	          issueBundleDuration: HasEnds[FiniteDuration] = Span.singleValue(Duration.Zero),
	          requestStoreLocation: Option[Path] = None,
	          issueDeprecationDurations: Map[Severity, Duration] = Map(),
	          maxLogVelocity: Option[(Int, Duration)] = None, modifyIssue: Mutate[ClientIssue] = Identity)
	         (implicit exc: ExecutionContext): MasterScribe =
		new _MasterScribe(queueSystem, loggingEndpointPath, backupLogger, issueBundleDuration, requestStoreLocation,
			issueDeprecationDurations, maxLogVelocity, modifyIssue)
	
	/**
	  * Creates a new master scribe backup implementation that never sends data to the server,
	  * but simply records any issues using the specified local logging implementation.
	  *
	  * This method is only intended to be used in scenarios where no server side connection may be established
	  * at all, where attempts to contact the server would always be futile.
	  *
	  * @param localLogging A local logging implementation
	  * @return A new master scribe backup implementation
	  */
	def localOnly(localLogging: Logger): MasterScribe = new BackupMasterScribe(localLogging)
	
	
	// NESTED   --------------------------------
	
	/**
	  * Provides an interface for logging errors and other issues.
	  * Sends the collected data to the server, periodically.
	  * @author Mikko Hilpinen
	  * @since 23.5.2023, v0.1
	  */
	private class _MasterScribe(queueSystem: QueueSystem, loggingEndpointPath: String,
	                            backupLogger: Logger = SysErrLogger,
	                            issueBundleDuration: HasEnds[FiniteDuration] = Span.singleValue(Duration.Zero),
	                            requestStoreLocation: Option[Path] = None,
	                            issueDeprecationDurations: Map[Severity, Duration] = Map(),
	                            maxLogVelocity: Option[(Int, Duration)],
	                            modifyIssue: Mutate[ClientIssue] = Identity)
	                           (implicit exc: ExecutionContext)
		extends MasterScribe
	{
		// ATTRIBUTES   --------------------------
		
		private implicit val log: Logger = backupLogger
		private implicit val jsonParser: JsonParser = JsonBunny
		
		// Pointer for tracking outgoing issue count/velocity (for limiting output)
		private val issueCounter = Volatile.eventful(0)
		private val firstIssueTimestampPointer = issueCounter.incrementalMap { _ => Now.toInstant } { (prev, event) =>
			if (event.newValue <= 1) Now.toInstant else prev
		}
		
		private val pendingIssuesPointer = Volatile.eventful.seq[(ClientIssue, Instant)]()
		
		private lazy val requestQueue: RequestQueue = requestStoreLocation match {
			case Some(path) =>
				val queue = PersistingRequestQueue(queueSystem, path, Single(RequestHandler),
					saveLogic = OnlyOnTrigger)
				// Sends and persists the requests on JVM shutdown
				CloseHook.registerAsyncAction {
					// Persists the request before and after its completion
					val sendFuture = sendPendingIssues()
					queue.persistRequests().flatMap { _ => sendFuture }.flatMap { _ => queue.persistRequests() }
				}
				queue
			case None => RequestQueue(queueSystem)
		}
		private lazy val sendIssuesProcess = PostponingProcess.by(issueBundleDuration) { sendPendingIssues() }
		
		private var limitListeners: Seq[MaximumLogLimitReachedListener] = Empty
		
		
		// INITIAL CODE ------------------------
		
		// Triggers the send process when issues are queued (may delay sending)
		pendingIssuesPointer.addContinuousListener { e =>
			if (e.newValue.nonEmpty)
				sendIssuesProcess.runAsync()
		}
		
		// Starts resetting the issue counter after the first issue has been recorded (optional feature)
		maxLogVelocity.foreach { case (maxCount, resetInterval) =>
			resetInterval.finite.foreach { resetInterval =>
				pendingIssuesPointer.once { _.nonEmpty } { _ =>
					Loop.regularly(resetInterval, waitFirst = true) {
						issueCounter.setIf { _ < maxCount }(0)
					}
				}
			}
		}
		
		
		// IMPLEMENTED  ------------------------
		
		/**
		  * Requests for an issue to be recorded and sent to the server
		  * @param issue An issue to record
		  */
		override def accept(issue: ClientIssue) = pendingIssuesPointer.update { pending =>
			// Checks whether the maximum logging velocity has been reached.
			// Prevents additional logging, if so.
			val (shouldLog, limitReachedEvent) = issueCounter.mutate { count =>
				// Case: Int.MaxValue log entries reached => Stops counting or resets counter
				if (count == Int.MaxValue) {
					if (maxLogVelocity.isDefined)
						(false, None) -> count
					else
						(true, None) -> 0
				}
				else {
					val nextCount = count + 1
					val result = maxLogVelocity match {
						case Some((max, _)) =>
							// Case: Maximum not reached => OK to log
							if (count < max)
								(true, None)
							// Case: Maximum just reached => Logs a warning, prevents logging
							else if (count == max) {
								val event = MaximumLogLimitReachedEvent(max,
									Span(firstIssueTimestampPointer.value, Now.toInstant))
								(false, Some(event))
							}
							// Case: Maximum reached earlier => Prevents logging
							else
								(false, None)
						// Case: No maximum defined => OK to log
						case None => (true, None)
					}
					result -> nextCount
				}
			}
			// Case: Normal logging
			if (shouldLog) {
				// Adds a new pending issue
				// If there already was a similar pending issue,
				// merges them together instead of adding a new entry altogether
				val now = Now.toInstant
				val modified = modifyIssue(issue)
				pending.mergeOrAppend(modified -> now) { _._1 ~== modified } { case ((existing, previousRecording), _) =>
					existing.repeated(modified.instances, now - previousRecording) -> now
				}
			}
			else
				limitReachedEvent match {
					// Case: Maximum reached -warning
					case Some(event) =>
						// Informs the listeners as well
						limitListeners.foreach { _.onLogLimitReached(event) }
						pending :+ (ClientIssue(issue.version, "MasterScribe.accept", Unrecoverable,
							message = "Maximum outgoing issue count reached. Stops logging.",
							occurrenceDetails = Model.from("limit" -> maxLogVelocity.map { _._1 })) -> Now)
					// Case: Logging prevented
					case None => pending
				}
		}
		
		override def sendPendingIssues() = NotEmpty(pendingIssuesPointer.popAll()) match {
			// Case: Issues to send => Sends them and records the result, if failure
			case Some(issues) => requestQueue.push(new PostIssuesRequest(issues)).map { handlePostResult(issues, _) }
			// Case: No issues to send => Resolves immediately
			case None => Future.successful(())
		}
		
		override def addLoggingLimitReachedListener(listener: MaximumLogLimitReachedListener): Unit = {
			if (!limitListeners.contains(listener))
				limitListeners :+= listener
		}
		
		
		// OTHER    ----------------------------
		
		private def handlePostResult(issues: Iterable[(ClientIssue, Instant)], result: RequestResult[Unit]) = {
			result match {
				// Case: Request was deprecated => Only logs locally (WET WET)
				case RequestWasDeprecated =>
					issues.foreach { case (issue, recorded) => log(issue.delayedBy(Now - recorded).toString) }
				// Case: Request-sending failed => Logs the original issues and the sending error using the backup logger
				case f: RequestFailure =>
					log(f.cause, s"Failed to send ${ issues.map { _._1.instances }.sum } occurrences of ${
						issues.size } issues over to the server")
					issues.foreach { case (issue, recorded) => log(issue.delayedBy(Now - recorded).toString) }
				// Case: Success => No action required
				case _ => ()
			}
		}
		
		
		// NESTED   ----------------------------
		
		private object RequestHandler extends PersistedRequestHandler
		{
			// IMPLEMENTED  -------------------------
			
			override def handle(requestModel: Model, queue: PersistingRequestQueue): Unit = {
				// Parses the issues from the request model
				// Logs possible parsing errors, whether critical or non-critical
				parseIssuesFrom(requestModel).log.filter { _.nonEmpty }.foreach { issues =>
					// Sends the issues to the server, logs possible failures
					queue.push(new PostIssuesRequest(issues)).foreach { handlePostResult(issues, _) }
				}
			}
			
			override def shouldHandle(requestModel: Model): Boolean = requestModel.contains("issues")
			
			
			// OTHER    -----------------------------
			
			private def parseIssuesFrom(model: AnyModel) =
				model("issues").getVector
					// Attempts to parse the issues
					.map { v =>
						val issueModel = v.getModel
						ClientIssue(issueModel).map { _ -> issueModel("lastUpdated").getInstant }
					}
					// Logs non-critical errors using the backup logger
					.toTryCatch
		}
		
		private class PostIssuesRequest(initialIssues: Seq[(ClientIssue, Instant)])
			extends ApiRequest[Unit] with Persisting
		{
			// ATTRIBUTES   --------------------
			
			private val remainingIssuesPointer = EventfulPointer(initialIssues)
			
			override lazy val persistingModelPointer: Changing[Option[Model]] = {
				// Updates the issue status before persisting them
				updateStoreDurations()
				deprecateOldIssues()
				remainingIssuesPointer.map { issues =>
					NotEmpty(issues).map { issues =>
						Model.from(
							"issues" -> issues
								.map { case (issue, lastUpdate) => issue.toModel + Constant("lastUpdated", lastUpdate) }
								.toVector
						)
					}
				}
			}
			
			
			// IMPLEMENTED  --------------------
			
			override def path: String = loggingEndpointPath
			override def method: Method = Post
			
			// Updates the store durations of all the issues
			override def body = Left(updateStoreDurations().map { _._1 }.toVector)
			
			// Removes the deprecated issues
			// Considers this request deprecated once all issues have deprecated
			override def deprecated: Boolean = deprecateOldIssues().isEmpty
			
			override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[Unit]] = prepared.send()
			
			
			// OTHER    ------------------------
			
			private def updateStoreDurations() =
				remainingIssuesPointer.updateAndGet { issues =>
					val now = Now.toInstant
					issues.map { case (issue, recorded) => issue.delayedBy(now - recorded) -> now }
				}
			
			private def deprecateOldIssues() = remainingIssuesPointer.updateAndGet {
				_.filter { case (issue, lastUpdate) =>
					issueDeprecationDurations.getOrElse(issue.severity, Duration.Inf).finite.forall { liveDuration =>
						(Now - lastUpdate + issue.storeDuration.start) < liveDuration
					}
				}
			}
		}
	}
	
	// TODO: Possibly add bundling feature to this implementation as well
	// TODO: Also, support Scribes also
	private class BackupMasterScribe(logger: Logger) extends MasterScribe
	{
		// Immediately relays the issues to the specified logger
		override def accept(issue: ClientIssue): Unit = logger(issue.toString)
		
		override def sendPendingIssues(): Future[Unit] = Future.successful(())
		
		override def addLoggingLimitReachedListener(listener: MaximumLogLimitReachedListener): Unit = ()
	}
}

/**
  * Provides an interface for logging errors and other issues.
  * @author Mikko Hilpinen
  * @since 23.5.2023, v0.1
  */
trait MasterScribe
{
	/**
	  * Requests for an issue to be recorded and sent to the server
	  * @param issue An issue to record
	  */
	def accept(issue: ClientIssue): Unit
	
	/**
	  * Sends any issues that are being bundled
	  * @return Future that resolves once the issues have been sent (or sending has failed)
	  */
	def sendPendingIssues(): Future[Unit]
	
	/**
	  * Adds a new listener to be informed in case of a log limit reached -event,
	  * i.e. when/if the maximum number of logging entries is reached within a specific time period.
	  * @param listener A listener to be informed in case of a limit reached -event
	  */
	def addLoggingLimitReachedListener(listener: MaximumLogLimitReachedListener): Unit
}