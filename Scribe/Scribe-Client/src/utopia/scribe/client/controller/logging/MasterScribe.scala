package utopia.scribe.client.controller.logging

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.annex.controller.{PersistedRequestHandler, PersistingRequestQueue, QueueSystem, RequestQueue}
import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.RequestNotSent.RequestSendingFailed
import utopia.annex.model.response.RequestResult
import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.async.context.CloseHook
import utopia.flow.async.process.PostponingProcess
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.{HasEnds, Span}
import utopia.flow.collection.mutable.VolatileList
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.generic.model.template.{ModelLike, Property}
import utopia.flow.operator.Identity
import utopia.flow.parse.file.container.SaveTiming.OnlyOnTrigger
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.{Mutate, NotEmpty}
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.mutable.Pointer
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.post.logging.ClientIssue

import java.nio.file.Path
import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Try

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
	          issueDeprecationDurations: Map[Severity, Duration] = Map(), modifyIssue: Mutate[ClientIssue] = Identity)
	         (implicit exc: ExecutionContext): MasterScribe =
		new _MasterScribe(queueSystem, loggingEndpointPath, backupLogger, issueBundleDuration, requestStoreLocation,
			issueDeprecationDurations, modifyIssue)
	
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
	private class _MasterScribe(queueSystem: QueueSystem, loggingEndpointPath: String, backupLogger: Logger = SysErrLogger,
	                            issueBundleDuration: HasEnds[FiniteDuration] = Span.singleValue(Duration.Zero),
	                            requestStoreLocation: Option[Path] = None,
	                            issueDeprecationDurations: Map[Severity, Duration] = Map(),
	                            modifyIssue: Mutate[ClientIssue] = Identity)
	                           (implicit exc: ExecutionContext)
		extends MasterScribe
	{
		// ATTRIBUTES   --------------------------
		
		private implicit val log: Logger = backupLogger
		private implicit val jsonParser: JsonParser = JsonBunny
		
		private val pendingIssuesPointer = VolatileList[(ClientIssue, Instant)]()
		
		private lazy val requestQueue: RequestQueue = requestStoreLocation match {
			case Some(path) =>
				val (queue, errors) = PersistingRequestQueue(queueSystem, path, Vector(RequestHandler),
					saveLogic = OnlyOnTrigger)
				// Logs possible request parsing errors
				errors.headOption.foreach {
					backupLogger(_, s"Failed to restore ${ errors.size } persisted request for sending issue data")
				}
				// Persists the requests on JVM shutdown
				CloseHook.registerAsyncAction {
					sendPendingIssues()
					queue.persistRequests()
				}
				queue
			case None => RequestQueue(queueSystem)
		}
		private lazy val sendIssuesProcess = PostponingProcess.by(issueBundleDuration) { sendPendingIssues() }
		
		
		// INITIAL CODE ------------------------
		
		// Triggers the send process when issues are queued (may delay sending)
		pendingIssuesPointer.addContinuousListener { e =>
			if (e.newValue.nonEmpty)
				sendIssuesProcess.runAsync()
		}
		
		
		// IMPLEMENTED  ------------------------
		
		/**
		  * Requests for an issue to be recorded and sent to the server
		  * @param issue An issue to record
		  */
		override def accept(issue: ClientIssue) = pendingIssuesPointer.update { pending =>
			// Adds a new pending issue
			// If there already was a similar pending issue,
			// merges them together instead of adding a new entry altogether
			val now = Now.toInstant
			val modified = modifyIssue(issue)
			pending.mergeOrAppend(modified -> now) { _._1 ~== issue } { case ((existing, previousRecording), _) =>
				existing.repeated(modified.instances, now - previousRecording) -> now
			}
		}
		
		
		// OTHER    ----------------------------
		
		private def sendPendingIssues() = NotEmpty(pendingIssuesPointer.popAll()).foreach { issues =>
			requestQueue.push(new PostIssuesRequest(issues))
				.onComplete { res => handlePostResult(issues, res.getOrMap(RequestSendingFailed)) }
		}
		
		private def handlePostResult(issues: Vector[(ClientIssue, Instant)], result: RequestResult) = {
			result.toEmptyTry.failure.foreach { error =>
				// On send failure, records the failure itself, as well as any issues that couldn't be sent
				log(error, s"Failed to send ${issues.map { _._1.instances }.sum} occurrences of ${
					issues.size} issues over to the server")
				issues.foreach { case (issue, recorded) => log(issue.delayedBy(Now - recorded).toString) }
			}
		}
		
		
		// NESTED   ----------------------------
		
		private object RequestHandler extends PersistedRequestHandler
		{
			override def factory: FromModelFactory[ApiRequest] = PostIssuesRequest
			
			override def shouldHandle(requestModel: Model): Boolean = requestModel.contains("issues")
			override def handle(result: RequestResult): Unit = handlePostResult(Vector(), result)
		}
		
		private object PostIssuesRequest extends FromModelFactory[PostIssuesRequest]
		{
			override def apply(model: ModelLike[Property]): Try[PostIssuesRequest] =
				model("issues").getVector
					// Attempts to parse the issues
					.map { v =>
						val issueModel = v.getModel
						ClientIssue(issueModel).map { _ -> issueModel("lastUpdated").getInstant }
					}
					// Logs non-critical errors using the backup logger
					.toTryCatch.logToTry
					// Converts to a request
					.map { new PostIssuesRequest(_) }
		}
		
		private class PostIssuesRequest(initialIssues: Vector[(ClientIssue, Instant)]) extends ApiRequest
		{
			// ATTRIBUTES   --------------------
			
			private val remainingIssuesPointer = Pointer(initialIssues)
			
			
			// IMPLEMENTED  --------------------
			
			override def path: String = loggingEndpointPath
			
			override def method: Method = Post
			
			// Updates the store durations of all the issues
			override def body: Value = updateStoreDurations().map { _._1 }
			
			// Removes the deprecated issues
			// Considers this request deprecated once all issues have deprecated
			override def isDeprecated: Boolean = deprecateOldIssues().nonEmpty
			
			override def persistingModel: Option[Model] = {
				// Updates the issue status before persisting them
				updateStoreDurations()
				val issues = deprecateOldIssues()
				Some(Model.from(
					"issues" -> issues.map { case (issue, lastUpdate) =>
						issue.toModel + Constant("lastUpdated", lastUpdate)
					}
				))
			}
			
			
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
	private class BackupMasterScribe(logger: Logger) extends MasterScribe
	{
		// Immediately relays the issues to the specified logger
		override def accept(issue: ClientIssue): Unit = logger(issue.toString)
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
}