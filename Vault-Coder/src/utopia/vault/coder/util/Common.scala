package utopia.vault.coder.util

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.async.context.ThreadPool
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.vault.database.ConnectionPool

import scala.concurrent.ExecutionContext

/**
  * Provides commonly used values
  * @author Mikko Hilpinen
  * @since 17.10.2022, v1.7.1
  */
object Common
{
	implicit val log: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("Vault-Coder", 2, 30, 20.seconds)
	implicit val connectionPool: ConnectionPool =
		new ConnectionPool(20, 4, 10.seconds)
	implicit val jsonParser: JsonParser = JsonBunny
}
