package utopia.logos.controller.app

import utopia.flow.async.context.ThreadPool
import utopia.flow.collection.immutable.Pair
import utopia.flow.time.TimeExtensions._
import utopia.flow.parse.AutoClose._
import utopia.flow.parse.json.{JsonParser, JsonReader}
import utopia.flow.util.Version
import utopia.flow.util.console.ConsoleExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.logos.database.LogosContext
import utopia.logos.database.store.DomainDb
import utopia.vault.database.{Connection, ConnectionPool, Tables}

import scala.io.StdIn

/**
 * A utility application for migrating between different Logos versions
 * @author Mikko Hilpinen
 * @since 25.11.2025, v0.7
 */
object MigrateLogos extends App
{
	// ATTRIBUTES   -----------------------
	
	private implicit val log: Logger = SysErrLogger
	private implicit val exc: ThreadPool = new ThreadPool("Logos-Migrate", coreSize = 1, maxIdleDuration = 15.seconds)
	private implicit val cPool: ConnectionPool = new ConnectionPool(maxConnections = 10, maxClientsPerConnection = 20)
	private implicit val jsonParser: JsonParser = JsonReader
	
	
	// APP CODE ---------------------------
	
	StdIn.readNonEmptyLine("Which Logos version are you migrating FROM?").foreach { fromVersionStr =>
		StdIn.readNonEmptyLine("Which Logos version are you migrating TO?").foreach { toVersionStr =>
			val fromTo = Pair(fromVersionStr, toVersionStr).map(Version.apply)
			if (fromTo.first >= fromTo.second)
				println(s"There's no update from ${ fromTo.mkString(" to ") }")
			else {
				val threshold = Version(0, 7)
				if (fromTo.isAsymmetricBy { _ >= threshold }) {
					val user = StdIn.readNonEmptyLine("Which user should we connect to MySQL with? (default = root)")
						.getOrElse("root")
					println("Please type the MySQL password to use")
					val password = StdIn.readLine()
					
					StdIn.readNonEmptyLine("Which database should we modify?").foreach { dbName =>
						Connection.modifySettings { _.copy(user = user, password = password,
							defaultDBName = Some(dbName)) }
						LogosContext.setup(exc, cPool, dbName, new Tables(cPool), jsonParser, log)
						
						new Connection(Some(dbName)).consume { implicit c =>
							println("Cleaning http:// prefixes from domains")
							DomainDb.cleanHttpPrefixes()
							println("Done")
						}
					}
				}
				else
					println("No updates required")
			}
		}
	}
}
