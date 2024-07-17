package utopia.annex.test

import utopia.access.http.Status
import utopia.annex.controller.{QueueSystem, RequestQueue}
import utopia.flow.async.context.ThreadPool
import utopia.flow.parse.json.{JsonParser, JsonReader}
import utopia.flow.time.WeekDays
import utopia.flow.time.WeekDays.MondayToSunday
import utopia.flow.util.logging.{Logger, SysErrLogger}

import scala.concurrent.ExecutionContext

/**
  * Sets up basic properties for tests with [[TestApiClient]]
  * @author Mikko Hilpinen
  * @since 17.07.2024, v1.8
  */
object TestClientContext
{
	Status.setup()
	
	implicit val logger: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("Test")
	implicit val weekdays: WeekDays = MondayToSunday
	implicit val jsonParser: JsonParser = JsonReader
	
	val client = TestApiClient()
	lazy val queueSystem = new QueueSystem(client)
	lazy val queue = RequestQueue(queueSystem)
}
