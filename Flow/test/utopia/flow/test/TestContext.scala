package utopia.flow.test

import utopia.flow.async.context.ThreadPool
import utopia.flow.util.logging.{Logger, SysErrLogger}

import scala.concurrent.ExecutionContext

/**
  * Common implicit parameters for testing
  * @author Mikko Hilpinen
  * @since 24.7.2022, v1.16
  */
object TestContext
{
	implicit val logger: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("Test").executionContext
}
