package utopia.exodus.test.rest.servlet

import utopia.bunnymunch.jawn.JsonBunny
import utopia.citadel.util.CitadelContext
import utopia.exodus.model.enumeration.ExodusScope._
import utopia.exodus.rest.resource.ExodusResources
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext
import utopia.flow.async.context.ThreadPool
import utopia.flow.collection.value.typeless.Model
import utopia.flow.generic.DataType
import utopia.flow.parse.JsonParser
import utopia.flow.util.FileExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.nexus.http.{Path, ServerSettings}
import utopia.nexus.rest.RequestHandler
import utopia.nexus.servlet.{ApiLogic, LogicWrappingServlet}
import utopia.vault.database.columnlength.ColumnLengthRules
import utopia.vault.database.{Connection, ConnectionPool}
import utopia.vault.util.ErrorHandling
import utopia.vault.util.ErrorHandlingPrinciple.Log

import java.nio.file.Paths
import javax.servlet.annotation.MultipartConfig
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
class ApiServlet extends LogicWrappingServlet
{
	// ATTRIBUTES   ----------------------------
	
	DataType.setup()
	
	private implicit val logger: Logger = SysErrLogger
	private implicit val exc: ExecutionContext = new ThreadPool("Exodus-Test-Server").executionContext
	private implicit val connectionPool: ConnectionPool = new ConnectionPool()
	private implicit val serverSettings: ServerSettings = ServerSettings("http://localhost:9999")
	private implicit val jsonParser: JsonParser = JsonBunny
	
	private val dbSettingsRead = JsonBunny.munchPath("settings/exodus-db-settings.json").map { _.getModel }
	private val dbSettings = dbSettingsRead.getOrElse(Model.empty)
	
	override lazy val logic = new ApiLogic(
		new RequestHandler(Map("v1" -> ExodusResources.all), Some(Path("exodus", "api")), r => AuthorizedContext(r)),
		Vector(r => {
			logger(s"${r.method} ${r.pathString}")
			r
		}),
		Vector(r => {
			logger(s"=> ${r.status}")
			r
		})
	)
	
	
	// INITIAL CODE	----------------------------
	
	ExodusContext.setup(exc, connectionPool,
		dbSettings("db_name", "db").stringOr("exodus_db")) {
		Set(ReadGeneralData, ReadPersonalData, PersonalActions, ReadOrganizationData, OrganizationActions,
			CreateOrganization, RequestPasswordReset, ChangeKnownPassword, TerminateOtherSessions, RevokeOtherTokens,
			JoinOrganization)
	}
	Connection.modifySettings { _.copy(driver = Some("org.mariadb.jdbc.Driver"), charsetName = "utf8",
		charsetCollationName = "utf8_general_ci") }
	
	dbSettingsRead match
	{
		case Success(settings) => Connection.modifySettings { _.copy(password = settings("password").getString) }
		case Failure(error) =>
			logger("Database settings read failed (error below). Continues with no password and database name 'exodus-test'")
			logger(error.getMessage)
	}
	ErrorHandling.defaultPrinciple = Log
	
	// Applies length rules
	Paths.get("length-rules/exodus")
		.iterateChildren { _.filter { _.fileType == "json" }
			.foreach { rules => ColumnLengthRules.loadFrom(rules, CitadelContext.databaseName) } }
}