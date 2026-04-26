package utopia.echo.controller.client

import utopia.disciple.controller.Gateway
import utopia.disciple.model.request.Timeout
import utopia.flow.async.context.Scheduler
import utopia.flow.util.logging.Logger
import utopia.flow.time.TimeExtensions._

import scala.concurrent.ExecutionContext

object PiperClient
{
	/**
	 * Creates a new interface for sending requests to Piper
	 * @param serverAddress Address to the Piper server. Default = http://localhost:5000 - a locally running server
	 * @param log Implicit logging implementation
	 * @param exc Implicit execution context
	 * @param scheduler Implicit scheduler for timed events
	 * @return A new client interface
	 */
	def apply(serverAddress: String = "http://localhost:5000")
	         (implicit log: Logger, exc: ExecutionContext, scheduler: Scheduler) =
		using(Gateway(maximumTimeout = Timeout(3.minutes, 15.minutes)), serverAddress)
	
	/**
	 * Creates a new interface for sending requests to Piper
	 * @param gateway Utilized [[Gateway]] interface
	 * @param serverAddress Address to the Piper server. Default = http://localhost:5000 - a locally running server
	 * @param log Implicit logging implementation
	 * @param exc Implicit execution context
	 * @param scheduler Implicit scheduler for timed events
	 * @return A new client interface
	 */
	def using(gateway: Gateway, serverAddress: String = "http://localhost:5000")
	         (implicit log: Logger, exc: ExecutionContext, scheduler: Scheduler) =
		new PiperClient(gateway, serverAddress)
}

/**
 * An interface for sending requests to a Piper server
 * @author Mikko Hilpinen
 * @since 29.09.2025, v1.4
 */
class PiperClient(gateway: Gateway, serverAddress: String = "http://localhost:5000")
                 (implicit log: Logger, exc: ExecutionContext, scheduler: Scheduler)
	extends LlmServiceClient(gateway, serverAddress)