package utopia.flow.test.async

import utopia.flow.async.context.ThreadPool
import utopia.flow.async.process.{Loop, SynchronizedLoops, WaitUtils}
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.async.process.WaitTarget.WaitDuration
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}

import java.time.LocalTime
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

/**
 * Tests loop synchronization
 * @author Mikko Hilpinen
 * @since 3.4.2020, v1.7
 */
@deprecated("Replaced with TimedTasks and their tests", "v1.15")
object SynchronizedLoopTest extends App
{
	
	implicit val logger: Logger = SysErrLogger
	
	class Printloop(text: String, time: Duration) extends Loop
	{
		override def runOnce() = println(s"$text - ${LocalTime.now().getSecond}")
		
		override def nextWaitTarget = WaitDuration(time)
	}
	
	val loops = new SynchronizedLoops(Vector(new Printloop("A(1)", 1.seconds),
		new Printloop("B(1)", 1.seconds), new Printloop("C(2)", 2.seconds)))
	
	implicit val exc: ExecutionContext = new ThreadPool("Test").executionContext
	loops.registerToStopOnceJVMCloses()
	loops.startAsync()
	
	WaitUtils.wait(10.seconds, new AnyRef)
}
