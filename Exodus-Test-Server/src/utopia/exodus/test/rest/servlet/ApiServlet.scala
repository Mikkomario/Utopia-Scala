package utopia.exodus.test.rest.servlet

import javax.servlet.annotation.MultipartConfig
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import utopia.access.http.Method
import utopia.access.http.Status.BadRequest
import utopia.bunnymunch.jawn.JsonBunny
import utopia.exodus.rest.resource.description.{DescriptionRolesNode, LanguageFamiliaritiesNode, LanguagesNode, RolesNode, TasksNode}
import utopia.exodus.rest.resource.device.DevicesNode
import utopia.exodus.rest.resource.organization.OrganizationsNode
import utopia.exodus.rest.resource.user.UsersNode
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext
import utopia.flow.async.ThreadPool
import utopia.flow.generic.DataType
import utopia.flow.parse.JsonParser
import utopia.flow.util.StringExtensions.ExtendedString
import utopia.flow.util.FileExtensions._
import utopia.nexus.http.{Path, ServerSettings}
import utopia.nexus.rest.RequestHandler
import utopia.vault.database.{Connection, ConnectionPool}
import utopia.vault.util.ErrorHandling
import utopia.vault.util.ErrorHandlingPrinciple.Custom
import utopia.nexus.servlet.HttpExtensions._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
  * A servlet that serves the test environment api
  * @author Mikko Hilpinen
  * @since 24.11.2020, v1
  */
@MultipartConfig(
	fileSizeThreshold   = 1048576,  // 1 MB
	maxFileSize         = 10485760, // 10 MB
	maxRequestSize      = 20971520, // 20 MB
)
class ApiServlet extends HttpServlet
{
	// INITIAL CODE	----------------------------
	
	DataType.setup()
	
	private implicit val exc: ExecutionContext = new ThreadPool("Exodus-Test-Server").executionContext
	private implicit val connectionPool: ConnectionPool = new ConnectionPool()
	
	val dbSettings = JsonBunny.munchPath("settings/exodus-db-settings.json").map { _.getModel }
	
	ExodusContext.setup(exc, connectionPool,
		dbSettings.toOption.flatMap { _("db_name").string }.getOrElse("exodus-test")) { (error, message) =>
		println(message)
		error.printStackTrace()
	}
	Connection.modifySettings { _.copy(driver = Some("org.mariadb.jdbc.Driver")) }
	dbSettings match
	{
		case Success(settings) => Connection.modifySettings { _.copy(password = settings("password").getString) }
		case Failure(error) =>
			println("Database settings read failed (error below). Continues with no password and database name 'exodus-test'")
			error.printStackTrace()
	}
	// TODO: Change this once more advanced logging systems are available and in production
	ErrorHandling.defaultPrinciple = Custom { _.printStackTrace() }
	
	
	// ATTRIBUTES	----------------------------
	
	// TODO: When going to production, read these from settings and maybe use parameter encoding
	private implicit val serverSettings: ServerSettings = ServerSettings("http://localhost:9999")
	private implicit val jsonParser: JsonParser = JsonBunny
	
	private val handler = new RequestHandler(
		Vector(UsersNode.forApiKey, DevicesNode, OrganizationsNode, LanguagesNode.public,
			LanguageFamiliaritiesNode.public, DescriptionRolesNode.public,
			RolesNode, TasksNode),
		Some(Path("exodus", "api", "v1")), r => AuthorizedContext(r) { _.printStackTrace() })
	
	
	// IMPLEMENTED	----------------------------
	
	override def service(req: HttpServletRequest, resp: HttpServletResponse) =
	{
		// Default implementation doesn't support patch, so skips some validations from parent if possible
		if (Method.values.exists { _.name ~== req.getMethod })
			handleRequest(req, resp)
		else
			super.service(req, resp)
	}
	
	override def doGet(req: HttpServletRequest, resp: HttpServletResponse) = handleRequest(req, resp)
	
	override def doPost(req: HttpServletRequest, resp: HttpServletResponse) = handleRequest(req, resp)
	
	override def doPut(req: HttpServletRequest, resp: HttpServletResponse) = handleRequest(req, resp)
	
	override def doDelete(req: HttpServletRequest, resp: HttpServletResponse) = handleRequest(req, resp)
	
	
	// OTHER	--------------------------------
	
	private def handleRequest(request: HttpServletRequest, response: HttpServletResponse) =
	{
		request.toRequest match
		{
			case Some(parseRequest) =>
				val newResponse = handler(parseRequest)
				newResponse.update(response)
			case None => response.setStatus(BadRequest.code)
		}
	}
}