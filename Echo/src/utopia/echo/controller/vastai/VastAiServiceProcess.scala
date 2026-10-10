package utopia.echo.controller.vastai

import utopia.annex.model.response.{RequestFailure, Response}
import utopia.annex.util.RequestResultExtensions._
import utopia.echo.controller.client.VastAiApiClient
import utopia.echo.controller.vastai.InstanceReuseLogic.NeverReuse
import utopia.echo.controller.vastai.VastAiServiceProcess.{VastAiServiceSettings, takenMachineIdsP, unsupportedStatuses}
import utopia.echo.model.enumeration.ServiceState
import utopia.echo.model.enumeration.ServiceState.NotInstalled
import utopia.echo.model.request.vastai._
import utopia.echo.model.unit.ByteCount
import utopia.echo.model.vastai.instance.InstanceState.Disconnected
import utopia.echo.model.vastai.instance.offer.Offer
import utopia.echo.model.vastai.instance.offer.RunType.DirectSsh
import utopia.echo.model.vastai.instance.{InstanceStatus, NewInstanceFoundation, SshConnection, VastAiInstance}
import utopia.echo.model.vastai.process.VastAiServiceState.VastAiServicePhase.{NotStarted, Serving}
import utopia.echo.model.vastai.process.VastAiServiceState._
import utopia.echo.model.vastai.process.{HostingResult, VastAiProcessState, VastAiServiceState, VastAiVllmProcessRecord}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.TryFuture
import utopia.flow.async.context.Scheduler
import utopia.flow.async.process.ShutdownReaction.SkipDelay
import utopia.flow.async.process.{Delay, Process, Wait}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.file.KeptOpenWriter
import utopia.flow.parse.string.StringFrom
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.{Duration, Now}
import utopia.flow.util.StringExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.util.result.TryExtensions._
import utopia.flow.util.{Env, Mutate, NotEmpty}
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.Settable
import utopia.flow.view.mutable.async.{Volatile, VolatileFlag}
import utopia.flow.view.mutable.eventful.{AssignableOnce, MayBeAssignedOnce, SettableFlag}
import utopia.flow.view.template.eventful.{Changing, Flag}

import java.io.FileNotFoundException
import java.nio.file.Path
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future, Promise, TimeoutException}
import scala.util.{Failure, Success, Try}

object VastAiServiceProcess
{
	// ATTRIBUTES   -------------------
	
	private val unsupportedStatuses = Set(
		"docker_build() error writing dockerfile",
		"Error: GPU error, unable to start instance.",
		"Error response")
	
	/**
	 * Lists the currently taken machine IDs
	 */
	private val takenMachineIdsP = Volatile(Set[Int]())
	
	
	// NESTED   ------------------------
	
	/**
	 * Common trait for factory-like interfaces that are used in Vast AI service construction
	 * @tparam Repr Type of the constructed copies
	 */
	trait VastAiServiceSettingsFactory[+Repr]
	{
		// ABSTRACT ---------------------
		
		/**
		 * @return A copy of this instance with port forwarding disabled
		 */
		def withoutPortForwarding: Repr
		
		/**
		 * @param reuseLogic Logic that may allow reusing already rented Vast AI instances.
		 * @return A copy of this instance applying the specified instance-reuse logic.
		 */
		def reusingInstancesWith(reuseLogic: InstanceReuseLogic): Repr
		/**
		 * @param shouldDestroy A function that determines whether a live instance should be destroyed.
		 *                      Should yield false in situations where the instance should only be stopped instead.
		 *                      Notice that stopped instances still incur costs.
		 *
		 *                      Receives three parameters:
		 *                      1. Last state of the Vast AI instance in question
		 *                      1. How long it's been since the instance started
		 *                      1. Whether the service was successfully hosted. False if the service was not hosted yet.
		 *
		 *                      Never called in situations where the instance is unstable or disconnected,
		 *                      or where the service became unresponsive (indicating a low-quality instance).
		 *
		 * @return A copy of this instance applying the specified instance-destruction logic
		 */
		def withDestructionLogic(shouldDestroy: (VastAiInstance, Duration, Boolean) => Boolean): Repr
		
		/**
		 * @param scriptPath Path to a script file that installs the service when run on the rented device.
		 *                   Note: Installation may be skipped, depending on how the instance / offer is selected.
		 * @return A copy of this instance using the specified installation script when appropriate.
		 */
		def runningInstallScript(scriptPath: Path): Repr
		/**
		 * Enables port forwarding
		 * @param portsView A view to the ports to use, where the first value represents the local port
		 *                  (where the service will be accessed/used)
		 *                  and the second value matches the remote (internal) port,
		 *                  where it is hosted on the remote server.
		 * @return A copy of this instance applying port forwarding over the specified ports.
		 */
		def withPortForwarding(portsView: View[Pair[Int]]): Repr
		
		/**
		 * @param timeout Timeout for the setup process, i.e. the process after the instance has been loaded,
		 *                during which the service should become usable.
		 *
		 *                If the service doesn't become usable before this timeout, the instance is destroyed.
		 * @return A copy of this instance applying the specified setup timeout
		 */
		def withSetupTimeout(timeout: Duration): Repr
		/**
		 * @param timeout Timeout for recovering from SSH and/or service failures.
		 *                During this time period, the service and port forwarding (if applicable) are restarted and
		 *                reconnected to using the normal setup procedure.
		 * @return A copy of this instance applying the specified recovery timeout.
		 */
		def withRecoveryTimeout(timeout: Duration): Repr
		
		/**
		 * @param interval Interval between instance status updates.
		 * @return A copy of this instance applying the specified status-check interval.
		 */
		def updatingStatusEvery(interval: Duration): Repr
		
		/**
		 * @param label Custom label to give to the rented Vast AI instance(s)
		 * @return A copy of this instance using the specified label.
		 */
		def withLabel(label: String): Repr
		
		/**
		 * Enables debug logging
		 * @param writer Interface for making debug log entries. None if debug logging should be disabled.
		 * @return Copy of this instance using the specified debug logger.
		 */
		def debugLoggingWith(writer: Option[KeptOpenWriter]): Repr
		
		
		// OTHER -------------------------
		
		/**
		 * Enables port forwarding
		 * @param localPort Local port to which the remotely hosted service will be forwarded.
		 *                  Call-by-name: Only called once service hosting / port forwarding is started.
		 * @param remotePort (Internal) port at which the hosted service is accessible on the remote instance.
		 *                   Call-by-name: Only called once service hosting / port forwarding is started.
		 * @return A copy of this instance applying port forwarding over the specified ports.
		 */
		def withPortForwarding(localPort: => Int, remotePort: => Int): Repr =
			withPortForwarding(Lazy { Pair(localPort, remotePort) })
		
		/**
		 * Enables debug logging
		 * @param writer Interface for making debug log entries.
		 * @return Copy of this instance using the specified debug logger.
		 */
		def debugLoggingWith(writer: KeptOpenWriter): Repr = debugLoggingWith(Some(writer))
	}
	
	object VastAiServiceSettings
	{
		/**
		 * Settings applied by default
		 */
		lazy val default = VastAiServiceSettings()
	}
	/**
	 * Wraps settings for Vast AI service creation
	 * @param instanceReuseLogic Logic applied for reusing already rented Vast AI instances.
	 *                           Default = never reuse instances.
	 * @param installScriptPath Path to a script for installing vLLM on the rented device.
	 *                          Used (and required), only if 'chooseImage' indicates that vLLM should be installed.
	 *                          Default = None.
	 * @param forwardedPorts Applied port forwarding represented using a Pair of port numbers.
	 *                       The first number matches the local port and the second value the remote port.
	 *                       A [[View]] should be passed in order to reserve the ports lazily.
	 *                       None if no port forwarding should be applied (default).
	 * @param setupTimeout Timeout for the setup process.
	 *                     If the service doesn't become usable before this timeout, the instance is destroyed.
	 *                     Default = infinite (not recommended).
	 * @param recoveryTimeout Timeout for recovering from SSH and/or service failures. Default = 60 seconds.
	 * @param statusCheckInterval Interval between instance pointer / instance status-check updates. Default = 30 seconds.
	 * @param instanceLabel Custom label given to the rented Vast AI instance. Default = empty.
	 * @param debugLogger Interface for making debug log entries (optional)
	 * @param shouldDestroy A function that determines whether a live instance should be destroyed.
	 *                      Should yield false in situations where the instance should only be stopped instead.
	 *                      Notice that stopped instances still incur costs.
	 *
	 *                      Receives two parameters:
	 *                      1. Last state of the Vast AI instance in question
	 *                      1. Whether the service was successfully hosted. False if the service was not hosted yet.
	 *
	 *                      Never called in situations where the instance is unstable or disconnected,
	 *                      or where the service became unresponsive (indicating a low-quality instance).
	 *
	 *                      Default = always yield true = always destroy.
	 */
	case class VastAiServiceSettings(instanceReuseLogic: InstanceReuseLogic = NeverReuse,
	                                 installScriptPath: Option[Path] = None,
	                                 forwardedPorts: Option[View[Pair[Int]]] = None,
	                                 setupTimeout: Duration = 15.minutes, recoveryTimeout: Duration = 60.seconds,
	                                 statusCheckInterval: Duration = 30.seconds, instanceLabel: String = "",
	                                 debugLogger: Option[KeptOpenWriter] = None,
	                                 shouldDestroy: (VastAiInstance, Duration, Boolean) => Boolean = { (_, _, _) => true })
		extends VastAiServiceSettingsFactory[VastAiServiceSettings]
	{
		override def withoutPortForwarding: VastAiServiceSettings = copy(forwardedPorts = None)
		override def reusingInstancesWith(reuseLogic: InstanceReuseLogic): VastAiServiceSettings =
			copy(instanceReuseLogic = reuseLogic)
		override def withDestructionLogic(shouldDestroy: (VastAiInstance, Duration, Boolean) => Boolean): VastAiServiceSettings =
			copy(shouldDestroy = shouldDestroy)
		override def runningInstallScript(scriptPath: Path): VastAiServiceSettings =
			copy(installScriptPath = Some(scriptPath))
		override def withPortForwarding(portsView: View[Pair[Int]]): VastAiServiceSettings =
			copy(forwardedPorts = Some(portsView))
		override def withSetupTimeout(timeout: Duration): VastAiServiceSettings = copy(setupTimeout = timeout)
		override def withRecoveryTimeout(timeout: Duration): VastAiServiceSettings = copy(recoveryTimeout = timeout)
		override def updatingStatusEvery(interval: Duration): VastAiServiceSettings =
			copy(statusCheckInterval = interval)
		override def withLabel(label: String): VastAiServiceSettings = copy(instanceLabel = label)
		override def debugLoggingWith(writer: Option[KeptOpenWriter]): VastAiServiceSettings =
			copy(debugLogger = writer)
	}
	
	trait VastAiServiceSettingsWrapper[+Repr] extends VastAiServiceSettingsFactory[Repr]
	{
		// ABSTRACT -------------------------
		
		/**
		 * @return Applied Vast AI service settings
		 */
		def serviceSettings: VastAiServiceSettings
		/**
		 * @param settings New settings to apply
		 * @return A copy of this instance applying the specified settings
		 */
		def withSettings(settings: VastAiServiceSettings): Repr
		
		
		// IMPLEMENTED  ---------------------
		
		override def withoutPortForwarding: Repr = mapServiceSettings { _.withoutPortForwarding }
		override def reusingInstancesWith(reuseLogic: InstanceReuseLogic): Repr =
			mapServiceSettings { _.reusingInstancesWith(reuseLogic) }
		override def withDestructionLogic(shouldDestroy: (VastAiInstance, Duration, Boolean) => Boolean): Repr =
			mapServiceSettings { _.withDestructionLogic(shouldDestroy) }
		override def runningInstallScript(scriptPath: Path): Repr =
			mapServiceSettings { _.runningInstallScript(scriptPath) }
		override def withPortForwarding(portsView: View[Pair[Int]]): Repr =
			mapServiceSettings { _.withPortForwarding(portsView) }
		override def withSetupTimeout(timeout: Duration): Repr = mapServiceSettings { _.withSetupTimeout(timeout) }
		override def withRecoveryTimeout(timeout: Duration): Repr =
			mapServiceSettings { _.withRecoveryTimeout(timeout) }
		override def updatingStatusEvery(interval: Duration): Repr =
			mapServiceSettings { _.updatingStatusEvery(interval) }
		override def withLabel(label: String): Repr = mapServiceSettings { _.withLabel(label) }
		override def debugLoggingWith(writer: Option[KeptOpenWriter]): Repr =
			mapServiceSettings { _.debugLoggingWith(writer) }
		
		
		// OTHER    -------------------------
		
		def mapServiceSettings(f: Mutate[VastAiServiceSettings]) = withSettings(f(serviceSettings))
	}
}

/**
 * A process for setting up and managing a vLLM server on a rented Vast AI instance
 * @param selectOffer Logic for selecting a Vast AI instance offer
 * @param requiredDiskSpace Amount of disk space that should be reserved on the rented Vast AI instances.
 * @param settings Applied service settings. Default = use the default settings.
 * @tparam Service Type of the internal service hosted
 * @tparam Client Type of the exposed client interface
 * @tparam R Type of successful test connection results. These are acquired during the setup phase.
 * @author Mikko Hilpinen
 * @since 26.02.2026, v1.5
 */
// TODO: Add separate timeout for individual status phases (i.e. if keeps at same status for >X minutes, fail)
abstract class VastAiServiceProcess[Service, Client, R](selectOffer: SelectOffer, requiredDiskSpace: ByteCount,
                                                        settings: VastAiServiceSettings = VastAiServiceSettings.default)
                                                       (implicit exc: ExecutionContext, scheduler: Scheduler,
                                                        log: Logger, vastAiClient: VastAiApiClient)
	extends Process(shutdownReaction = Some(SkipDelay))
{
	// ATTRIBUTES   ------------------------
	
	override protected val isRestartable: Boolean = false
	
	private val stateP = Volatile.lockable[VastAiServiceState](NotStarted)
	/**
	 * A pointer that contains [[VastAiServiceState]] of this process
	 */
	val detailedStatePointer = stateP.readOnly
	/**
	 * A pointer that contains [[VastAiServicePhase]] of this process
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
	 * A pointer that will store an interface to the hosted service, if one is successfully created.
	 */
	private val clientP = AssignableOnce[Try[Client]]()
	
	/**
	 * A mutable pointer that is set if requests start timing out.
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
	 * Determines whether the service should be installed, and whether it is expected to run by itself.
	 * Specified when creating the instance, since this property is dependent on the image/template used.
	 */
	private var assumedServiceState: ServiceState = NotInstalled
	/**
	 * The process used for managing the Vast AI instance
	 */
	private val vastAiProcess = VastAiProcess(settings.statusCheckInterval, maxConsecutiveStatusCheckFailures = Some(5),
		debugLogger = settings.debugLogger) {
		hurryFlag =>
			// Checks for instance-reuse
			val reuseResultFuture: Future[Option[Int]] = {
				// Case: Instances may be reused => Checks whether any are available
				if (settings.instanceReuseLogic.reuses)
					vastAiClient.send(ShowInstances).map {
						// Case: Available instances read => Checks whether any are unused and accepted
						case Response.Success(instances, _, _) =>
							// Case: No instances available => No reuse
							if (instances.isEmpty)
								None
							else {
								val machineIds = takenMachineIdsP.value
								val unused = {
									if (machineIds.isEmpty)
										instances
									else
										instances.filterNot { instance => machineIds(instance.machineId) }
								}
								// Case: All rented instances are taken => No reuse
								if (unused.isEmpty)
									None
								// Case: Unused instances exist => Checks whether any are accepted
								else
									settings.instanceReuseLogic.findReusableFrom(unused).map { _.id }
							}
						// Case: Failed to check for reusable instances => Logs and skips reuse
						case failure: RequestFailure =>
							log(failure.cause, "Failed to check for reusable instances")
							None
					}
				// Case: Reuse is disabled => Won't check for available instances
				else
					Future.successful(None)
			}
			reuseResultFuture.flatMap {
				// Case: Reusing an instance => Success
				case Some(reusableInstanceId) => TryFuture.success(reusableInstanceId)
				// Case: Renting a new instance
				case None =>
					// Case: Interrupted => Fails
					if (hurryFlag.isSet || this.hurryFlag.isSet)
						TryFuture.failure(new InterruptedException(
							"Process was interrupted before an instance could be selected"))
					else {
						// Requests for offers
						vastAiClient.send(GetOffers.forSelector(selectOffer, requiredDiskSpace))
							.tryFlatMap { offers =>
								// Won't include the currently used machine IDs
								val usedMachineIds = takenMachineIdsP.value
								selectFromOffers(offers.filterNot { o => usedMachineIds.contains(o.machineId) },
									requiredDiskSpace, hurryFlag || this.hurryFlag)
							}
							.toTryFuture
					}
			}
	} { instance =>
		// Includes the service state for the destroy-check
		val runtime = Now - _startTime
		detailedState match {
			case _ :Hosting => settings.shouldDestroy(instance, runtime, true)
			// Case: API is being stopped => If timed out, always destroys the instance (because it's unstable)
			case StoppingService(_, _, timedOut) =>
				if (timedOut)
					false
				else
					settings.shouldDestroy(instance, runtime, true)
			case _ => settings.shouldDestroy(instance, runtime, false)
		}
	}
	
	/**
	 * A future that resolves once this process has been requested to stop
	 */
	private val stopFuture = hurryFlag.future
	
	/**
	 * A pointer that contains a client interface, while one is usable.
	 * Contains None at other times.
	 */
	lazy val usableClientPointer = clientP.mergeWith(phasePointer, vastAiProcess.instanceStatePointer) {
		(client, phase, state) =>
			if (phase == Serving && state.exists { _.instanceShouldBeUsed })
				client.flatMap { _.toOption }
			else
				None
	}
	/**
	 * A future that resolves into either:
	 *      - Success: If the service is available for use
	 *      - Failure: If failed to set up the service
	 */
	lazy val clientFuture = clientP.future
	
	
	// INITIAL CODE ----------------------
	
	registerToStopOnceJVMCloses()
	
	// Records a timestamp of when stop() was called
	hurryFlag.onceSet { stopTime = Some(Now) }
	
	// If requests start timing out, starts the shutdown process
	requestTimedOutStateP.onceSet { _ =>
		debugLog(s"${ vastAiProcess.instanceId.mkString }: Stopping because of a request timeout")
		stop()
	}
	
	
	// ABSTRACT --------------------------
	
	/**
	 * Chooses the image or Vast AI template to use. Called after an offer has been selected.
	 * @param acceptedOffer The accepted offer
	 * @return Returns two values:
	 *         1. Instance-creation settings
	 *         1. Expected initial service state at the remote instance
	 */
	protected def chooseImage(acceptedOffer: Offer): (NewInstanceFoundation, ServiceState)
	
	/**
	 * Initializes the internal service instance.
	 * This is called during the setup phase, once SSH connections are available.
	 * Note: Port forwarding is not yet active at this point.
	 * @param ssh An interface for reaching the instance over SSH.
	 * @return Returns 2 values:
	 *         1. Initialized service instance
	 *         1. Service startup/run command that should be executed over SSH.
	 *            Empty if no explicit startup command is used.
	 */
	protected def initializeService(ssh: SshExecutor): (Service, String)
	/**
	 * Attempts to connect to the internally hosted service.
	 * This is called during the setup phase, when:
	 *      - SSH connection is running
	 *      - SSH port-forwarding is online (if applicable)
	 *      - The service has been started
	 *
	 * This function is called within a loop until a successful result is received or until broken.
	 * @param service The started internal service instance
	 * @param deprecationView A view that contains true when connection attempts should be interrupted or terminated.
	 * @return Returns a future that yields either:
	 *         - Right: A successful setup result, indicating that the server is reachable and functioning correctly
	 *         - Left: Whether another attempt should be performed (after a short delay)
	 */
	protected def attemptServiceConnection(service: Service, deprecationView: View[Boolean]): Future[Either[Boolean, R]]
	
	/**
	 * Initializes the publicly exposed client instance.
	 * This is called right before the hosting phase, when:
	 *      - SSH connection is running
	 *      - SSH port-forwarding is online (if applicable)
	 *      - The service has been successfully reached
	 * @param service The running service instance
	 * @param setupResult Successful result from the initial service connection attempts.
	 * @param stopFlag A flag that is set once this process is requested to stop.
	 * @return Client interface to expose.
	 */
	protected def initializeClient(service: Service, setupResult: R, stopFlag: Flag): Client
	/**
	 * Starts a monitoring process that shuts down this service / process if the hosted service becomes unreachable.
	 * @param client Exposed client interface
	 * @param service Internally hosted service instance
	 * @param timedOutFlag A flag that may be set to indicate that the server became unresponsive
	 *                     and should be terminated.
	 * @param continueView A view that contains true while this monitoring process should / may continue.
	 */
	protected def monitorRequestTimeouts(client: Client, service: Service, timedOutFlag: Settable,
	                                     continueView: View[Boolean]): Unit
	
	/**
	 * Stops the hosted service.
	 * @param service The internally hosted service instance.
	 * @param client Exposed client interface.
	 *               None if no client interface was exposed, indicating that the service didn't become reachable.
	 * @return Returns 2 values:
	 *         1. Service stop completion future
	 *         1. A pointer that contains the number of remaining pending requests.
	 */
	protected def stopService(service: Service, client: Option[Client]): (Future[Any], Changing[Int])
	
	
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
	 * @return The current service-hosting phase of this process
	 */
	def phase = detailedState.phase
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
	 *              - Some(Success), if a client was successfully acquired.
	 *
	 *         Note: Even if this yields Some(Success), the returned client might not be usable anymore.
	 *               Always also follow [[detailedState]] to see whether the service has been terminated
	 *               or became inaccessible.
	 *
	 * @see [[usableClient]]
	 */
	def client = clientP.value
	/**
	 * @return A pointer that contains either:
	 *              - None, if a client is still pending / not yet acquired
	 *              - Some(Failure), if no functioning client could be acquired
	 *              - Some(Success), if a client was successfully acquired.
	 *
	 *         The final state of this pointer will always be Some.
	 *
	 *         Note: Even if this pointer contains Some(Success), the returned client might not be usable anymore.
	 *               Always also follow [[detailedState]] to see whether the client has been terminated
	 *               or became inaccessible.
	 *
	 * @see [[usableClientPointer]]
	 * @see [[usableClient]]
	 */
	def clientPointer = clientP.readOnly
	/**
	 * @return Currently usable client interface. None if no client is currently usable.
	 * @see [[vastAiClient]] and [[clientFuture]]
	 */
	def usableClient = usableClientPointer.value
	
	/**
	 * @return Whether the queued requests have timed out, causing this process to enter the stopping state.
	 */
	def hasTimedOut = requestTimedOutStateP.value.isDefined
	
	/**
	 * @return Whether installation script should be ran, if available.
	 *         Note: May change when accepting an offer.
	 */
	protected def shouldInstall = assumedServiceState.wasNotInstalled
	/**
	 * @return Whether the service should be expected to run automatically without any startup call.
	 *         Note: May change when accepting an offer.
	 */
	protected def serviceAutoRuns = assumedServiceState.hasStarted
	
	
	// IMPLEMENTED  ----------------------
	
	override protected def runOnce(): Unit = {
		_startTime = Now
		val runResult = Try {
			stateP.value = SelectingOffer
			// Sets up a Vast AI instance and waits for it to load (or for timeout or stop() call)
			vastAiProcess.runAsync()
			val stopFuture = this.stopFuture.map { _ => false }
			val timeoutOrStopFuture = {
				if (settings.setupTimeout.isFinite)
					stopFuture.raceWith(Delay(settings.setupTimeout)(false))
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
			
			// Case: Instance successfully loaded => Starts hosting the service, if possible (blocks extensively)
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
							.flatMap { host(_, instancePointer, timeoutOrStopFuture).waitForResult() }
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
			// Queues the stopping state once the underlying instance is actually being stopped or destroyed
			vastAiProcess.detailedStatePointer.addListener { stateChange =>
				// Checks whether the instance is being stopped or destroyed, or whether it's still running
				val stoppingDetails = stateChange.newValue match {
					// Case: Stopping (expected) => Prepares to enter StoppingInstance phase
					case VastAiProcessState.Stopping(_, destroying) => Some(destroying)
					case VastAiProcessState.Terminated(destroyed) => Some(destroyed)
					case VastAiProcessState.Failed(cause, _, remainingId) =>
						remainingId.foreach { remainingId =>
							log(cause, s"Failed to terminate Vast AI instance", Model.from("instanceId" -> remainingId))
							debugLog(s"$remainingId: Failed to terminate Vast AI instance: ${ cause.getMessage }")
						}
						Some(false)
						
					// Case: Running => Continues monitoring the process state
					case _ => None
				}
				stoppingDetails match {
					// Case: Underlying instance is stopping or stopped => Enters the StoppingInstance phase
					case Some(destroying) =>
						stateP.value = StoppingInstance(
							// Checks the last API state
							apiStatus = clientResult match {
								case Success(_) =>
									requestTimedOutStateP.value match {
										// Case: Timed out => Considers the API to have disconnected
										case Some(timeoutState) => HostingResult.Disconnected(timeoutState)
										// Case: No timeout
										//       => Considers the API to have stopped after successful hosting
										case None => HostingResult.Stopped
									}
								// Case: API setup failed
								case Failure(error) => HostingResult.Failed(error)
							},
							instance = vastAiProcess.instance, destroying = destroying)
						
					// Case: The underlying process is still running => Continues tracking it
					case None => Continue
				}
			}
		}
		finally {
			// Destroys or stops the Vast AI instance
			debugLog(s"${ vastAiProcess.instanceId.mkString }: Destroying or stopping the Vast AI instance")
			vastAiProcess.stop().waitFor()
				.logWithMessage("Failure while waiting for the Vast AI instance to be destroyed / stopped")
			// Finalizes the state
			val hostingResult = stateP.mutate { state =>
				val hostingResult = state match {
					case StoppingInstance(hostingResult, _, _) => hostingResult
					case state =>
						log(s"${ vastAiProcess.instanceId.mkString }:Unexpected state after instance-destruction: $state")
						HostingResult.Failed(new IllegalStateException("Unexpected state at instance destruction"))
				}
				hostingResult -> Stopped(hostingResult, vastAiProcess.detailedState, vastAiProcess.instance,
					destroyed = vastAiProcess.destroyedFlag.isSet)
			}
			stateP.lock()
			offerP.lock()
			
			// Records the completion of this process
			debugLog(s"${ vastAiProcess.instanceId.mkString }: Records process completion")
			recordP.set(VastAiVllmProcessRecord(
				hostingResult, started = startTime, terminated = Now, loaded = _loadCompletionTime,
				serviceStarted = hostingStartTime, stopped = stopTime, offer = offerP.value))
		}
	}
	
	
	// OTHER    ---------------------
	
	/**
	 * Logs an entry in the debug logger, if applicable
	 * @param entry Entry to log (call-by-name)
	 */
	protected def debugLog(entry: => String) = settings.debugLogger.foreach { logger =>
		logger { _.println(s"${ Now.toLocalTime }: $entry") }
	}
	
	private def selectFromOffers(offers: Seq[Offer], requiredDiskSpace: ByteCount, hurryFlag: Flag): Future[Try[Int]] =
		selectOffer(offers).flatMapOrFail { offer =>
			offerP.setOne(offer)
			stateP.value = AcquiringInstance(offer)
			
			val (base, serviceDefaultState) = chooseImage(offer)
			assumedServiceState = serviceDefaultState
			
			// Accepts the offer, requesting a new instance
			val request = AcceptOffer(offer.id, base, runType = DirectSsh, reservedDiskSpace = requiredDiskSpace,
				label = settings.instanceLabel, deprecatedView = hurryFlag, cancelIfUnavailable = true)
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
	 * Sets up and hosts the service on a rented Vast AI instance
	 * @param sshConfig SSH connection settings
	 * @param instancePointer A pointer that contains the latest state of the rented instance
	 * @param setupTimeoutOrStopFuture A future that resolves once this process is requested to stop,
	 *                                 or if the setup process should time out, whichever comes first.
	 * @return A future that resolves once API hosting has terminated,
	 *         either because of a failure, or because stop() was called.
	 */
	private def host(sshConfig: SshConnection, instancePointer: View[VastAiInstance],
	                 setupTimeoutOrStopFuture: Future[_]) =
	{
		// Sets up SSH and vLLM
		stateP.value = SettingUp(instancePointer.value)
		setupSshAndService(instancePointer.value.id, sshConfig, setupTimeoutOrStopFuture).tryFlatMap { ssh =>
			// Initializes the service but doesn't expose it yet
			val (service, startupCommand) = initializeService(ssh)
			val publicClientP = Volatile.empty[Client]
			val hostingResultPromise = Promise[Try[R]]()
			val shutdownProgressPromise = Promise[Changing[Int]]()
			
			// Starts hosting the service, if possible
			val hostingEndFuture = tryHost(ssh, service, startupCommand, publicClientP, hostingResultPromise,
				shutdownProgressPromise, instancePointer, setupTimeoutOrStopFuture)
			
			hostingResultPromise.future.forResult {
				// Case: Service was successfully hosted => Exposes the client for external use
				case Success(setupResult) =>
					// The client is now usable. Stores it in a pointer, enabling external use.
					debugLog(s"${ instancePointer.value.id }: Service is now usable")
					val client = publicClientP.setOneIfEmpty(initializeClient(service, setupResult, hurryFlag))
					hostingStartTime = Some(Now)
					clientP.set(Success(client))
					stateP.value = Hosting(instancePointer.value)
					
					// Updates the state once requested to stop
					hurryFlag.onceSet {
						debugLog(s"${ instancePointer.value.id }: Starting the service shutdown process")
						// Timeout is not possible at this point, anymore
						requestTimedOutStateP.lock()
						
						shutdownProgressPromise.future.foreach { pendingRequestsP =>
							stateP.value = StoppingService(instancePointer.value, pendingRequestsP.value,
								timedOut = hasTimedOut)
							// Includes the pending request count in the state during this phase
							pendingRequestsP.addListener { e =>
								debugLog(s"${ instancePointer.value.id }: ${
									e.newValue } more request to process before shutdown")
								stateP.mutate {
									case stopping: StoppingService =>
										Continue -> stopping.withRequestsPending(e.newValue)
									case other => Detach -> other
								}
							}
						}
					}
					
					// Starts monitoring request timeouts in order to automatically shut down this service, if necessary
					val exposedTimeoutFlag = SettableFlag()
					monitorRequestTimeouts(client, service, exposedTimeoutFlag,
						continueView = View { requestTimedOutStateP.unlocked && !shouldHurry })
					
				// Case: service hosting failed
				case Failure(error) => clientP.set(Failure(error))
			}
			
			hostingEndFuture
		}
	}
	
	/**
	 * A recursive algorithm that attempts to host the service, restarting it if necessary.
	 * @param ssh SSH executor used
	 * @param service A private service interface it has been set up
	 * @param startupCommand A command to execute over SSH in order to start/run the service. Empty if not applicable.
	 * @param exposedClientView A view that contains the publicly exposed client to the hosted service.
	 *                          Yields None while no client has been exposed / created yet.
	 * @param resultPromise Promise that should be completed once either:
	 *                          - The service becomes usable (success)
	 *                          - The service doesn't become usable before 'setupTimeoutOrStopFuture' resolves (failure)
	 * @param shutdownProgressPromise A promise that should be completed once this process is requested to stop.
	 *                                Receives a pointer that contains the number
	 *                                of service requests still pending processing.
	 * @param instancePointer A pointer that contains the used Vast AI instance
	 * @param setupTimeoutOrStopFuture A future that resolves once the setup process should time out or stop
	 * @return A future that resolves once hosting ends (for good).
	 *         Yields a success if the service was usable at one point, and the hosting ended because stop() was called.
	 *         Yields a failure if the hosting ended for some other reason.
	 */
	private def tryHost(ssh: SshExecutor, service: Service, startupCommand: String,
	                    exposedClientView: View[Option[Client]], resultPromise: Promise[Try[R]],
	                    shutdownProgressPromise: Promise[Changing[Int]], instancePointer: View[VastAiInstance],
	                    setupTimeoutOrStopFuture: Future[_]): Future[Try[Unit]] =
	{
		// Starts the service and port-forwarding
		debugLog(s"${ instancePointer.value.id }: Starting the service & port-forwarding")
		val (serviceProcess, portForwardingProcess) = startServiceAndPortForwarding(ssh, startupCommand)
		val processes = Pair(serviceProcess, portForwardingProcess).flatten
		val hostingEndFuture = NotEmpty(processes) match {
			case Some(processes) => processes.map { _.future }.reduce { _ raceWith _ }
			case None => stopFuture
		}
		
		def stopHosting() = {
			debugLog(s"${ instancePointer.value.id }: Stopping service & port-forwarding")
			portForwardingProcess.foreach { _.kill() }
			serviceProcess.foreach { _.kill() }
		}
		
		// When this process is requested to stop, stops the underlying service and then the whole hosting process
		// This is only performed in the last recursive iteration
		val loopedFlag = VolatileFlag()
		hurryFlag.onceSet {
			if (loopedFlag.isNotSet) {
				val (serviceStoppedFuture, stopProgressP) = stopService(service, exposedClientView.value)
				// Stores the shutdown progress -pointer so that it may be used in the stopping state management
				shutdownProgressPromise.success(stopProgressP)
				serviceStoppedFuture.onComplete { _ => stopHosting() }
			}
		}
		
		// After a short delay, starts checking whether the service becomes usable
		val resultFuture = Delay
			.future(10.seconds) {
				serviceReadyFuture(service, setupTimeoutFuture = setupTimeoutOrStopFuture.raceWith(hostingEndFuture))
			}
			.flatMap {
				// Case: The service is usable => Remembers it (if not already known) and waits for the hosting to end
				case Success(connectResult) =>
					debugLog(s"${ instancePointer.value.id }: Hosting the service")
					resultPromise.trySuccess(Success(connectResult))
					hostingEndFuture.flatMap { _ =>
						// Case: Hosting ended because this process was requested to stop => Completes
						if (processes.isEmpty || shouldHurry)
							TryFuture.successCompletion
						// Case: Hosting failed because of some other reason (network issues, crash, or instance failure)
						//       => Reattempts hosting after a short delay (with limited recovery timeout)
						else {
							// Makes sure the existing processes are killed before attempting restart
							debugLog(s"${ instancePointer.value.id }: Stopping the service")
							stopHosting()
							
							Delay.future(10.seconds) {
								debugLog(s"${ instancePointer.value.id }: Attempting hosting again")
								loopedFlag.set()
								tryHost(ssh, service, startupCommand, exposedClientView, resultPromise,
									shutdownProgressPromise, instancePointer,
									setupTimeoutOrStopFuture = Delay(settings.recoveryTimeout) { () }
										.raceWith(stopFuture))
							}
						}
					}
				// Case: The service didn't become usable in time => Terminates / fails
				case Failure(error) =>
					resultPromise.trySuccess(Failure(error))
					TryFuture.failure(error)
			}
		// Makes sure the hosting processes are not left active in the background
		resultFuture.onComplete { _ => stopHosting() }
		
		resultFuture
	}
	
	/**
	 * Starts up the service and port forwarding (if necessary)
	 * @param ssh Interface for executing commands over SSH
	 * @return Returns 2 processes:
	 *              1. The started process. None if no separate process was necessary.
	 *              1. The port-forwarding process. None if no port-forwarding was used.
	 */
	// TODO: Add process logging for SSH
	private def startServiceAndPortForwarding(ssh: SshExecutor, startupCommand: String) = {
		// Starts the service in the background, unless it should be assumed to be running already
		val serviceProcess = {
			if (serviceAutoRuns)
				None
			else
				startupCommand.ifNotEmpty.map { command => ssh(s"-- '$command'").run().async }
		}
		// Also starts port forwarding, if appropriate
		val forwardingProcess = settings.forwardedPorts.map { portsView =>
			val ports = portsView.value
			ssh.portForwarding(localPort = ports.first, remotePort = ports.second).run().async
		}
		
		serviceProcess -> forwardingProcess
	}
	
	/**
	 * Makes sure SSH and the service are usable on the rented instance
	 * @param instanceId ID of the rented instance
	 * @param sshConfig Configuration for performing SSH connections
	 * @param setupTimeoutOrStopFuture A future that resolves once the setup process should time out,
	 *                                 or if stop() is called.
	 * @return A future that resolves once the service may be started.
	 *         Yields an [[SshExecutor]] on success.
	 *         Yields a failure if SSH setup or the service installation (if applicable) failed.
	 */
	private def setupSshAndService(instanceId: Int, sshConfig: SshConnection, setupTimeoutOrStopFuture: Future[_]) = {
		// Makes sure the installation script is present, if required
		val missingScriptFailure = {
			if (shouldInstall)
				settings.installScriptPath match {
					case Some(path) =>
						if (path.notExists)
							Some(new FileNotFoundException(s"Install script ($path) doesn't exist"))
						// Case: Installation script is present
						else
							None
					case None => Some(new FileNotFoundException("Service installation script has not been specified"))
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
							"Setup process was interrupted before the service could be started"))
					// Case: Service should be installed separately
					//       => Proceeds to run the installation script, if applicable
					else if (shouldInstall)
						runInstallation(ssh, setupTimeoutOrStopFuture).mapSuccess { _ => ssh }
					// Case: vLLM is already installed => Succeeds
					else
						TryFuture.success(ssh)
				}
		}
	}
	/**
	 * Installs the service on the rented device, if possible
	 * @param ssh SSH-executing interface to use
	 * @param stopOrTimeoutFuture A future that resolves if this process should fail / cancel (on stop or timeout)
	 * @return A future that resolves successfully, if the installation succeeded in time
	 */
	private def runInstallation(ssh: SshExecutor, stopOrTimeoutFuture: Future[_]) =
		settings.installScriptPath match {
			case Some(scriptPath) =>
				// Transfers and executes the startup/installation script
				val remoteInstallScriptPath = "~/install_service.sh"
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
	
	private def serviceReadyFuture(service: Service, setupTimeoutFuture: Future[_]) = {
		val resultPromise = Promise[Try[R]]()
		// Completes the promise on timeout
		setupTimeoutFuture.onComplete { _ =>
			if (!resultPromise.isCompleted)
				resultPromise.trySuccess(Failure(new TimeoutException(
					"The setup process timed out while waiting for the service to respond")))
		}
		// Also sets up a process for testing whether the service is usable,
		// possibly completing the result promise before the timeout
		tryCompletePromiseWhenServiceIsReady(service, resultPromise, View { resultPromise.isCompleted })
			.onComplete { _.logWithMessage("Unexpected failure while attempting to connect to the service") }
		
		// Resolves once either the timeout is reached, or when a successful request has completed
		resultPromise.future
	}
	/**
	 * Attempts to connect to the service until a timeout is reached, until connection attempts are terminated,
	 * or until a successful response is received.
	 * @param service Tested service
	 * @param resultPromise A promise that will be completed on success
	 * @param completionView A view that contains true if a request should be retracted
	 * @return A future that resolves once this process completes
	 */
	private def tryCompletePromiseWhenServiceIsReady(service: Service, resultPromise: Promise[Try[R]],
	                                                 completionView: View[Boolean]): Future[Unit] =
	{
		// Case: Promise was already completed => Finishes
		if (resultPromise.isCompleted)
			Future.unit
		// Case: Process is still pending => Continues testing
		else
			attemptServiceConnection(service, completionView).flatMap {
				// Case: Connection successful => Finishes successfully
				case Right(successResult) =>
					resultPromise.trySuccess(Success(successResult))
					Future.unit
				
				// Case: Not connected yet => Attempts again after a while
				case Left(shouldContinue) =>
					if (shouldContinue)
						Delay(settings.statusCheckInterval) {
							tryCompletePromiseWhenServiceIsReady(service, resultPromise, completionView)
						}
					else {
						resultPromise.trySuccess(Failure(new IllegalStateException("Service couldn't be connected to")))
						Future.unit
					}
			}
	}
}
