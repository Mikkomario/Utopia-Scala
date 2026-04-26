package utopia.flow.test

import utopia.flow.async.context.{Scheduler, ThreadPool}
import utopia.flow.time.WeekDays
import utopia.flow.time.WeekDays.MondayToSunday
import utopia.flow.util.logging.{Logger, TimedSysErrLogger}

import scala.concurrent.ExecutionContext

/**
  * Common implicit parameters for testing
  * @author Mikko Hilpinen
  * @since 24.7.2022, v1.16
  */
object TestContext
{
	implicit val log: Logger = TimedSysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("Test")
	implicit val scheduler: Scheduler = Scheduler.newInstance
	implicit val weekdays: WeekDays = MondayToSunday
}
