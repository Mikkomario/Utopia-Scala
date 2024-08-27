package utopia.disciple.test

import utopia.disciple.controller.RequestRateLimiter
import utopia.flow.async.process.Wait
import utopia.flow.test.TestContext._
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.async.Volatile

import scala.concurrent.Future

/**
  * Tests RequestRateLimiter
  * @author Mikko Hilpinen
  * @since 19.11.2021, v1.4.4
  */
object RequestRateLimiterTest extends App
{
	val limiter = new RequestRateLimiter(5, 2.seconds)
	val counter = Volatile(0)
	val waitLock = new AnyRef
	
	// Makes sure requests before limit break are performed immediately
	(1 to 5).foreach { i =>
		limiter.push { Future { counter.update { _ + 1 } } }
		Wait(0.02.seconds, waitLock)
		assert(counter.value == i)
	}
	
	// Makes sure the following requests are not performed immediately
	(1 to 2).foreach { _ =>
		limiter.push { Future { counter.update { _ + 1 } } }
		Wait(0.02.seconds, waitLock)
		assert(counter.value == 5)
	}
	
	// Makes sure those requests get performed eventually
	Wait(2.seconds, waitLock)
	assert(counter.value == 7)
	
	// Makes sure the following request is again performed immediately
	limiter.push { Future { counter.update { _ + 1 } } }
	Wait(0.02.seconds, waitLock)
	assert(counter.value == 8)
	
	println("Success...")
}
