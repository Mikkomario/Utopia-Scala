package utopia.trove.test

import utopia.flow.async.ThreadPool
import utopia.flow.generic.DataType
import utopia.flow.util.TimeLogger
import utopia.vault.database.ConnectionPool

import scala.concurrent.ExecutionContext

/**
  * Tests starting, running and stopping the database
  * @author Mikko Hilpinen
  * @since 22.9.2020, v1
  */
object StartDbTest extends App
{
	DataType.setup()
	val logger = new TimeLogger()
	
	implicit val exc: ExecutionContext = new ThreadPool("Trove-Test").executionContext
	implicit val connectionPool: ConnectionPool = new ConnectionPool()
	
	// TODO: Continue
}
