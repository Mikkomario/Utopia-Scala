package utopia.echo.controller.vastai.vllm

import utopia.annex.controller.LockingRequestQueue
import utopia.annex.util.RequestResultExtensions._
import utopia.disciple.controller.Gateway
import utopia.disciple.model.request.Timeout
import utopia.echo.controller.client.{LlmServiceClient, VastAiApiClient}
import utopia.echo.controller.vastai.VastAiProcess
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.request.openai.ListOpenAiModels
import utopia.echo.model.request.vastai.{AcceptOffer, GetOffers}
import utopia.echo.model.unit.ByteCount
import utopia.echo.model.unit.ByteCountExtensions._
import utopia.echo.model.vastai.instance.SshConnection
import utopia.echo.model.vastai.instance.offer.OfferType.OnDemand
import utopia.echo.model.vastai.instance.offer.RunType.DirectSsh
import utopia.echo.model.vastai.instance.offer.{Offer, OfferProperty, OfferType, SearchFilter}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.ShutdownReaction.SkipDelay
import utopia.flow.async.process.{Delay, Process}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.operator.sign.Sign
import utopia.flow.time.Duration
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.util.result.TryExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.eventful.AssignableOnce

import scala.concurrent.{ExecutionContext, Future, Promise, TimeoutException}
import scala.sys.process
import scala.util.{Failure, Success, Try}

/**
 * A process for setting up and managing a vLLM server on a rented Vast AI instance
 * @author Mikko Hilpinen
 * @since 26.02.2026, v1.5
 */
// Template doc: https://cloud.vast.ai/template/readme/728eda674fd3c3810d227c9668d899bd
// Use the full model path (e.g., meta-llama/Llama-3.1-8B-Instruct)
class VastAiVllmProcess(vllmTemplateHashId: String, modelSize: ByteCount, additionalReservedDisk: ByteCount = 8.gb,
                        offerSearchFilters: Seq[SearchFilter] = Empty,
                        offerOrdering: Seq[(OfferProperty[_], Sign)] = Empty,
                        offerLimit: Option[Int] = None, offerType: OfferType = OnDemand,
                        gateway: => Gateway = Gateway(maxConnectionsPerRoute = 12, maxConnectionsTotal = 12,
	                        maximumTimeout = Timeout(connection = 60.seconds, read = 10.minutes, manager = 15.minutes),
	                        allowBodyParameters = false, allowJsonInUriParameters = false,
	                        disableTrustStoreVerification = true),
                        maxContextSize: Int = 8192,
                        gpuUtilizationRate: Double = 0.8, localPort: Int = 18000,
                        setupTimeout: Duration = Duration.infinite, statusCheckInterval: Duration = 15.seconds,
                        instanceLabel: String = "", imageCredentials: String = "", useHttps: Boolean = false)
                       (selectOffer: Seq[Offer] => Future[Try[Offer]])
                       (implicit exc: ExecutionContext, log: Logger, client: VastAiApiClient, llm: LlmDesignator)
	extends Process(shutdownReaction = Some(SkipDelay))
{
	// TODO: Add state tracking
	
	// ATTRIBUTES   ------------------------
	
	override protected val isRestartable: Boolean = false
	
	/**
	 * A pointer that will store the hosted vLLM API client, if one is successfully created.
	 */
	private val clientP = AssignableOnce[Try[LockingRequestQueue]]()
	
	private val vastAiProcess = VastAiProcess(statusCheckInterval) { hurryFlag =>
		// Requests for offers
		client.send(GetOffers(modelSize + additionalReservedDisk,
				offerSearchFilters, offerOrdering, offerLimit, offerType))
			// Selects one offer
			.tryFlatMap(selectOffer)
			.flatMapOrFail { offer =>
				// Accepts the offer, requesting a new instance
				// Assumes that vLLM template (or similar) is used
				val quantizationParam = {
					if (llm.llmName.contains("AWQ"))
						" --quantization awq_marlin"
					else
						""
				}
				client.send(AcceptOffer(offer.id, vllmTemplateHashId, runType = DirectSsh,
					env = Model.from(
						"VLLM_MODEL" -> llm.llmName,
						"VLLM_ARGS" -> s"--max-model-len $maxContextSize --gpu-memory-utilization $gpuUtilizationRate$quantizationParam"
					),
					label = instanceLabel, imageCredentials = imageCredentials, deprecatedView = hurryFlag,
					cancelIfUnavailable = true))
			}
			.toTryFuture
	}
	
	/**
	 * A future that resolves into either:
	 *      - Success: If the API is available for use
	 *      - Failure: If failed to set up the API
	 */
	val clientFuture = clientP.future
	
	
	// IMPLEMENTED  ----------------------
	
	override protected def runOnce(): Unit = {
		try {
			// Sets up a Vast AI instance and waits for it to load (or for timeout or stop() call)
			vastAiProcess.runAsync()
			val stopFuture = hurryFlag.future.map { _ => false }
			val timeoutOrStopFuture = {
				if (setupTimeout.isFinite)
					stopFuture.raceWith(Delay(setupTimeout)(false))
				else
					stopFuture
			}
			val instanceLoadedFuture = vastAiProcess.liveInstanceFuture.flatMap {
				// Case: Instance-acquisition succeeded => Checks when it's fully loaded
				case Success(instance) => instance.loadedFuture
				// Case: Instance-acquisition failed => Won't proceed
				case Failure(error) =>
					clientP.set(Failure(error))
					log(error, "Failed to acquire a Vast AI instance")
					Future.successful(false)
			}
			
			// Case: Instance successfully loaded => Sets up port-forwarding in order to access the vLLM API
			if (instanceLoadedFuture.raceWith(timeoutOrStopFuture).waitFor()
				.logWithMessage("Failure while waiting for instance to load, timeout or stop").getOrElse(false))
				vastAiProcess.instancePointerFuture.waitForResult()
					.flatMap { instancePointer =>
						instancePointer.value.ssh
							.toTry {
								new IllegalStateException("SSH connection is not available on the rented instance")
							}
							.flatMap { hostApi(_, timeoutOrStopFuture) }
					}
					// Logs failures and ensures that clientP receives a value
					.failure.foreach { error =>
						clientP.trySet(Failure(error))
						log(error, "Failure while attempting to host the vLLM API")
					}
			// Case: Instance failed to load => Completes the client pointer, if not already completed
			else
				clientP.trySet(Failure(new IllegalStateException("The Vast AI instance failed to load")))
		}
		finally {
			// Destroys the Vast AI instance
			vastAiProcess.stop().waitFor()
				.logWithMessage("Failure while waiting for the Vast AI instance to be destroyed")
		}
	}
	
	
	// OTHER    ---------------------
	
	/**
	 * Sets up and hosts the vLLM API by specifying clientP.
	 * Blocks extensively. Only returns on failure, or once stop() has been called and all pending requests completed.
	 * @param ssh Settings for the SSH connection
	 * @param setupTimeoutOrStopFuture A future that resolves if setup timeout is reached, or if stop() is called.
	 * @return Whether API-hosting (fully) succeeded
	 */
	private def hostApi(ssh: SshConnection, setupTimeoutOrStopFuture: Future[_]) = {
		// SSH port-forwarding is kept active as long as the API is used
		// TODO: Pass a ProcessLogger instance to run()
		val sshProcess = process.Process(s"ssh -N -L $localPort:localhost:18000 root@${ ssh.host } -p ${ ssh.port }")
			.run()
		try {
			// Creates the API client and waits until it's responsive (or until timeout is reached)
			val _gateway = gateway
			val vllmClient = new LlmServiceClient(_gateway,
				s"http${ if (useHttps) "s" else "" }://localhost:$localPort/v1",
				maxParallelRequests = _gateway.maxConnectionsPerRoute)
			waitUntilGetModelsSucceeds(vllmClient, setupTimeoutOrStopFuture) match {
				case Success(_) =>
					// The client is now usable. Stores it in a pointer, enabling external use.
					val exposedClient = LockingRequestQueue.wrap(vllmClient, hurryFlag)
					clientP.set(Success(exposedClient))
					
					// Keeps the SSH connection open, and the API exposed, until stop() is called
					// and until all pending requests complete
					exposedClient.stopFuture.waitForResult()
					
				// Case: Failed to acquire a working client
				case Failure(error) =>
					clientP.set(Failure(error))
					Failure(error)
			}
		}
		finally {
			// Stops the port-forwarding
			sshProcess.destroy()
		}
	}
	
	private def waitUntilGetModelsSucceeds(vllmClient: LlmServiceClient, setupTimeoutFuture: Future[_]) = {
		val resultPromise = Promise[Try[Unit]]()
		// Completes the promise on timeout
		setupTimeoutFuture.onComplete { _ =>
			if (!resultPromise.isCompleted)
				resultPromise.trySuccess(Failure(new TimeoutException(
					"The setup process timed out while waiting for the vLLM API to respond")))
		}
		// Also sets up a process for testing whether the API is usable,
		// possibly completing the result promise before the timeout
		tryCompletePromiseWhenModelsAreAvailable(vllmClient, resultPromise, View { resultPromise.isCompleted })
			.onComplete { _.logWithMessage("Unexpected failure while querying models in vLLM") }
		
		// Blocks until either the timeout is reached, or until a successful request has completed
		resultPromise.future.waitForResult()
	}
	/**
	 * Performs GET /models queries until a timeout is reached, or until models are found from the API response,
	 * indicating that it's usable.
	 * @param vllmClient Client to the tested API
	 * @param resultPromise A promise that will be completed on success
	 * @param completionView A view that contains true if a request should be retracted
	 * @return A future that resolves once this process completes
	 */
	private def tryCompletePromiseWhenModelsAreAvailable(vllmClient: LlmServiceClient,
	                                                     resultPromise: Promise[Try[Unit]],
	                                                     completionView: View[Boolean]): Future[Unit] =
	{
		// Case: Promise was already completed => Finishes
		if (resultPromise.isCompleted)
			Future.unit
		// Case: Process is still pending => Sends a GET /models request
		else
			vllmClient.push(ListOpenAiModels.withDeprecationView(completionView)).flatMap { result =>
				// Case: Models are available => Finishes successfully
				if (result.success.exists { _.nonEmpty }) {
					resultPromise.trySuccess(Success(()))
					Future.unit
				}
				// Case: No models are available yet => Attempts again after a while
				else
					Delay(statusCheckInterval) {
						tryCompletePromiseWhenModelsAreAvailable(vllmClient, resultPromise, completionView)
					}
			}
	}
}
