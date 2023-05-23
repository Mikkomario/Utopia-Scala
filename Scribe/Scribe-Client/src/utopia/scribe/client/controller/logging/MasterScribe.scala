package utopia.scribe.client.controller.logging

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.annex.controller.{PersistedRequestHandler, PersistingRequestQueue, QueueSystem, RequestQueue}
import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.RequestNotSent.RequestSendingFailed
import utopia.annex.model.response.RequestResult
import utopia.bunnymunch.jawn.JsonBunny
import utopia.disciple.apache.Gateway
import utopia.disciple.http.request.Timeout
import utopia.flow.async.context.CloseHook
import utopia.flow.async.process.PostponingProcess
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.HasEnds
import utopia.flow.collection.mutable.VolatileList
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.generic.model.template.{ModelLike, Property}
import utopia.flow.parse.file.container.SaveTiming.OnlyOnTrigger
import utopia.flow.parse.json.{JsonParser, JsonReader}
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.NotEmpty
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
	// ATTRIBUTES   --------------------------
	
	// TODO: Use a more sophisticated logging implementation
	private implicit val backupLogger: Logger = SysErrLogger
	
	private lazy val gateway = Gateway(
		jsonParsers = Vector(JsonBunny, JsonReader),
		maxConnectionsPerRoute = 1,
		maxConnectionsTotal = 3,
		maximumTimeout = Timeout(30.minutes, 30.minutes, 6.hours)
	)
	
	/*
	private object ApiAccess extends Api
	{
		override protected implicit def log: Logger = MasterScribe.backupLogger
		
		override protected def gateway: Gateway = MasterScribe.gateway
		override protected def rootPath: String = apiPath
		override protected def headers: Headers = headersView.value
		
		override protected def makeRequestBody(bodyContent: Value): Body = StringBody.json(bodyContent.toJson)
	}
	 */
}

/**
  * Provides an interface for logging errors and other issues.
  * Sends the collected data to the server, periodically.
  * @author Mikko Hilpinen
  * @since 23.5.2023, v0.1
  */
class MasterScribe(queueSystem: QueueSystem, loggingEndpointPath: String, issueBundleDuration: HasEnds[FiniteDuration],
                   requestStoreLocation: Option[Path], issueDeprecationDurations: Map[Severity, Duration])
                  (implicit exc: ExecutionContext)
{
	// ATTRIBUTES   --------------------------
	
	import MasterScribe.backupLogger
	
	implicit val jsonParser: JsonParser = JsonBunny
	
	private val pendingIssuesPointer = VolatileList[(ClientIssue, Instant)]()
	
	private lazy val requestQueue = requestStoreLocation match {
		case Some(path) =>
			val (queue, errors) = PersistingRequestQueue(queueSystem, path, Vector(RequestHandler),
				saveLogic = OnlyOnTrigger)
			// Logs possible request parsing errors
			errors.headOption.foreach {
				backupLogger(_, s"Failed to restore ${errors.size} persisted request for sending issue data")
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
	
	
	// OTHER    ----------------------------
	
	/**
	  * Requests for an issue to be recorded and sent to the server
	  * @param issue An issue to record
	  */
	def accept(issue: ClientIssue) = pendingIssuesPointer :+= (issue -> Now)
	
	private def sendPendingIssues() = NotEmpty(pendingIssuesPointer.popAll()).foreach { issues =>
		requestQueue.push(new PostIssuesRequest(issues))
			.onComplete { res => handlePostResult(res.getOrMap(RequestSendingFailed)) }
	}
	
	private def handlePostResult(result: RequestResult) = result.toEmptyTry.logFailure
	
	
	// NESTED   ----------------------------
	
	private object RequestHandler extends PersistedRequestHandler
	{
		override def factory: FromModelFactory[ApiRequest] = PostIssuesRequest
		
		override def shouldHandle(requestModel: Model): Boolean = requestModel.contains("issues")
		override def handle(result: RequestResult): Unit = handlePostResult(result)
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
					(Now - lastUpdate + issue.storeDuration) < liveDuration
				}
			}
		}
	}
}
