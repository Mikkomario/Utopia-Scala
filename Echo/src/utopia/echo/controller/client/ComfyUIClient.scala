package utopia.echo.controller.client

import utopia.annex.util.RequestResultExtensions._
import utopia.disciple.controller.Gateway
import utopia.echo.controller.client.ComfyUIClient.waitInterval
import utopia.echo.model.request.comfyui.workflow.node.WorkflowNode
import utopia.echo.model.request.comfyui.{GetWorkResult, RequestWork}
import utopia.flow.async.TryFuture
import utopia.flow.async.process.Wait
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object ComfyUIClient
{
	private lazy val waitInterval = 1.seconds
}

/**
 * An API client used for interacting with a (local) ComfyUI server
 *
 * @author Mikko Hilpinen
 * @since 05.08.2025, v1.4
 */
class ComfyUIClient(gateway: Gateway = Gateway(), serverAddress: String = "http://localhost:8188",
                    clientId: String = RequestWork.defaultClientId, maxParallelRequests: Int = 4)
                   (implicit log: Logger, exc: ExecutionContext)
	extends LlmServiceClient(gateway, serverAddress, maxParallelRequests = maxParallelRequests,
		offlineWaitThreshold = 30.seconds)
{
	/**
	 * Requests a workflow to be performed, and waits for the results to arrive
	 * @param workFlow Workflow to run
	 * @param resultParser A parser for the result object's "outputs" property.
	 *                     [[GetWorkResult]] for predefined implementations.
	 * @param deprecationView A view that, once it contains true, causes requests to no longer be sent,
	 *                        effectively terminating the result-waiting process.
	 *                        Default = always false.
	 * @tparam A Type of successfully parsed results
	 * @return A future that yields the results once they're acquired.
	 *         May take a while to complete, and may yield a failure.
	 */
	def apply[A](workFlow: Iterable[WorkflowNode], resultParser: FromModelFactory[A],
	             deprecationView: View[Boolean] = AlwaysFalse) =
		push(new RequestWork(workFlow, clientId, deprecationView)).future.tryFlatMapSuccess { promptId =>
			waitForResult(new GetWorkResult[A](promptId, resultParser, deprecationView))
		}
	
	private def waitForResult[A](request: GetWorkResult[A]): Future[Try[A]] = {
		// Waits a while before looking for the results
		// Case: Wait completed => Performs a new look-up request
		if (Wait(waitInterval)) {
			push(request).future.tryFlatMapSuccess {
				// Case: Request completed with a result => Finishes
				case Some(result) => TryFuture.success(result)
				// Case: No results are available yet => Attempts again after a wait
				case None => waitForResult(request)
			}
		}
		// Case: Wait interrupted
		else
			TryFuture.failure(new InterruptedException("Wait for the request result was interrupted"))
	}
}