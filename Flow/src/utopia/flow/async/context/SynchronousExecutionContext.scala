package utopia.flow.async.context

import scala.concurrent.ExecutionContext

/**
  * An execution context that performs all operations synchronously. <b>Use with caution!</b>.
  * @author Mikko Hilpinen
  * @since 30.11.2020, v2
  */
object SynchronousExecutionContext extends ExecutionContext
{
	override def execute(runnable: Runnable) = runnable.run()
	
	override def reportFailure(cause: Throwable) = cause.printStackTrace()
}
