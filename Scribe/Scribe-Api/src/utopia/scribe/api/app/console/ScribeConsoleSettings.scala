package utopia.scribe.api.app.console

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.async.context.ThreadPool
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.AppConfig
import utopia.flow.util.console.ConsoleExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.scribe.api.database.access.logging.issue.AccessIssues
import utopia.vault.database.{Connection, ConnectionPool}

import java.nio.file.Path
import scala.io.StdIn

/**
  * An interface for user-specified settings (json-based)
  * @author Mikko Hilpinen
  * @since 25.5.2023, v0.1
  */
object ScribeConsoleSettings
{
	// ATTRIBUTES   ----------------
	
	/**
	 * JSON parser used during console sessions
	 */
	implicit val jsonParser: JsonParser = JsonBunny
	/**
	 * Execution context to use in the console
	 */
	implicit val threadPool: ThreadPool = new ThreadPool("Scribe-Console", 3, 100, 10.seconds)(SysErrLogger)
	/**
	 * DB connection pool to use in the console.
	 * Please use this only if [[setupDb]] yields a success.
	 */
	implicit val cPool: ConnectionPool = new ConnectionPool(30)
	
	/**
	 * App configuration file access
	 */
	private val config = {
		implicit val log: Logger = SysErrLogger
		AppConfig("scribe", allowWorkingDirectoryAsAppDirectory = true).logToTry
	}
	
	
	// COMPUTED --------------------
	
	/**
	  * @return The directory where errors should be logged. None if no directory has been specified.
	  */
	def logDirectory = config.toOption.flatMap { _("log_directory", "log").string.map { s => s: Path } }
	/**
	 * @param directory Directory where file-logging should be performed
	 * @return Success or failure, depending on whether the app configuration could be updated
	 */
	def logDirectory_=(directory: Path) = config.map { _("log_directory") = directory.toJson }
	
	/**
	  * @return Name of the database to connect to when using Scribe features
	  */
	def dbName = config.flatMap { _("database:name").tryString }
	/**
	 * @param dbName Name of the database to use
	 * @return Success or failure, depending on whether the app configuration could be updated
	 */
	def dbName_=(dbName: String) = {
		Connection.modifySettings { _.copy(defaultDBName = Some(dbName)) }
		config.map { _("database:name") = dbName }
	}
	
	
	// OTHER    --------------------
	
	/**
	  * Initializes database connection settings and tests database accessibility
	  * @param allowUserInteraction Whether this application should request DB access settings from the user,
	 *                             if they haven't been specified
	 * @return Success containing the name of the targeted database, or a failure.
	 *         If failure, the database can't be accessed.
	  */
	def setupDb(allowUserInteraction: Boolean = false) = {
		config.flatMap { config =>
			val defaultConnectionTarget = "jdbc:mariadb://localhost:3306/"
			// Attempts to load previously specified settings
			val loadResult = config("database").tryModel.flatMap { db =>
				db("password").tryString.map { password =>
					val dbName = db("name").stringOr("scribe_db")
					Connection.modifySettings { _.copy(
						connectionTarget = db("address").stringOr(defaultConnectionTarget),
						user = db("user").stringOr("root"), password = password, defaultDBName = Some(dbName))
					}
					dbName
				}
			}
			// Requests settings from the user, if necessary & possible
			val setupResult = {
				if (loadResult.isSuccess || !allowUserInteraction ||
					!StdIn.ask("Database access settings have not been specified. Are you able to specify them now?",
						default = true))
					loadResult
				else {
					// Requests DB access settings
					val address = StdIn.readNonEmptyLine(
						s"Please specify the DB connection address. Default = $defaultConnectionTarget")
						.getOrElse(defaultConnectionTarget)
					val user = StdIn.readNonEmptyLine(
						"Please specify the user to connect to the DB with. Default = root.").getOrElse("root")
					StdIn.readNonEmptyLine("Please specify the password to access the DB with",
						"Not specifying a password will cancel this process. Please specify one.")
						.toTry { new IllegalArgumentException("DB password was not specified") }
						.map { password =>
							val dbName = StdIn.readNonEmptyLine(
								"Please specify the name of the used database. Default = scribe_db.")
								.getOrElse("scribe_db")
							
							// Updates DB access settings
							Connection.modifySettings { _.copy(
								connectionTarget = address, user = user, password = password,
								defaultDBName = Some(dbName))
							}
							
							// Stores the acquired settings
							config("database") = Model.from("address" -> address, "name" -> dbName,
								"user" -> user, "password" -> password)
							
							dbName
						}
				}
			}
			setupResult.flatMap { dbName =>
				// Tests DB access
				cPool.tryWith { implicit c =>
					AccessIssues.nonEmpty
					dbName
				}
			}
		}
	}
	@deprecated("Renamed to setupDb(Boolean)", "v1.2")
	def initializeDbSettings(allowUserInteraction: Boolean = false) = setupDb(allowUserInteraction)
}
