package utopia.flow.test.event

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.Delay
import utopia.flow.async.process.ShutdownReaction.DelayShutdown
import utopia.flow.test.TestContext._
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.async.VolatileFlag

/**
 *
 * @author Mikko Hilpinen
 * @since 09.12.2025, v
 */
object VolatileFlagFutureTest extends App
{
	private val flag = VolatileFlag()
	println("Calling .future")
	private val future = flag.future
	println(".future returned")
	
	Delay.delayingShutdown(3.seconds) {
		println("Setting flag")
		flag.set()
	}
	
	println("Waiting on the flag")
	future.waitFor()
	
	println("Done!")
}
