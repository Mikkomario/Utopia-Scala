package utopia.scribe.core.test

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.ThreadPool
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.scribe.core.model.cached.logging.RecordableError

import scala.concurrent.Future
import scala.util.Failure

/**
  * Tests RecordableError forming and toString
  * @author Mikko Hilpinen
  * @since 7.7.2023, v1.0
  */
object RecordableErrorTest extends App
{
	def fail() = Failure(new IllegalStateException("Test"))
	
	val failure = fail()
	val error = RecordableError(failure.exception).get
	
	println(error)
	println()
	
	implicit val log: Logger = Logger { (error, message) =>
		error.foreach { e =>
			e.printStackTrace()
			println(RecordableError(e).get)
			println(message)
		}
	}
	implicit val threadPool: ThreadPool = new ThreadPool("Test")
	
	Future { throw new IllegalStateException("Testing") }.waitFor().log
	
	println("Done!")
}
