package utopia.nexus.test

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.async.context.ThreadPool
import utopia.flow.collection.immutable.Pair
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{FileLogger, Logger, SysErrLogger}
import utopia.nexus.controller.api.ApiRoot
import utopia.nexus.controller.servlet.{ApiLogic, LogicWrappingServlet, ServletLogic}
import utopia.nexus.controller.write.ContentWriter.JsonOrXmlContentWriter
import utopia.nexus.model.request.StreamOrReader
import utopia.nexus.model.servlet.ParameterEncoding

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
	// ATTRIBUTES   ---------------------------
	
	implicit val codec: Codec = Codec.UTF8
	implicit val exc: ExecutionContext = new ThreadPool("Test-API", 1, 20, 30.seconds)(SysErrLogger)
	override implicit val logger: Logger = new FileLogger("log/test-api", 1.seconds, copyToSysErr = true)
	override implicit val jsonParser: JsonParser = JsonBunny
	override implicit val expectedParameterEncoding: ParameterEncoding = ParameterEncoding.none
	
	override val logic: ServletLogic = {
		val api = ApiRoot.newBuilder[NexusTestContext, StreamOrReader]("test/api", JsonOrXmlContentWriter()) {
			(request, version) => new NexusTestContext(request, version) }
		
		api ++= Pair(new EchoNode(), new SlowNode())
		api(2) ++= Pair(MethodNotAllowedNode, new StreamNode())
		
		api += TestInterceptor
		
		new ApiLogic(api.result())
	}
}
