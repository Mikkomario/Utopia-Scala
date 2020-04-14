package utopia.flow.test

import java.time.LocalTime

import utopia.flow.async.{Loop, SynchronizedLoops, ThreadPool}
import utopia.flow.generic.DataType
import utopia.flow.util.WaitTarget.WaitDuration
import utopia.flow.util.TimeExtensions._
import utopia.flow.util.WaitUtils

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

/**
  * Tests loop synchronization
  * @author Mikko Hilpinen
  * @since 3.4.2020, v1.7
  */
object SynchronizedLoopTest extends App
{
	DataType.setup()
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
