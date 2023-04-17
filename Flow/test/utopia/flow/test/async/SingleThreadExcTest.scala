package utopia.flow.test.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.SingleThreadExecutionContext
import utopia.flow.async.process.Delay
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Tests SingleThreadExecutionContext
  * @author Mikko Hilpinen
  * @since 16.4.2023, v2.1
  */
object SingleThreadExcTest extends App
{
	implicit val log: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new SingleThreadExecutionContext("Test")
	
	def slowPrint(text: String) = Future {
		val lock = new AnyRef
		lock.synchronized {
			println("Waiting...")
			lock.wait(1000)
			println(text)
		}
	}
	
	slowPrint("1")
	slowPrint("2")
	slowPrint("3")
	
	Future { println("Success!") }.waitFor().get
}
