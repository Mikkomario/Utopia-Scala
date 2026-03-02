package utopia.echo.controller.vastai.vllm

import utopia.annex.controller.LockingRequestQueue
import utopia.annex.model.response.Response
import utopia.annex.util.RequestResultExtensions._
import utopia.disciple.controller.Gateway
import utopia.disciple.model.request.Timeout
import utopia.echo.controller.client.{LlmServiceClient, VastAiApiClient}
import utopia.echo.controller.vastai.{SelectOffer, VastAiProcess}
import utopia.echo.model.request.openai.ListOpenAiModels
import utopia.echo.model.request.vastai.{AcceptOffer, AttachSshKey, GetOffers, GetSshKeys}
import utopia.echo.model.response.openai.OpenAiModelInfo
import utopia.echo.model.unit.ByteCount
import utopia.echo.model.unit.ByteCountExtensions._
import utopia.echo.model.vastai.instance.offer.RunType.DirectSsh
import utopia.echo.model.vastai.instance.{InstanceStatus, SshConnection, VastAiInstance}
import utopia.echo.model.vastai.process.VastAiVllmProcessState._
import utopia.echo.model.vastai.process.{ApiHostingResult, VastAiVllmProcessState}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.ShutdownReaction.SkipDelay
import utopia.flow.async.process.{Delay, Loop, Process, Wait}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.file.FileUtils
import utopia.flow.parse.string.StringFrom
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.{Duration, Now}
import utopia.flow.util.EitherExtensions.Sided
import utopia.flow.util.Env
import utopia.flow.util.logging.Logger
import utopia.flow.util.result.TryExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.{AssignableOnce, MayBeAssignedOnce}

import java.nio.file.Path
import scala.concurrent.{ExecutionContext, Future, Promise, TimeoutException}
import scala.sys.process
import scala.util.{Failure, Success, Try}

/**
 * A process for setting up and managing a vLLM server on a rented Vast AI instance
 * @param imageOrVllmTemplateHashId Either:
 *                                      - Left: Used image
 *                                      - Right: Hash ID of the used template
 *
 *                                  Whichever is used, the implementation must handle vLLM hosting.
 * @param env Applied environment variables (such as maximum context length)
 * @param selectOffer Logic for selecting a Vast AI instance offer
 * @param modelSize Size of the used model. Used for calculating the reserved disk space.
 * @param additionalReservedDisk Additional disk space to reserve, beyond the model size. Default = 5 GB.
 * @param startCommands Commands to run once the instance is set up. Default = empty.
 * @param gateway Gateway instance to use for connecting to the rented instance.
 *                Default = new Gateway with 12 max connections.
 * @param localPort Port used for accessing the vLLM API locally. Default = 18000.
 * @param setupTimeout Timeout for the setup process.
 *                     If the API doesn't become usable before this timeout, the instance is destroyed.
 *                     Default = infinite (not recommended).
 * @param noResponseTimeout Timeout for started API requests.
 *                          If this timeout is reached, the request queue is closed
 *                          and the underlying Vast AI instance is destroyed.
 *                          Default = infinite (not recommended, unless you have your own monitoring process in place).
 * @param statusCheckInterval Interval between instance pointer / check updates. Default = 30 seconds.
 * @param instanceLabel Custom label given to the rented Vast AI instance. Default = empty.
 * @param imageCredentials Credentials for authorizing the use of the selected Docker image. Default = empty.
 * @param useHttps Whether to use HTTPS when connecting to the instance. Default = false.
 * @author Mikko Hilpinen
 * @since 26.02.2026, v1.5
 */
// TODO: Add separate timeout for individual status phases (i.e. if keeps at same status for >X minutes, fail)
// FIXME: vLLM port might be 8000 and not 18000?
class VastAiVllmProcess(imageOrVllmTemplateHashId: Sided[String], env: Model, selectOffer: SelectOffer,
                        modelSize: ByteCount, additionalReservedDisk: ByteCount = 5.gb,
                        startCommands: Seq[String] = Empty,
                        gateway: => Gateway = Gateway(maxConnectionsPerRoute = 12, maxConnectionsTotal = 12,
	                        maximumTimeout = Timeout(connection = 60.seconds, read = 10.minutes, manager = 15.minutes),
	                        allowBodyParameters = false, allowJsonInUriParameters = false,
	                        disableTrustStoreVerification = true),
                        localPort: Int = 18000, setupTimeout: Duration = Duration.infinite,
                        noResponseTimeout: Duration = Duration.infinite, statusCheckInterval: Duration = 30.seconds,
                        instanceLabel: String = "", imageCredentials: String = "", useHttps: Boolean = false)
                       (implicit exc: ExecutionContext, log: Logger, client: VastAiApiClient)
	extends Process(shutdownReaction = Some(SkipDelay))
{
	// ATTRIBUTES   ------------------------
	
	override protected val isRestartable: Boolean = false
	
	private val stateP = Volatile.lockable[VastAiVllmProcessState](NotStarted)
	/**
	 * A pointer that contains [[VastAiVllmProcessState]] of this process
	 */
	val detailedStatePointer = stateP.readOnly
	
	/**
	 * A pointer that will store the hosted vLLM API client, if one is successfully created.
	 */
	private val clientP = AssignableOnce[Try[(LockingRequestQueue, OpenAiModelInfo)]]()
	private val requestTimedOutStateP = MayBeAssignedOnce[InstanceStatus]()
	
	private val vastAiProcess = VastAiProcess(statusCheckInterval) { hurryFlag =>
		// Requests for offers
		val requiredDiskSpace = modelSize + additionalReservedDisk
		client.send(GetOffers(requiredDiskSpace, selectOffer.filters, selectOffer.ordering,
				selectOffer.limit, selectOffer.offerType))
			// Selects one offer
			.tryFlatMap(selectOffer.apply)
			.flatMapOrFail { offer =>
				stateP.value = AcquiringInstance(offer)
				val (image, templateHashId) = imageOrVllmTemplateHashId match {
					case Left(image) => image -> ""
					case Right(hashId) => "" -> hashId
				}
				// Accepts the offer, requesting a new instance
				// TODO: Remove test
				val request = AcceptOffer(offer.id, templateHashId = templateHashId, image = image, runType = DirectSsh,
					reservedDiskSpace = requiredDiskSpace, env = env, startCommands = startCommands,
					label = instanceLabel, imageCredentials = imageCredentials, deprecatedView = hurryFlag,
					cancelIfUnavailable = true)
				println(request.path)
				println(request.body)
				client.send(request)
			}
			.toTryFuture
	}
	
	/**
	 * A future that resolves into either:
	 *      - Success: If the API is available for use
	 *      - Failure: If failed to set up the API
	 */
	val clientFuture = clientP.future
	
	
	// COMPUTED --------------------------
	
	/**
	 * @return The current (detailed) state of this process
	 */
	def detailedState = stateP.value
	
	/**
	 * @return The current state of the utilized Vast AI instance
	 */
	def vastAiState = vastAiProcess.detailedState
	/**
	 * @return A pointer that contains the current state of the utilized Vast AI instance
	 */
	def vastAiStatePointer = vastAiProcess.detailedStatePointer
	
	/**
	 * @return Currently usable client interface, along with the model to use
	 */
	def usableClient = detailedState match {
		case HostingApi(instance, client, model) =>
			if (instance.status.instanceIsUsable)
				Some(client -> model)
			else
				None
		case _ => None
	}
	
	/**
	 * @return Whether the queued requests have timed out, causing this process to enter the stopping state.
	 */
	def hasTimedOut = requestTimedOutStateP.value.isDefined
	
	
	// IMPLEMENTED  ----------------------
	
	override protected def runOnce(): Unit = {
		val runResult = Try {
			stateP.value = SelectingOffer
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
				case Success(instance) =>
					stateP.value = InstanceLoading(instance)
					// Starts tracking the instance state
					instance.instancePointer.addListener { e =>
						stateP.update { _.atInstanceState(e.newValue) }
						Continue.onlyIf(state.isRunning)
					}
					instance.loadedFuture
				
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
							.flatMap { hostApi(_, timeoutOrStopFuture, instancePointer) }
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
		runResult.failure.foreach { error => clientP.trySet(Failure(error)) }
		try {
			val clientResult = clientP.getOrElseUpdate {
				Failure(new IllegalStateException("No API was hosted - reason unknown"))
			}
			stateP.value = DestroyingInstance(clientResult match {
				case Success(_) =>
					requestTimedOutStateP.value match {
						case Some(timeoutState) => ApiHostingResult.Disconnected(timeoutState)
						case None => ApiHostingResult.Stopped
					}
				case Failure(error) => ApiHostingResult.Failed(error)
			})
		}
		finally {
			// Destroys the Vast AI instance
			vastAiProcess.stop().waitFor()
				.logWithMessage("Failure while waiting for the Vast AI instance to be destroyed")
			// Finalizes the state
			stateP.update {
				case DestroyingInstance(hostingResult) => Stopped(hostingResult, vastAiProcess.detailedState)
				case state =>
					log(s"Unexpected state after instance-destruction: $state")
					Stopped(
						ApiHostingResult.Failed(new IllegalStateException("Unexpected state at instance destruction")),
						vastAiProcess.detailedState)
			}
			stateP.lock()
		}
	}
	
	
	// OTHER    ---------------------
	
	/**
	 * Sets up and hosts the vLLM API by specifying clientP.
	 * Blocks extensively. Only returns on failure, or once stop() has been called and all pending requests completed.
	 * @param ssh Settings for the SSH connection
	 * @param setupTimeoutOrStopFuture A future that resolves if setup timeout is reached, or if stop() is called.
	 * @param instancePointer A pointer that contains the latest Vast AI instance status
	 * @return Whether API-hosting (fully) succeeded
	 */
	private def hostApi(ssh: SshConnection, setupTimeoutOrStopFuture: Future[_], instancePointer: View[VastAiInstance]) =
	{
		stateP.value = WaitingForApi(instancePointer.value)
		// SSH port-forwarding is kept active as long as the API is used
		println(s"Activating SSH port forwarding for root@${ ssh.host } from port ${ ssh.port } to port $localPort")
		startSshPortForwarding(instancePointer.value.id, ssh, setupTimeoutOrStopFuture).flatMap { sshProcess =>
			try {
				// Creates the API client and waits until it's responsive (or until timeout is reached)
				val _gateway = gateway
				val vllmClient = new LlmServiceClient(_gateway,
					s"http${ if (useHttps) "s" else "" }://127.0.0.1:$localPort/v1",
					maxParallelRequests = _gateway.maxConnectionsPerRoute)
				waitUntilGetModelsSucceeds(vllmClient, setupTimeoutOrStopFuture) match {
					case Success(model) =>
						// The client is now usable. Stores it in a pointer, enabling external use.
						val exposedClient = LockingRequestQueue.wrap(vllmClient, hurryFlag)
						clientP.set(Success(exposedClient -> model))
						stateP.value = HostingApi(instancePointer.value, exposedClient, model)
						
						// Updates the state once requested to stop
						hurryFlag.onceSet {
							// Timeout is not possible at this point, anymore
							requestTimedOutStateP.lock()
							
							stateP.value = StoppingApi(instancePointer.value, exposedClient.pendingRequestCount,
								timedOut = hasTimedOut)
							// Includes the pending request count in the state during this phase
							exposedClient.pendingRequestCountPointer.addListener { e =>
								stateP.mutate {
									case stopping: StoppingApi => Continue -> stopping.withRequestsPending(e.newValue)
									case other => Detach -> other
								}
							}
						}
						
						// Checks for request timeouts in order to automatically shut down this service, if necessary
						monitorRequestTimeouts(exposedClient, instancePointer.value.status)
						
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
	}
	
	private def startSshPortForwarding(instanceId: Int, ssh: SshConnection, setupTimeoutOrStopFuture: Future[_]) = {
		// Makes sure we have a local SSH key
		Env.home.toTry { new NoSuchElementException("HOME environment variable is not available") }.flatMap { home =>
			val sshDirPath = home/".ssh"
			StringFrom.path(sshDirPath/"id_ed25519.pub")
				.flatMap { sshKey =>
					// Makes sure that key is attached to the rented instance
					val requestDeprecationView = View { setupTimeoutOrStopFuture.isCompleted }
					client.send(GetSshKeys(instanceId, requestDeprecationView)).waitForResult()
						.mapOrFail { keysOnInstance =>
							println(s"${ keysOnInstance.size } SSH keys attached:")
							keysOnInstance.foreach { key => println(s"- $key") }
							if (keysOnInstance.exists { _.publicKey == sshKey }) {
								println("SSH key already attached")
								Success("")
							}
							else {
								println(s"The instance had ${
									keysOnInstance.size } SSH keys; None matched id_ed25519 => Attaches the SSH key ($sshKey) to the remote device")
								val result = client.send(AttachSshKey(instanceId, sshKey, requestDeprecationView)).waitForResult()
								println("Waiting 30 more seconds in order for the SSH key to be registered on the remote device")
								// TODO: We need a more dynamic approach
								Wait(30.seconds)
								result
							}
						}
						.toTry
				}
				.map { _ =>
					// Starts SSH port forwarding
					println("Starts the port forwarding")
					// TODO: Pass a ProcessLogger instance to run()
					// TODO: Sometimes it may be good to try resetting the SSH connection in case of timeouts / failures
					process.Process(s"ssh -N -i ${ sshDirPath/"id_ed25519" } -L $localPort:127.0.0.1:18000 root@${
							ssh.host } -p ${
							ssh.port } -o BatchMode=yes -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ServerAliveInterval=60 -o ServerAliveCountMax=5")
						.run()
				}
		}
	}
	
	private def waitUntilGetModelsSucceeds(vllmClient: LlmServiceClient, setupTimeoutFuture: Future[_]) = {
		val resultPromise = Promise[Try[OpenAiModelInfo]]()
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
	                                                     resultPromise: Promise[Try[OpenAiModelInfo]],
	                                                     completionView: View[Boolean]): Future[Unit] =
	{
		// Case: Promise was already completed => Finishes
		if (resultPromise.isCompleted)
			Future.unit
		// Case: Process is still pending => Sends a GET /models request
		else
			vllmClient.push(ListOpenAiModels.withDeprecationView(completionView)).flatMap { result =>
				result.success.flatMap { _.headOption } match {
					// Case: Models are available => Finishes successfully
					case Some(model) =>
						resultPromise.trySuccess(Success(model))
						Future.unit
						
					// Case: No models are available yet => Attempts again after a while
					case None =>
						if (result.isSuccess)
							println("No models are available yet")
						else
							result.failure.foreach { log(_, "GET /models failed") }
						
						Delay(statusCheckInterval) {
							tryCompletePromiseWhenModelsAreAvailable(vllmClient, resultPromise, completionView)
						}
				}
			}
	}
	
	private def monitorRequestTimeouts(queue: LockingRequestQueue, getInstanceStatus: => InstanceStatus) = {
		if (noResponseTimeout.isFinite) {
			// Compares timeout against the earliest request queue time, or the earliest recorded request start time
			// This is in order to avoid timeouts for requests that have been queued (but not running) for a long time
			val lastRecordedStartTimeP = Volatile(Now.toInstant)
			val process = Loop.after(noResponseTimeout) {
				queue.pendingRequests.notEmpty match {
					// Case: One or more requests are being executed => Checks if any of them are too old
					case Some(requests) =>
						val earliestRequestTime = requests.iterator.map { _.queueTime }.min max
							lastRecordedStartTimeP.value
						// Case: At least one request has timed out
						//       => Remembers the instance state & requests the API to stop
						if (earliestRequestTime <= Now - noResponseTimeout) {
							if (requestTimedOutStateP.trySet(getInstanceStatus))
								stop()
							None
						}
						// Case: No request has timed out => Updates the start time
						else {
							requests.findMap { request => Some(request.result.startFuture).filterNot { _.isCompleted } }
								.foreach { _.onComplete { _ => lastRecordedStartTimeP.value = Now } }
							
							// Schedules the next check
							Some(earliestRequestTime + noResponseTimeout)
						}
					// Case: No pending requests => No timeout is possible
					case None => Some(noResponseTimeout)
				}
			}
			// Once this process is requested to stop, timeouts are not needed anymore
			hurryFlag.onceSet { process.stop() }
		}
	}
}
