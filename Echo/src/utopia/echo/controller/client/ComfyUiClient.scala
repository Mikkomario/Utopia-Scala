package utopia.echo.controller.client

import utopia.annex.model.response.RequestNotSent.RequestSendingFailed
import utopia.annex.model.response.{RequestFailure, RequestResult, Response}
import utopia.disciple.controller.Gateway
import utopia.echo.controller.client.ComfyUiClient.waitInterval
import utopia.echo.model.comfyui.request.{GetWorkResult, RequestWork}
import utopia.echo.model.comfyui.workflow.node.WorkflowNode
import utopia.flow.async.process.Wait
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import scala.concurrent.{ExecutionContext, Future}

object ComfyUiClient
{
	private lazy val waitInterval = 1.seconds
}

/**
 * An API client used for interacting with a (local) ComfyUI server
 *
 * @author Mikko Hilpinen
 * @since 05.08.2025, v1.4
 */
class ComfyUiClient(gateway: Gateway = Gateway(), serverAddress: String = "http://localhost:8188",
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
		push(new RequestWork(workFlow, clientId, deprecationView)).future.flatMap {
			case Response.Success(promptId, _, _) =>
				waitForResult(new GetWorkResult[A](promptId, resultParser, deprecationView))
			case failure: RequestFailure => Future.successful(failure)
		}
	
	private def waitForResult[A](request: GetWorkResult[A]): Future[RequestResult[A]] = {
		// Waits a while before looking for the results
		// Case: Wait completed => Performs a new look-up request
		if (Wait(waitInterval))
			push(request).future.flatMap {
				case success: Response.Success[Option[A]] =>
					success.value match {
						// Case: Request completed with a result => Finishes
						case Some(result) => Future.successful(success.withValue(result))
						// Case: No results are available yet => Attempts again after a wait
						case None => waitForResult(request)
					}
				case failure: RequestFailure => Future.successful(failure)
			}
		// Case: Wait interrupted
		else
			Future.successful(
				RequestSendingFailed(new InterruptedException("Wait for the request result was interrupted")))
	}
}