package utopia.exodus.test.rest.servlet

import javax.servlet.annotation.MultipartConfig
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import utopia.access.http.Method
import utopia.access.http.Status.BadRequest
import utopia.bunnymunch.jawn.JsonBunny
import utopia.exodus.rest.resource.description.{DescriptionRolesNode, LanguagesNode, RolesNode, TasksNode}
import utopia.exodus.rest.resource.device.DevicesNode
import utopia.exodus.rest.resource.organization.OrganizationsNode
import utopia.exodus.rest.resource.user.UsersNode
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.DataType
import utopia.flow.parse.JsonParser
import utopia.flow.util.StringExtensions.ExtendedString
import utopia.nexus.http.{Path, ServerSettings}
import utopia.nexus.rest.RequestHandler
import utopia.vault.database.Connection
import utopia.vault.util.ErrorHandling
import utopia.vault.util.ErrorHandlingPrinciple.Custom
import utopia.nexus.servlet.HttpExtensions._

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
	Connection.modifySettings { _.copy(driver = Some("org.mariadb.jdbc.Driver")) }
	// TODO: Change this once more advanced logging systems are available and in production
	ErrorHandling.defaultPrinciple = Custom { _.printStackTrace() }
	
	
	// ATTRIBUTES	----------------------------
	
	// TODO: When going to production, read these from settings and maybe use parameter encoding
	private implicit val serverSettings: ServerSettings = ServerSettings("http://localhost:9999")
	private implicit val jsonParser: JsonParser = JsonBunny
	
	private val handler = new RequestHandler(
		Vector(UsersNode.forApiKey, DevicesNode, OrganizationsNode, LanguagesNode.public, DescriptionRolesNode.public,
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