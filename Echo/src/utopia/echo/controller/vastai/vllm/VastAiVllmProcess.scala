package utopia.echo.controller.vastai.vllm

import utopia.annex.controller.{LockingRequestQueue, RequestQueue}
import utopia.annex.util.RequestResultExtensions._
import utopia.disciple.controller.Gateway
import utopia.disciple.model.request.Timeout
import utopia.echo.controller.client.{LlmServiceClient, VastAiApiClient}
import utopia.echo.controller.vastai.vllm.VastAiVllmProcess.{defaultGateway, takenMachineIdsP, unsupportedStatuses}
import utopia.echo.controller.vastai.{SelectOffer, SshExecutor, VastAiProcess}
import utopia.echo.model.enumeration.ServiceState
import utopia.echo.model.enumeration.ServiceState.NotInstalled
import utopia.echo.model.request.openai.ListOpenAiModels
import utopia.echo.model.request.vastai.{AcceptOffer, AttachSshKey, GetOffers, GetSshKeys}
import utopia.echo.model.response.openai.OpenAiModelInfo
import utopia.echo.model.tokenization.TokenCount
import utopia.echo.model.unit.ByteCount
import utopia.echo.model.unit.ByteCountExtensions._
import utopia.echo.model.vastai.instance.InstanceState.Disconnected
import utopia.echo.model.vastai.instance.offer.Offer
import utopia.echo.model.vastai.instance.offer.RunType.DirectSsh
import utopia.echo.model.vastai.instance.{InstanceStatus, NewInstanceFoundation, SshConnection, VastAiInstance}
import utopia.echo.model.vastai.process.VastAiVllmProcessState.VastAiVllmProcessPhase.NotStarted
import utopia.echo.model.vastai.process.VastAiVllmProcessState._
import utopia.echo.model.vastai.process.{ApiHostingResult, VastAiVllmProcessRecord, VastAiVllmProcessState}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.TryFuture
import utopia.flow.async.context.Scheduler
import utopia.flow.async.process.ShutdownReaction.SkipDelay
import utopia.flow.async.process.{Delay, LoopingProcess, Process, Wait}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.file.KeptOpenWriter
import utopia.flow.parse.string.StringFrom
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.{Duration, Now}
import utopia.flow.util.Env
import utopia.flow.util.StringExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.util.result.TryExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.{AssignableOnce, MayBeAssignedOnce}
import utopia.flow.view.template.eventful.Flag

import java.io.FileNotFoundException
import java.nio.file.Path
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future, Promise, TimeoutException}
import scala.util.{Failure, Success, Try}

object VastAiVllmProcess
{
	// ATTRIBUTES   -------------------
	
	private val unsupportedStatuses = Set(
		"docker_build() error writing dockerfile",
		"Error: GPU error, unable to start instance.")
	
	/**
	 * Lists the currently taken machine IDs
	 */
	private val takenMachineIdsP = Volatile(Set[Int]())
	
	
	// COMPUTED -----------------------
	
	private def defaultGateway = Gateway(maxConnectionsPerRoute = 4, maxConnectionsTotal = 4,
		maximumTimeout = Timeout(read = 10.minutes, manager = 15.minutes), connectionTimeout = 60.seconds,
		disableTrustStoreVerification = true)
	
	
	// OTHER    -----------------------
	
	/**
	 * Creates a new Vast AI + vLLM process.
	 * Note: This process is not automatically started.
	 * @param selectOffer Logic for selecting a Vast AI instance offer
	 * @param modelSize Size of the used model. Used for calculating the reserved disk space.
	 *                  If various model sizes are used, specify the largest.
	 * @param additionalReservedDisk Additional disk space to reserve, beyond the model size. Default = 5 GB.
	 * @param gateway [[Gateway]] instance to use for connecting to the rented instance.
	 *                Default = new Gateway with 4 max connections.
	 *                Note: The used Gateway instance determines the generated request queue's width.
	 * @param installScriptPath Path to a script for installing vLLM on the rented device.
	 *                          Used (and required), only if 'chooseImage' indicates that vLLM should be installed.
	 *                          Default = None.
	 * @param localPort Port used for accessing the vLLM API locally. Default = 8000.
	 * @param remotePort Port at which the vLLM API is served at the remote instance. Default = 8000.
	 *                   Note: If vLLM is auto-hosted by the image / template, make sure to specify the correct port.
	 * @param maxGpuUtil Maximum GPU utilization, as a fraction between 0 and 1. Used when/if starting vLLM.
	 *                   Default = 0.9.
	 * @param maxParallelRequests Maximum number of requests to run in parallel.
	 *                            None (default), if parallelism should not be limited on this level.
	 *
	 *                            Note: The used 'gateway' instance may also limit the number of parallel HTTP connections.
	 * @param extraStartupArgs Additional arguments passed to vLLM serve (default = empty)
	 * @param setupTimeout Timeout for the setup process.
	 *                     If the API doesn't become usable before this timeout, the instance is destroyed.
	 *                     Default = infinite (not recommended).
	 * @param recoveryTimeout Timeout for recovering from SSH and/or vLLM failures. Default = 60 seconds.
	 * @param noResponseTimeout Timeout for started API requests.
	 *                          If this timeout is reached, the request queue is closed
	 *                          and the underlying Vast AI instance is destroyed.
	 *                          Default = infinite (not recommended, unless you have your own monitoring process in place).
	 * @param statusCheckInterval Interval between instance pointer / check updates. Default = 30 seconds.
	 * @param instanceLabel Custom label given to the rented Vast AI instance. Default = empty.
	 * @param debugLogger Interface for making debug log entries (optional)
	 * @param chooseImage A function for choosing the image or Vast AI template to use.
	 *                    Accepts the selected offer, yields:
	 *                          1. Instance-creation settings
	 *                          1. Expected initial vLLM service state at the remote instance
	 *                          1. Maximum context size applied or applicable
	 *                          1. Name of the model to start vLLM with.
	 *                             Optional if vLLM is started automatically by the image / template.
	 * @return A new process instance
	 */
	def apply(selectOffer: SelectOffer, modelSize: ByteCount, additionalReservedDisk: ByteCount = 5.gb,
	          gateway: => Gateway = defaultGateway, installScriptPath: Option[Path] = None,
	          localPort: Int = 8000, remotePort: Int = 8000, maxGpuUtil: Double = 0.9,
	          maxParallelRequests: Option[Int] = None, extraStartupArgs: String = "",
	          setupTimeout: Duration = Duration.infinite, recoveryTimeout: Duration = 60.seconds,
	          noResponseTimeout: Duration = Duration.infinite, statusCheckInterval: Duration = 30.seconds,
	          instanceLabel: String = "", debugLogger: Option[KeptOpenWriter] = None)
	         (chooseImage: Offer => (NewInstanceFoundation, ServiceState, TokenCount, String))
	         (implicit exc: ExecutionContext, scheduler: Scheduler, log: Logger, client: VastAiApiClient) =
		new VastAiVllmProcess(selectOffer, modelSize, additionalReservedDisk, gateway, installScriptPath, localPort,
			remotePort, maxGpuUtil, maxParallelRequests, extraStartupArgs, setupTimeout, recoveryTimeout,
			noResponseTimeout, statusCheckInterval, instanceLabel, debugLogger)(chooseImage)
}

/**
 * A process for setting up and managing a vLLM server on a rented Vast AI instance
 * @param selectOffer Logic for selecting a Vast AI instance offer
 * @param modelSize Size of the used model. Used for calculating the reserved disk space.
 *                  If various model sizes are used, specify the largest.
 * @param additionalReservedDisk Additional disk space to reserve, beyond the model size. Default = 5 GB.
 * @param gateway [[Gateway]] instance to use for connecting to the rented instance.
 *                Default = new Gateway with 4 max connections.
 *                Note: The used Gateway instance determines the generated request queue's width.
 * @param installScriptPath Path to a script for installing vLLM on the rented device.
 *                          Used (and required), only if 'chooseImage' indicates that vLLM should be installed.
 *                          Default = None.
 * @param getLocalPort Port used for accessing the vLLM API locally.
 *                     Call-by-name: Only called once API-hosting / port-forwarding is started.
 *                     Default = 8000.
 * @param remotePort Port at which the vLLM API is served at the remote instance. Default = 8000.
 *                   Note: If vLLM is auto-hosted by the image / template, make sure to specify the correct port.
 * @param maxParallelRequests Maximum number of requests to run in parallel.
 *                            None (default), if parallelism should not be limited on this level.
 *
 *                            Note: The used 'gateway' instance may also limit the number of parallel HTTP connections.
 * @param maxGpuUtil Maximum GPU utilization, as a fraction between 0 and 1. Used when/if starting vLLM.
 *                   Default = 0.9.
 * @param extraStartupArgs Additional arguments passed to vLLM serve (default = empty)
 * @param setupTimeout Timeout for the setup process.
 *                     If the API doesn't become usable before this timeout, the instance is destroyed.
 *                     Default = infinite (not recommended).
 * @param recoveryTimeout Timeout for recovering from SSH and/or vLLM failures. Default = 60 seconds.
 * @param noResponseTimeout Timeout for started API requests.
 *                          If this timeout is reached, the request queue is closed
 *                          and the underlying Vast AI instance is destroyed.
 *                          Default = infinite (not recommended, unless you have your own monitoring process in place).
 * @param statusCheckInterval Interval between instance pointer / check updates. Default = 30 seconds.
 * @param instanceLabel Custom label given to the rented Vast AI instance. Default = empty.
 * @param debugLogger Interface for making debug log entries (optional)
 * @param chooseImage A function for choosing the image or Vast AI template to use.
 *                    Accepts the selected offer, yields:
 *                          1. Instance-creation settings
 *                          1. Expected initial vLLM service state at the remote instance
 *                          1. Maximum context size applied or applicable
 *                          1. Name of the model to start vLLM with.
 *                             Optional if vLLM is started automatically by the image / template.
 * @author Mikko Hilpinen
 * @since 26.02.2026, v1.5
 */
// TODO: Add separate timeout for individual status phases (i.e. if keeps at same status for >X minutes, fail)
class VastAiVllmProcess(selectOffer: SelectOffer, modelSize: ByteCount, additionalReservedDisk: ByteCount = 5.gb,
                        gateway: => Gateway = defaultGateway, installScriptPath: Option[Path] = None,
                        getLocalPort: => Int = 8000, remotePort: Int = 8000, maxGpuUtil: Double = 0.9,
                        maxParallelRequests: Option[Int] = None, extraStartupArgs: String = "",
                        setupTimeout: Duration = Duration.infinite, recoveryTimeout: Duration = 60.seconds,
                        noResponseTimeout: Duration = Duration.infinite, statusCheckInterval: Duration = 30.seconds,
                        instanceLabel: String = "", debugLogger: Option[KeptOpenWriter] = None)
                       (chooseImage: Offer => (NewInstanceFoundation, ServiceState, TokenCount, String))
                       (implicit exc: ExecutionContext, scheduler: Scheduler, log: Logger,
                        vastAiClient: VastAiApiClient)
	extends Process(shutdownReaction = Some(SkipDelay))
{
	// ATTRIBUTES   ------------------------
	
	override protected val isRestartable: Boolean = false
	
	// Only requests the port once it is actually needed
	private lazy val localPort = getLocalPort
	
	private val stateP = Volatile.lockable[VastAiVllmProcessState](NotStarted)
	/**
	 * A pointer that contains [[VastAiVllmProcessState]] of this process
	 */
	val detailedStatePointer = stateP.readOnly
	/**
	 * A pointer that contains [[VastAiVllmProcessPhase]] of this process
	 */
	val phasePointer = stateP.lightMap { _.phase }
	
	// Collects timestamps of various events, for the final result
	private var _startTime = Now.toInstant
	private var _loadCompletionTime: Option[Instant] = None
	private var hostingStartTime: Option[Instant] = None
	private var stopTime: Option[Instant] = None
	
	private val offerP = Volatile.lockable.empty[Offer]
	/**
	 * A pointer that contains the selected offer, once known
	 */
	val offerPointer = offerP.readOnly
	
	/**
	 * A pointer that will store the hosted vLLM API client, if one is successfully created.
	 * If successful, will contain 3 values:
	 *      1. The request queue for accessing the API
	 *      1. Information about the used model
	 *      1. Applied maximum context size
	 */
	private val clientP = AssignableOnce[Try[(LockingRequestQueue, OpenAiModelInfo, TokenCount)]]()
	/**
	 * A mutable pointer that is set if requests start timing out (see [[noResponseTimeout]]).
	 * Causes this process to stop (more or less gracefully).
	 */
	private val requestTimedOutStateP = MayBeAssignedOnce[InstanceStatus]()
	
	private val recordP = AssignableOnce[VastAiVllmProcessRecord]()
	/**
	 * A pointer that contains a record of this process, once completed
	 */
	val recordPointer = recordP.readOnly
	/**
	 * A future that resolves once this process completes.
	 * Contains a record of this process' data.
	 */
	lazy val recordFuture = recordP.future
	
	/**
	 * Determines whether vLLM should be installed, and whether it is expected to run by itself.
	 * Specified when creating the instance, since this property is dependent on the image/template used.
	 */
	private var assumedVllmState: ServiceState = NotInstalled
	/**
	 * Name of the model to serve. Specified when creating the instance.
	 */
	private var modelToServe: String = ""
	/**
	 * Applied maximum context size. Specified when creating the instance.
	 */
	private var _maxContextSize: TokenCount = TokenCount.zero
	/**
	 * The process used for managing the Vast AI instance
	 */
	private val vastAiProcess = VastAiProcess(statusCheckInterval, maxConsecutiveStatusCheckFailures = Some(5),
		debugLogger = debugLogger) {
		hurryFlag =>
			// Requests for offers
			val requiredDiskSpace = modelSize + additionalReservedDisk
			vastAiClient.send(GetOffers(requiredDiskSpace, selectOffer.filters, selectOffer.ordering, selectOffer.limit,
					selectOffer.offerType))
				.tryFlatMap { offers =>
					// Won't include the currently used machine IDs
					val usedMachineIds = takenMachineIdsP.value
					selectFromOffers(offers.filterNot { o => usedMachineIds.contains(o.machineId) }, requiredDiskSpace,
						hurryFlag || this.hurryFlag)
				}
				.toTryFuture
	}
	
	/**
	 * A future that resolves once this process has been requested to stop
	 */
	private val stopFuture = hurryFlag.future
	/**
	 * A future that resolves into either:
	 *      - Success: If the API is available for use
	 *      - Failure: If failed to set up the API
	 */
	val clientFuture = clientP.future
	
	
	// INITIAL CODE ----------------------
	
	registerToStopOnceJVMCloses()
	
	// Records a timestamp of when stop() was called
	hurryFlag.onceSet { stopTime = Some(Now) }
	
	// If requests start timing out, starts the shutdown process
	requestTimedOutStateP.onceSet { _ => stop() }
	
	
	// COMPUTED --------------------------
	
	/**
	 * @return Time when this process was started.
	 *         If this process hasn't been started, returns the time when it was created.
	 */
	def startTime = _startTime
	/**
	 * @return Time when the instance was fully loaded. None if not loaded (yet).
	 */
	def loadCompletionTime = _loadCompletionTime
	
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
	 * @return Maximum context size applied on the model.
	 *         None if maximum context size hasn't been determined (i.e. if no offer has been accepted yet).
	 */
	def maxContextSize = _maxContextSize.ifPositive
	
	/**
	 * @return ID of the managed instance. None if no instance was acquired yet.
	 *         Note: This instance might already have been destroyed. Use this value only for logging, etc.
	 */
	def instanceId = vastAiProcess.instanceId
	/**
	 * @return Status of the currently active Vast AI instance.
	 *         None if no instance is currently active.
	 */
	def instanceStatus = vastAiState.instanceStatus
	
	/**
	 * @return Yields either:
	 *              - None, if a client is still pending / not yet acquired
	 *              - Some(Failure), if no functioning client could be acquired
	 *              - Some(Success), if a client was successfully acquired. Contains the following 3 values:
	 *                  - A request queue for that client
	 *                  - Information about the utilized LLM
	 *                  - Applied maximum context size
	 *
	 *         Note: Even if this yields Some(Success), the returned client might not be usable anymore.
	 *               Always also follow [[detailedState]] to see whether the client has been terminated
	 *               or became inaccessible.
	 *
	 * @see [[usableClient]]
	 */
	def client = clientP.value
	/**
	 * @return A pointer that contains either:
	 *              - None, if a client is still pending / not yet acquired
	 *              - Some(Failure), if no functioning client could be acquired
	 *              - Some(Success), if a client was successfully acquired. Contains the following 3 values:
	 *                  - A request queue for that client
	 *                  - Information about the utilized LLM
	 *                  - Applied maximum context size
	 *
	 *         The final state of this pointer will always be Some.
	 *
	 *         Note: Even if this pointer contains Some(Success), the returned client might not be usable anymore.
	 *               Always also follow [[detailedState]] to see whether the client has been terminated
	 *               or became inaccessible.
	 *
	 * @see [[usableClient]]
	 */
	def clientPointer = clientP.readOnly
	/**
	 * @return Currently usable client interface, along with the model to use and the maximum context size.
	 *         None if no client is currently usable.
	 * @see [[vastAiClient]] and [[clientFuture]]
	 */
	def usableClient = detailedState match {
		case HostingApi(instance, client, model, maxContextSize) =>
			if (instance.status.instanceIsUsable)
				Some((client, model, maxContextSize))
			else
				None
		case _ => None
	}
	
	/**
	 * @return Whether the queued requests have timed out, causing this process to enter the stopping state.
	 */
	def hasTimedOut = requestTimedOutStateP.value.isDefined
	
	private def shouldInstallVllm = assumedVllmState.wasNotInstalled
	private def vllmAutoRuns = assumedVllmState.hasStarted
	
	
	// IMPLEMENTED  ----------------------
	
	override protected def runOnce(): Unit = {
		_startTime = Now
		val runResult = Try {
			stateP.value = SelectingOffer
			// Sets up a Vast AI instance and waits for it to load (or for timeout or stop() call)
			vastAiProcess.runAsync()
			val stopFuture = this.stopFuture.map { _ => false }
			val timeoutOrStopFuture = {
				if (setupTimeout.isFinite)
					stopFuture.raceWith(Delay(setupTimeout)(false))
				else
					stopFuture
			}
			val instanceLoadedFuture = vastAiProcess.liveInstanceFuture.flatMap {
				// Case: Instance-acquisition succeeded => Checks when it's fully loaded
				case Success(instance) =>
					debugLog(s"Instance ${ instance.id } acquired")
					offerP.lock()
					val machineId = instance.wrapped.machineId
					takenMachineIdsP.update { _ + machineId }
					stateP.value = InstanceLoading(instance)
					// Starts tracking the instance state
					instance.instancePointer.addListener { e =>
						stateP.update { _.atInstanceState(e.newValue) }
						// If the instance goes offline or disconnects, stops this process immediately
						val status = e.newValue.status
						if (status.actual.value == Disconnected || unsupportedStatuses.contains(status.message)) {
							debugLog(s"${e.newValue.id}: Status became ${
								e.newValue.status } => Starts the termination process")
							stop()
						}
						Continue.onlyIf(state.isRunning)
					}
					// Once the instance is no longer used, remembers that the machine is available
					vastAiProcess.completionFuture.onComplete { _ =>
						debugLog(s"${ instance.id }: Marks the machine as free again")
						takenMachineIdsP.update { _ - machineId }
					}
					instance.loadedFuture
				
				// Case: Instance-acquisition failed => Won't proceed
				case Failure(error) =>
					offerP.lock()
					clientP.set(Failure(error))
					log(error, "Failed to acquire a Vast AI instance")
					Future.successful(false)
			}
			
			// Case: Instance successfully loaded => Starts hosting the API, if possible (blocks extensively)
			if (instanceLoadedFuture.raceWith(timeoutOrStopFuture).waitFor()
				.logWithMessage("Failure while waiting for instance to load, timeout or stop").getOrElse(false))
				vastAiProcess.instancePointerFuture.waitForResult()
					.flatMap { instancePointer =>
						_loadCompletionTime = Some(Now)
						debugLog(s"${ instancePointer.value.id }: Instance loaded")
						instancePointer.value.ssh
							.toTry {
								new IllegalStateException("SSH connection is not available on the rented instance")
							}
							.flatMap { hostApi(_, instancePointer, timeoutOrStopFuture).waitForResult() }
					}
					// Logs failures and ensures that clientP receives a value
					.failure.foreach { error =>
						clientP.trySet(Failure(error))
						log(error, "Failure while attempting to host the vLLM API")
					}
			// Case: Instance failed to load => Completes the client pointer, if not already completed
			else {
				debugLog(s"${ vastAiProcess.instanceId.mkString }: Marks the machine as free again")
				clientP.trySet(Failure(new IllegalStateException("The Vast AI instance failed to load")))
			}
		}
		try {
			// Makes sure the client pointer receives failure value, if not yet set
			runResult.failure.foreach { error => clientP.trySet(Failure(error)) }
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
			debugLog(s"${ vastAiProcess.instanceId.mkString }: Destroying the Vast AI instance")
			vastAiProcess.stop().waitFor()
				.logWithMessage("Failure while waiting for the Vast AI instance to be destroyed")
			// Finalizes the state
			val hostingResult = stateP.mutate { state =>
				val hostingResult = state match {
					case DestroyingInstance(hostingResult) => hostingResult
					case state =>
						log(s"Unexpected state after instance-destruction: $state")
						ApiHostingResult.Failed(new IllegalStateException("Unexpected state at instance destruction"))
				}
				hostingResult -> Stopped(hostingResult, vastAiProcess.detailedState)
			}
			stateP.lock()
			offerP.lock()
			
			// Records the completion of this process
			debugLog(s"${ vastAiProcess.instanceId.mkString }: Records process completion")
			recordP.set(VastAiVllmProcessRecord(
				hostingResult, started = startTime, terminated = Now, loaded = _loadCompletionTime,
				apiStarted = hostingStartTime, stopped = stopTime, offer = offerP.value))
		}
	}
	
	
	// OTHER    ---------------------
	
	private def selectFromOffers(offers: Seq[Offer], requiredDiskSpace: ByteCount, hurryFlag: Flag): Future[Try[Int]] =
		selectOffer(offers).flatMapOrFail { offer =>
			offerP.setOne(offer)
			stateP.value = AcquiringInstance(offer)
			
			val (base, vllmDefaultState, contextSize, model) = chooseImage(offer)
			assumedVllmState = vllmDefaultState
			modelToServe = model
			_maxContextSize = contextSize
			
			// Accepts the offer, requesting a new instance
			val request = AcceptOffer(offer.id, base, runType = DirectSsh, reservedDiskSpace = requiredDiskSpace,
				label = instanceLabel, deprecatedView = hurryFlag, cancelIfUnavailable = true)
			val resultFuture = vastAiClient.send(request)
			// If accept offer fails, may try again with a different offer
			// TODO: Add handling for infinite loops
			if (offers.hasSize > 1 && hurryFlag.isNotSet)
				resultFuture.tryFlatMapFailure { error =>
					log(error, "Failed to accept an offer")
					selectFromOffers(offers.filterNot { _.id == offer.id }, requiredDiskSpace, hurryFlag)
				}
			else
				resultFuture
		}
	
	/**
	 * Sets up and hosts the vLLM API on a rented Vast AI instance
	 * @param sshConfig SSH connection settings
	 * @param instancePointer A pointer that contains the latest state of the rented instance
	 * @param setupTimeoutOrStopFuture A future that resolves once this process is requested to stop,
	 *                                 or if the setup process should time out, whichever comes first.
	 * @return A future that resolves once API hosting has terminated,
	 *         either because of a failure, or because stop() was called.
	 */
	private def hostApi(sshConfig: SshConnection, instancePointer: View[VastAiInstance],
	                    setupTimeoutOrStopFuture: Future[_]) =
	{
		// Sets up SSH and vLLM
		stateP.value = SettingUpApi(instancePointer.value)
		setupSshAndVllm(instancePointer.value.id, sshConfig, setupTimeoutOrStopFuture).tryFlatMap { ssh =>
			// Prepares an internal vLLM client interface first
			// Exposes a public wrapper once the API is known to respond
			val vllmClient = {
				val _gateway = gateway
				new LlmServiceClient(_gateway, s"http://127.0.0.1:$localPort/v1",
					maxParallelRequests = maxParallelRequests)
			}
			val lazyPublicClient = Lazy { LockingRequestQueue.wrap(vllmClient, hurryFlag) }
			val hostingResultPromise = Promise[Try[OpenAiModelInfo]]()
			
			// Starts hosting the API, if possible
			stateP.value = StartingApi(instancePointer.value)
			val hostingEndFuture = tryHostVllm(ssh, vllmClient, lazyPublicClient, hostingResultPromise, instancePointer,
				setupTimeoutOrStopFuture)
			
			hostingResultPromise.future.forResult {
				// Case: API was successfully hosted => Exposes the vLLM client for external use
				case Success(model) =>
					// The client is now usable. Stores it in a pointer, enabling external use.
					debugLog(s"${ instancePointer.value.id }: API is now usable")
					val publicClient = lazyPublicClient.value
					hostingStartTime = Some(Now)
					clientP.set(Success((publicClient, model, _maxContextSize)))
					stateP.value = HostingApi(instancePointer.value, publicClient, model, _maxContextSize)
					
					// Updates the state once requested to stop
					hurryFlag.onceSet {
						debugLog(s"${ instancePointer.value.id }: Starting the API shutdown process")
						// Timeout is not possible at this point, anymore
						requestTimedOutStateP.lock()
						
						stateP.value = StoppingApi(instancePointer.value, publicClient.pendingRequestCount,
							timedOut = hasTimedOut)
						// Includes the pending request count in the state during this phase
						publicClient.pendingRequestCountPointer.addListener { e =>
							debugLog(s"${ instancePointer.value.id }: ${ e.newValue } more request to process before shutdown")
							stateP.mutate {
								case stopping: StoppingApi => Continue -> stopping.withRequestsPending(e.newValue)
								case other => Detach -> other
							}
						}
					}
					
					// Checks for request timeouts in order to automatically shut down this service, if necessary
					monitorRequestTimeouts(publicClient, instancePointer.value.status)
					
				// Case: API-hosting failed
				case Failure(error) => clientP.set(Failure(error))
			}
			
			hostingEndFuture
		}
	}
	
	/**
	 * A recursive algorithm that attempts to host a vLLM API, restarting it if necessary.
	 * @param ssh SSH executor used
	 * @param internalVllmClient Interface for sending requests to the vLLM API once it has been set up
	 * @param lazyExposedVllmClient A lazily initialized container that contains the publicly exposed version of the
	 *                              served API client.
	 * @param resultPromise Promise that should be completed once either:
	 *                          - The API becomes usable (success)
	 *                          - The API doesn't become usable before 'stopFuture' resolves (failure)
	 * @param instancePointer A pointer that contains the used Vast AI instance
	 * @param stopFuture A future that resolves once this process should time out or stop
	 * @return A future that resolves once hosting ends (for good).
	 *         Yields a success if the API was usable at one point, and the hosting ended because stop() was called.
	 *         Yields a failure if the hosting ended for some other reason.
	 */
	private def tryHostVllm(ssh: SshExecutor, internalVllmClient: RequestQueue,
	                        lazyExposedVllmClient: Lazy[LockingRequestQueue],
	                        resultPromise: Promise[Try[OpenAiModelInfo]], instancePointer: View[VastAiInstance],
	                        stopFuture: Future[_]): Future[Try[Unit]] =
	{
		// Starts the vLLM service and port-forwarding
		debugLog(s"${ instancePointer.value.id }: Starting vLLM & port-forwarding")
		val (vllmProcess, portForwardingProcess) = startVllm(ssh, instancePointer)
		val hostingEndFuture = vllmProcess match {
			case Some(vllmProcess) => vllmProcess.future.raceWith(portForwardingProcess.future)
			case None => portForwardingProcess.future
		}
		
		def stopHosting() = {
			debugLog(s"${ instancePointer.value.id }: Stopping vLLM & port-forwarding")
			portForwardingProcess.kill()
			vllmProcess.foreach { _.kill() }
		}
		
		// When this process is requested to stop, also stops the hosting
		// (but waits for pending requests to complete first)
		hurryFlag.onceSet { lazyExposedVllmClient.current.foreach { _.stopFuture.onComplete { _ => stopHosting() } } }
		
		// After a short delay, starts checking whether the model is available (= API is usable)
		val resultFuture = Delay
			.future(10.seconds) { modelsAvailableFuture(internalVllmClient, stopFuture.raceWith(hostingEndFuture)) }
			.flatMap {
				// Case: API is usable => Remembers it (if not already known) and waits for the hosting to end
				case Success(model) =>
					debugLog(s"${ instancePointer.value.id }: Hosting the API")
					resultPromise.trySuccess(Success(model))
					hostingEndFuture.flatMap { _ =>
						// Case: Hosting ended because this process was requested to stop => Completes
						if (shouldHurry)
							TryFuture.successCompletion
						// Case: Hosting failed because of some other reason (network issues, crash, or instance failure)
						//       => Reattempts hosting after a short delay (with limited recovery timeout)
						else {
							// Makes sure the existing processes are killed before attempting restart
							debugLog(s"${ instancePointer.value.id }: Stopping the API")
							stopHosting()
							
							Delay.future(10.seconds) {
								debugLog(s"${ instancePointer.value.id }: Attempting hosting again")
								tryHostVllm(ssh, internalVllmClient, lazyExposedVllmClient, resultPromise,
									instancePointer, Delay(recoveryTimeout) { () }.raceWith(this.stopFuture))
							}
						}
					}
				// Case: API didn't become usable in time => Terminates / fails
				case Failure(error) =>
					resultPromise.trySuccess(Failure(error))
					TryFuture.failure(error)
			}
		// Makes sure the hosting processes are not left active in the background
		resultFuture.onComplete { _ => stopHosting() }
		
		resultFuture
	}
	
	/**
	 * Starts up vLLM (if necessary) and port forwarding
	 * @param ssh Interface for executing commands over SSH
	 * @param instancePointer Pointer to the Vast AI instance used
	 * @return Returns 2 processes:
	 *              1. The started vLLM process. None if no separate process was necessary.
	 *              1. The port-forwarding process.
	 */
	// TODO: Add process logging for SSH
	private def startVllm(ssh: SshExecutor, instancePointer: View[VastAiInstance]) = {
		// Starts the vLLM in the background, unless it should be assumed to be running already
		val vllmProcess = {
			if (vllmAutoRuns)
				None
			else {
				// Uses either python or vllm, depending on how vLLM was installed
				val baseCommand = {
					if (shouldInstallVllm)
						s"source ~/miniconda/etc/profile.d/conda.sh && conda activate vllm-env && exec python -m vllm.entrypoints.openai.api_server${
							modelToServe.mapIfNotEmpty { model => s" --model '$model'" } }"
					// TODO: This CUDA_VISIBLE_DEVICES is experimental. Maybe remove
					else
						s"CUDA_VISIBLE_DEVICES=0 exec vllm serve $modelToServe"
				}
				// TODO: Add --tensor-parallel-size N
				Some(ssh(s"-- '$baseCommand --max-model-len ${ _maxContextSize.value } --host 127.0.0.1 --port $remotePort --gpu-memory-utilization $maxGpuUtil${
					extraStartupArgs.prependIfNotEmpty(" ") }'")
					.run().async)
			}
		}
		
		// Also starts port forwarding
		val forwardingProcess = ssh.portForwarding(localPort, remotePort).run().async
		
		vllmProcess -> forwardingProcess
	}
	
	/**
	 * Makes sure SSH and vLLM are usable on the rented instance
	 * @param instanceId ID of the rented instance
	 * @param sshConfig Configuration for performing SSH connections
	 * @param setupTimeoutOrStopFuture A future that resolves once the setup process should time out,
	 *                                 or if stop() is called.
	 * @return A future that resolves once vLLM may be started.
	 *         Yields an [[SshExecutor]] on success.
	 *         Yields a failure if SSH setup or the vLLM installation (if applicable) failed.
	 */
	private def setupSshAndVllm(instanceId: Int, sshConfig: SshConnection, setupTimeoutOrStopFuture: Future[_]) = {
		// Makes sure the installation script is present, if required
		val missingScriptFailure = {
			if (shouldInstallVllm)
				installScriptPath match {
					case Some(path) =>
						if (path.notExists)
							Some(new FileNotFoundException(s"Install script ($path) doesn't exist"))
						// Case: Installation script is present
						else
							None
					case None => Some(new FileNotFoundException("vLLM installation script has not been specified"))
				}
			// Case: Installation not required
			else
				None
		}
		missingScriptFailure match {
			// Case: Installation script doesn't exist => Fails
			case Some(failure) => TryFuture.failure(failure)
			case None =>
				val deprecationView = View { setupTimeoutOrStopFuture.isCompleted }
				// Sets up SSH
				setupSsh(instanceId, sshConfig, deprecationView).map { _.toTry }.tryFlatMap { ssh =>
					// Case: Timed out => Fails
					if (deprecationView.value)
						TryFuture.failure(new TimeoutException(
							"Setup process was interrupted before vLLM could be started"))
					// Case: vLLM should be installed separately
					//       => Proceeds to run the installation script, if applicable
					else if (shouldInstallVllm)
						installVllm(ssh, setupTimeoutOrStopFuture).mapSuccess { _ => ssh }
					// Case: vLLM is already installed => Succeeds
					else
						TryFuture.success(ssh)
				}
		}
	}
	/**
	 * Installs vLLM on the rented device, if possible
	 * @param ssh SSH-executing interface to use
	 * @param stopOrTimeoutFuture A future that resolves if this process should fail / cancel (on stop or timeout)
	 * @return A future that resolves successfully, if the installation succeeded in time
	 */
	private def installVllm(ssh: SshExecutor, stopOrTimeoutFuture: Future[_]) =
		installScriptPath match {
			case Some(scriptPath) =>
				// Transfers and executes the startup/installation script
				val remoteInstallScriptPath = "~/start_vllm.sh"
				ssh.transfer(scriptPath, remoteInstallScriptPath).run().async.timeoutWith(stopOrTimeoutFuture).future
					.tryFlatMap { _ =>
						ssh.executeScript(remoteInstallScriptPath).run().async.timeoutWith(stopOrTimeoutFuture).future
					}
			// Case: No installation is necessary => Succeeds
			case None => TryFuture.successCompletion
		}
	
	// Makes sure SSH keys are usable
	private def setupSsh(instanceId: Int, sshConfig: SshConnection, deprecationView: View[Boolean]) = {
		debugLog(s"$instanceId: Setting up SSH")
		Env.home.toTry { new NoSuchElementException("HOME environment variable is not available") }
			.map { home =>
				val sshDir = home/".ssh"
				registerSshKey(instanceId, sshDir/"id_ed25519.pub", deprecationView).mapSuccess { _ =>
					SshExecutor(sshConfig, sshDir/"id_ed25519")
				}
			}
			.flattenToFuture
	}
	
	private def registerSshKey(instanceId: Int, publicSshKeyPath: Path, deprecationView: View[Boolean]) = {
		// Reads the public SSH key
		StringFrom.path(publicSshKeyPath)
			.map { sshKey =>
				// Makes sure that key is attached to the rented instance
				vastAiClient.send(GetSshKeys(instanceId, deprecationView)).mapOrFail { keysOnInstance =>
					if (keysOnInstance.exists { _.publicKey == sshKey })
						Success("Key was already attached")
					else {
						val result = vastAiClient.send(AttachSshKey(instanceId, sshKey, deprecationView)).waitForResult()
						debugLog(s"$instanceId: Waiting 40 more seconds in order for the SSH key to be registered on the remote device")
						// TODO: We need a more dynamic approach
						Wait(40.seconds)
						result
					}
				}
			}
			.flattenToFuture
	}
	
	private def modelsAvailableFuture(vllmClient: RequestQueue, setupTimeoutFuture: Future[_]) = {
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
		
		// Resolves once either the timeout is reached, or when a successful request has completed
		resultPromise.future
	}
	/**
	 * Performs GET /models queries until a timeout is reached, or until models are found from the API response,
	 * indicating that it's usable.
	 * @param vllmClient Client to the tested API
	 * @param resultPromise A promise that will be completed on success
	 * @param completionView A view that contains true if a request should be retracted
	 * @return A future that resolves once this process completes
	 */
	// TODO: Handle request deprecation error separately (the queue keeps the requests in until deprecated)
	private def tryCompletePromiseWhenModelsAreAvailable(vllmClient: RequestQueue,
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
							debugLog("No models are available yet")
						else
							result.failure.foreach { log(_, "GET /models failed") }
						
						Delay(statusCheckInterval) {
							tryCompletePromiseWhenModelsAreAvailable(vllmClient, resultPromise, completionView)
						}
				}
			}
	}
	
	// TODO: Refactor to use Scheduler / Loop instead
	private def monitorRequestTimeouts(queue: LockingRequestQueue, getInstanceStatus: => InstanceStatus) = {
		if (noResponseTimeout.isFinite) {
			// Compares timeout against the earliest request queue time, or the earliest recorded request start time
			// This is in order to avoid timeouts for requests that have been queued (but not running) for a long time
			debugLog(s"${ vastAiProcess.instanceId.mkString }: Starts monitoring request timeouts")
			val lastRecordedStartTimeP = Volatile(Now.toInstant)
			val process = LoopingProcess.started.after(noResponseTimeout) { _ =>
				queue.pendingRequests.notEmpty match {
					// Case: One or more requests are being executed => Checks if any of them are too old
					case Some(requests) =>
						val earliestRequestTime = requests.iterator.map { _.queueTime }.min max
							lastRecordedStartTimeP.value
						// Case: At least one request has timed out
						//       => Remembers the instance state & requests the API to stop
						if (earliestRequestTime <= Now - noResponseTimeout) {
							debugLog(s"${ vastAiProcess.instanceId.mkString }: Requests started timing out (${
								(Now - earliestRequestTime).description })")
							if (requestTimedOutStateP.trySet(getInstanceStatus)) {
								debugLog(s"${ vastAiProcess.instanceId.mkString }: Stopping because of a request timeout")
								stop()
							}
							None
						}
						// Case: No request has timed out => Updates the start time
						else {
							debugLog(s"${ vastAiProcess.instanceId.mkString }: No request timed out")
							requests.findMap { request => Some(request.result.startFuture).filterNot { _.isCompleted } }
								.foreach { _.onComplete { _ => lastRecordedStartTimeP.value = Now } }
							
							// Schedules the next check
							Some(earliestRequestTime + noResponseTimeout)
						}
					// Case: No pending requests => No timeout is possible
					case None =>
						debugLog(s"${ vastAiProcess.instanceId.mkString }: No requests are pending")
						Some(noResponseTimeout)
				}
			}
			// Once this process is requested to stop, timeouts are not needed anymore
			hurryFlag.onceSet { process.stop() }
		}
	}
	
	private def debugLog(entry: => String) = debugLogger.foreach { logger =>
		logger { _.println(s"${ Now.toLocalTime }: $entry") }
	}
}
