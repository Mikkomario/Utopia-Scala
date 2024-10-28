package utopia.vault.test.app

import utopia.flow.parse.json.{JsonParser, JsonReader}
import utopia.flow.util.console.ConsoleExtensions._
import utopia.flow.test.TestContext._
import utopia.flow.util.console.Console
import utopia.vault.context.VaultContext
import utopia.vault.database.{Connection, ConnectionPool, Tables}
import utopia.vault.util.console.ConsoleCommands

import scala.io.StdIn

/**
  * Provides a command line console for testing DB interactions
  * @author Mikko Hilpinen
  * @since 28.10.2024, v1.20.1
  */
object ConsoleTest extends App
{
	// APP CODE ------------------------
	
	// Sets up DB access
	private val user = StdIn.readNonEmptyLine("Please specify the database user (default = root)").getOrElse("root")
	println("Please specify the database password")
	private val password = StdIn.readLine()
	StdIn.readNonEmptyLine("Please specify the targeted database").foreach { dbName =>
		Connection.modifySettings { _.copy(user = user, password = password, defaultDBName = Some(dbName)) }
		implicit val cPool: ConnectionPool = new ConnectionPool()
		val tables = new Tables(cPool)
		implicit val vContext: VaultContext = VaultContext(exc, cPool, dbName, tables)
		val commands = new ConsoleCommands()
		implicit val jsonParser: JsonParser = JsonReader
		
		val console = Console.static(commands, "\nNext command", "exit")
		console.run()
	}
	
	println("Bye!")
}
