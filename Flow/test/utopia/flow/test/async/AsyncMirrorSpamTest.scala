package utopia.flow.test.async

import utopia.flow.async.process.Wait
import utopia.flow.async.AsyncExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.test.TestContext._
import utopia.flow.view.mutable.Pointer

import scala.concurrent.Future
import scala.util.Random

/**
  * Tests uncontrolled asynchronous mapping, trying to break the pointer
  * @author Mikko Hilpinen
  * @since 29.04.2025, v2.6
  */
object AsyncMirrorSpamTest extends App
{
	private val delayPerUnit = 0.01.seconds
	private val testDelayPerUnit = 0.002.seconds
	
	private def test() = {
		val origin = Pointer.eventful(20)
		val mapped = origin.mapAsync(-1) { i =>
			Wait(i * delayPerUnit)
			i
		}
		var lastAssigned = -1
		Iterator
			.continually {
				val newValue = Random.nextInt(50)
				origin.value = newValue
				lastAssigned = newValue
				Wait(newValue * testDelayPerUnit)
			}
			.take(100).foreach { _ => () }
		
		Wait(delayPerUnit * 100)
		assert(origin.value == lastAssigned)
		assert(mapped.value.isNotProcessing)
		assert(mapped.value.current == lastAssigned)
	}
	
	println("Running tests once")
	test()
	
	println("\nRunning 20 tests in parallel")
	Vector.fill(20) { Future { test() } }.foreach { _.waitFor().get }
	println("Done!")
}
