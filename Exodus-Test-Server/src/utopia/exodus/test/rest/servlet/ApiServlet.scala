package utopia.exodus.test.rest.servlet

import javax.servlet.annotation.MultipartConfig
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import utopia.access.http.Method
import utopia.access.http.Status.BadRequest
import utopia.bunnymunch.jawn.JsonBunny
import utopia.citadel.util.CitadelContext
import utopia.exodus.model.enumeration.ExodusScope.{ChangeKnownPassword, CreateOrganization, JoinOrganization, OrganizationActions, PersonalActions, ReadGeneralData, ReadOrganizationData, ReadPersonalData, RequestPasswordReset, RevokeOtherTokens, TerminateOtherSessions}
import utopia.exodus.rest.resource.ExodusResources
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext
import utopia.flow.async.ThreadPool
import utopia.flow.datastructure.immutable.Model
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
import utopia.vault.database.columnlength.ColumnLengthRules

import java.nio.file.Paths
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
	
	val dbSettingsRead = JsonBunny.munchPath("settings/exodus-db-settings.json").map { _.getModel }
	val dbSettings = dbSettingsRead.getOrElse(Model.empty)
	
	ExodusContext.setup(exc, connectionPool,
		dbSettings("db_name", "db").stringOr("exodus_db")) { (error, message) =>
		println(message)
		error.printStackTrace()
	} { Set(ReadGeneralData, ReadPersonalData, PersonalActions, ReadOrganizationData, OrganizationActions,
		CreateOrganization, RequestPasswordReset, ChangeKnownPassword, TerminateOtherSessions, RevokeOtherTokens,
		JoinOrganization) }
	Connection.modifySettings { _.copy(driver = Some("org.mariadb.jdbc.Driver"), charsetName = "utf8",
		charsetCollationName = "utf8_general_ci") }
	dbSettingsRead match
	{
		case Success(settings) => Connection.modifySettings { _.copy(password = settings("password").getString) }
		case Failure(error) =>
			println("Database settings read failed (error below). Continues with no password and database name 'exodus-test'")
			println(error.getMessage)
	}
	ErrorHandling.defaultPrinciple = Custom { _.printStackTrace() }
	
	// Applies length rules
	Paths.get("length-rules/exodus")
		.iterateChildren { _.filter { _.fileType == "json" }
			.foreach { rules => ColumnLengthRules.loadFrom(rules, CitadelContext.databaseName) } }
	
	
	// ATTRIBUTES	----------------------------
	
	private implicit val serverSettings: ServerSettings = ServerSettings("http://localhost:9999")
	private implicit val jsonParser: JsonParser = JsonBunny
	
	private val handler = new RequestHandler(Map("v1" -> ExodusResources.all),
		Some(Path("exodus", "api")), r => AuthorizedContext(r))
	
	
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
		request.toRequest match {
			case Some(parseRequest) =>
				val newResponse = handler(parseRequest)
				println(s"Responded to ${parseRequest.method} ${
					parseRequest.path.map { _.toString }.getOrElse("") } with ${newResponse.status}\n")
				newResponse.update(response)
			case None =>
				println("Failed to parse request")
				response.setStatus(BadRequest.code)
		}
	}
}