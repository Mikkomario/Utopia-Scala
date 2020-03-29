package utopia.vault.test

import utopia.flow.async.ThreadPool
import utopia.flow.generic.DataType
import utopia.flow.util.TimeExtensions._
import utopia.vault.database.ConnectionPool
import utopia.vault.sql.SelectAll

import scala.concurrent.ExecutionContext

/**
 * Tests connection shutdown on system exit
 * @author Mikko Hilpinen
 * @since 31.12.2019, v1.4
 */
object ConnectionPoolShutdownTest extends App
{
	DataType.setup()
	implicit val exc: ExecutionContext = new ThreadPool("Vault-Test").executionContext
	val cPool = new ConnectionPool(25, 5, 10.seconds)
	cPool { implicit connection =>
		println(connection(SelectAll(TestTables.person)))
	}
	cPool { implicit connection =>
		println(connection(SelectAll(TestTables.strength)))
	}
	// println("Stopping connection pool")
	// cPool.stop().waitFor().get
	println("Shutting down")
}
