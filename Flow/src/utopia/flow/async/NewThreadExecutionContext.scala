package utopia.flow.async

import scala.concurrent.ExecutionContext

/**
 * This execution context creates a new thread for all operations. For most purposes a ThreadPool is a better choice
 * for execution context since it reuses threads. This context should be used for very short time and with
 * very few operations.
 * @author Mikko Hilpinen
 * @since 31.12.2019, v1.6.1
 * @param errorHandler A function called when execution fails
 */
class NewThreadExecutionContext(val name: String = "NewThreadExc",
								val errorHandler: Throwable => Unit = _.printStackTrace()) extends ExecutionContext
{
	private val threadCounter = new Volatile[Int](0)
	
	override def execute(runnable: Runnable) =
	{
		val nextIndex = threadCounter.updateAndGet { _ + 1 }
		val thread = new Thread(runnable)
		thread.setDaemon(true)
		thread.setName(s"$name-$nextIndex")
		thread.start()
	}
	
	override def reportFailure(cause: Throwable) = errorHandler(cause)
}
