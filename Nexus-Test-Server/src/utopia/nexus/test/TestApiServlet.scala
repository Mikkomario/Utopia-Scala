package utopia.nexus.test

import utopia.access.http.{Headers, Status}
import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.async.context.ThreadPool
import utopia.flow.collection.immutable.{Pair, Single}
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.Version
import utopia.flow.util.logging.{FileLogger, Logger, SysErrLogger}
import utopia.nexus.http.{Path, Request, Response, ServerSettings}
import utopia.nexus.rest.{PostContext, RequestHandler, Resource}
import utopia.nexus.result.UseRawXmlOrJson
import utopia.nexus.servlet.{ApiLogic, LogicWrappingServlet, ServletLogic}

import scala.concurrent.ExecutionContext
import scala.io.Codec

/*
@MultipartConfig(
	fileSizeThreshold   = 1048576,  // 1 MB
	maxFileSize         = 10485760, // 10 MB
	maxRequestSize      = 20971520, // 20 MB
)
 */
/**
  * An API for test request -handling
  * @author Mikko Hilpinen
  * @since 17.07.2024, v1.0
  */
class TestApiServlet extends LogicWrappingServlet
{
	// INITIAL CODE ---------------------------
	
	Status.setup()
	
	
	// ATTRIBUTES   ---------------------------
	
	implicit val codec: Codec = Codec.UTF8
	implicit val exc: ExecutionContext = new ThreadPool("Test-API", 1, 20, 30.seconds)(SysErrLogger)
	implicit val log: Logger = new FileLogger("log/test-api", 1.seconds, copyToSysErr = true)
	implicit val jsonParser: JsonParser = JsonBunny
	implicit val serverSettings: ServerSettings = ServerSettings("http://localhost:9999")
	
	private val slowNode = new SlowNode()
	private val resources = Map(
		versionedResources(1) { implicit v => Pair(new EchoNode(), slowNode) },
		versionedResources(2) { implicit v => Vector(new EchoNode(), slowNode, MethodNotAllowedNode) }
	)
	private val resultParser = UseRawXmlOrJson()
	private val requestHandler = RequestHandler(resources, Some(Path("test", "api"))) { req =>
		PostContext(req, resultParser)
	}
	
	override val logic: ServletLogic = new ApiLogic(requestHandler, Single(intercept), Single(postProcess))
	
	
	// OTHER    -------------------------------
	
	private def intercept(request: Request) = {
		log.apply(s"Received request: ${ request.method } ${ request.pathString }")
		request
	}
	private def postProcess(response: Response) = {
		log.apply(s"Sending out response: ${ response.status }")
		// Adds the date header
		response.mapHeaders { Headers.currentDateHeaders ++ _ }
	}
	
	private def versionedResources(versionNumber: Int)(makeResources: Version => Seq[Resource[PostContext]]) = {
		val version = Version(versionNumber)
		val resources = makeResources(version)
		s"v$versionNumber" -> resources
	}
}
