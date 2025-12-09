package utopia.disciple.test

import utopia.flow.async.AsyncExtensions._
import utopia.disciple.controller.RequestRateLimiter
import utopia.flow.async.process.Wait
import utopia.flow.test.TestContext._
import utopia.flow.time.Now
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
	
	println("Adding a lot of requests at once")
	val start = Now.toInstant
	val futures = (1 to 100).map { _ =>
		Wait(0.1.seconds)
		limiter.push { Future { counter.update { _ + 1 } } }
	}
	Wait(4.seconds)
	assert(counter.value >= 18)
	
	futures.iterator.zipWithIndex.foreach { case (future, i) =>
		future.waitFor().get
		println(s"$i completed")
	}
	val time = Now - start
	println(time.description)
	assert(time > 30.seconds, time.description)
	assert(time < 55.seconds, time.description)
	
	println("Success...")
}
