package utopia.annex.test

import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.RequestNotSent.RequestWasDeprecated
import utopia.annex.test.TestClientContext._
import utopia.flow.async.process.Wait
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._

import scala.concurrent.duration.Duration

/**
  * Tests [[utopia.annex.controller.RequestQueue]] by sending requests to Nexus Test Server
  * @author Mikko Hilpinen
  * @since 17.07.2024, v1.8
  */
object RequestQueueTest extends App
{
	// APP CODE ----------------------
	
	// Queues three slow requests. Each should take about 4 seconds to complete.
	private val requests = Vector(slowRequest(), slowRequest(), slowRequest(7.seconds))
	println("Sending out the requests...")
	private val resultFutures = requests.map { queue.push(_) }
	
	// Waits a while and makes sure none of these are yet completed, but that the first request (only) has been sent
	Wait(2.0.seconds)
	
	assert(resultFutures.forall { !_.isCompleted })
	assert(resultFutures.head.state.hasStarted)
	assert(resultFutures.view.tail.forall { _.state.hasNotStarted })
	
	// Waits for the first result, makes sure its ok
	private val firstResult = resultFutures.head.waitFor().get
	println("First response received")
	assert(firstResult.isSuccess)
	
	// Waits a second and makes sure the second request has fired but not completed
	// Third request should not be fired yet
	Wait(1.0.seconds)
	
	assert(resultFutures(1).state.hasStarted)
	assert(!resultFutures(1).isCompleted)
	assert(resultFutures.last.state.hasNotStarted)
	
	// Waits one second more. The situation should be the same.
	Wait(1.0.seconds)
	
	assert(!resultFutures(1).isCompleted)
	
	// Waits until second request resolves. Makes sure its ok.
	private val secondResult = resultFutures(1).waitFor().get
	println("Second response received")
	assert(secondResult.isSuccess)
	
	// Waits a short while and then makes sure the third request has been failed as deprecated
	Wait(0.2.seconds)
	
	assert(resultFutures.last.state.hasStarted)
	assert(resultFutures.last.isCompleted)
	
	private val thirdResult = resultFutures.last.waitFor().get
	println("Third result received")
	assert(thirdResult.isFailure)
	assert(thirdResult == RequestWasDeprecated)
	
	println("Success!")
	
	
	// OTHER    ----------------------
	
	// NB: Slow request delay is 4 seconds server-side
	private def slowRequest(deprecatesAfter: Duration = Duration.Inf) = {
		val testDeprecation = deprecatesAfter.finite match {
			case Some(duration) =>
				val threshold = Now + duration
				() => threshold.isPast
			case None => () => false
		}
		ApiRequest.get("slow", testDeprecation()) { _.send() }
	}
}
