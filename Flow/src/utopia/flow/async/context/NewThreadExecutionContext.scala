package utopia.flow.async.context

import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.Volatile

import scala.concurrent.ExecutionContext

/**
 * This execution context creates a new thread for all operations. For most purposes a ThreadPool is a better choice
 * for execution context since it reuses threads. This context should be used for very short time and with
 * very few operations.
 * @author Mikko Hilpinen
 * @since 31.12.2019, v1.6.1
 */
class NewThreadExecutionContext(val name: String = "NewThreadExc")(implicit logger: Logger) extends ExecutionContext
{
	private val threadCounter = Volatile[Int](0)
	
	override def execute(runnable: Runnable) = {
		val nextIndex = threadCounter.updateAndGet { _ + 1 }
		val thread = new Thread(runnable)
		thread.setDaemon(true)
		thread.setName(s"$name-$nextIndex")
		thread.start()
	}
	
	override def reportFailure(cause: Throwable) = logger(cause)
}
