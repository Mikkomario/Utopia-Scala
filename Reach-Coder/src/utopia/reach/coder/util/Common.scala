package utopia.reach.coder.util

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.async.context.ThreadPool
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.{Logger, SysErrLogger}

import scala.concurrent.ExecutionContext

/**
  * Contains commonly used values
  * @author Mikko Hilpinen
  * @since 30.5.2023, v1.0
  */
object Common
{
	implicit val log: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("Reach-Coder", 2, 100)
	implicit val jsonParser: JsonParser = JsonBunny
}
