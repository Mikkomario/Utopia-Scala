package utopia.echo.controller.vastai.vllm

import utopia.access.model.enumeration.Status.BadRequest
import utopia.annex.controller.RequestQueue
import utopia.annex.model.response.RequestNotSent.{RequestSendingFailed, RequestWasDeprecated}
import utopia.annex.model.response.{RequestFailure, RequestResult, Response}
import utopia.disciple.controller.Gateway
import utopia.echo.controller.chat.BufferingChatRequestExecutor
import utopia.echo.controller.client.VastAiApiClient
import utopia.echo.controller.vastai.SelectOffer
import utopia.echo.controller.vastai.vllm.VastAiVllmChatExecutor.maxRetries
import utopia.echo.model.enumeration.ModelParameter.ContextTokens
import utopia.echo.model.enumeration.ServiceState
import utopia.echo.model.llm.{LlmDesignator, LlmVramUse}
import utopia.echo.model.request.ChatParams
import utopia.echo.model.request.openai.BufferedOpenAiChatCompletionRequest
import utopia.echo.model.response.openai.BufferedOpenAiReply
import utopia.echo.model.tokenization.TokenCount
import utopia.echo.model.unit.ByteCount
import utopia.echo.model.unit.ByteCountExtensions._
import utopia.echo.model.vastai.instance.NewInstanceFoundation
import utopia.echo.model.vastai.instance.offer.Offer
import utopia.echo.model.vastai.process.VastAiVllmProcessState.HostingApi
import utopia.echo.model.vastai.process.VastAiVllmProcessState.VastAiVllmProcessPhase.{ApiHosting, NotStarted, Stopping}
import utopia.echo.model.vastai.process.{VastAiVllmChatExecutorStatus, VastAiVllmProcessRecorder, VastAiVllmProcessorStatus}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.{AccessQueue, MappingFunnel}
import utopia.flow.async.process.WaitTarget.WaitDuration
import utopia.flow.async.process._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Pair}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.event.model.ChangeResponsePriority.After
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.operator.MaybeEmpty
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.file.KeptOpenWriter
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.{Duration, Now, Today}
import utopia.flow.util.logging.Logger
import utopia.flow.util.result.TryExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.CopyOnDemand
import utopia.flow.view.mutable.{Pointer, Settable}

import java.net.ServerSocket
import java.nio.file.Path
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

object VastAiVllmChatExecutor
{
	// ATTRIBUTES   --------------------
	
	private val maxRetries = 8
	
	
	// OTHER    ------------------------
	
	/**
	 * Creates a new chat executor that utilizes multiple parallel Vast AI instances when executing requests
	 * @param selectOffer Logic for selecting Vast AI offers to take
	 * @param modelSize Size of the used model, including how much VRAM should be reserved for requests.
	 *                  If multiple models are used, the size of the largest model should be returned.
	 * @param coreInstanceCount Number of Vast AI instance to reserve immediately (or when the first request is received).
	 *                          Default = 1.
	 * @param maxInstanceCount Maximum number of instances that may be run at once. Default = 4.
	 * @param instanceActivationQueueSizeThreshold Number of queued requests,
	 *                                             at which an additional instance should be acquired.
	 *                                             Default = 24.
	 * @param instanceActivationPendingTokensThreshold Number of pending request tokens, at which point an additional
	 *                                                 instance should be acquired.
	 *                                                 Default = 24K.
	 * @param instanceAccelerationPendingTokensThreshold Number of pending request tokens, at which point an additional
	 *                                                   instance should be acquired, even while there's already an
	 *                                                   instance being prepared.
	 *                                                   Default = 48K.
	 * @param maxConnectionsPerInstance Maximum number of HTTP connections allowed to a single instance.
	 *                                  May limit parallelism in case of smaller requests. Default = 28.
	 * @param additionalReservedDisk Disk space reserved in addition to the model size. Default = 5 GB.
	 * @param defaultContextSize Context size to assume when no context size is specified in the request.
	 *                           Default = None = no context size is assumed,
	 *                           but the backup solution is used instead (if applicable)
	 *
	 *                           NB: It is always recommended to specify the context size for the requests.
	 *                               This can be done, for example, by utilizing a StatelessBufferedReplyGenerator.
	 * @param contextSafetyMargin Safety margin added to context calculations.
	 *                            If the request is at the edge a pool's threshold,
	 *                            may transfer it to a larger pool instead.
	 *                            Default = 64 tokens.
	 * @param backupExecutor Request executor used for requests that exceed the largest allowed context size,
	 *                       and for those which don't have a context size specified (if 'defaultContextSize' is None).
	 *
	 *                       If left empty (default), such requests will be immediately failed.
	 * @param recorder An interface which receives records of completed Vast AI processes.
	 *                 None (default) if no recording should be performed.
	 * @param installScriptPath Path to a script for installing vLLM on the rented device.
	 *                          Used (and required), only if 'chooseImage' indicates that vLLM should be installed.
	 *                          Default = None.
	 * @param remotePort Port at which the vLLM API is served at the remote instance. Default = 8000.
	 *                   Note: If vLLM is auto-hosted by the image / template, make sure to specify the correct port.
	 * @param maxGpuUtil Maximum GPU utilization, as a fraction between 0 and 1. Used when/if starting vLLM. Default = 0.9.
	 * @param setupTimeout Timeout for the setup process.
	 *                     If the API doesn't become usable before this timeout, the instance is destroyed.
	 *                     Default = infinite (not recommended).
	 * @param recoveryTimeout Timeout for recovering from SSH and/or vLLM failures. Default = 60 seconds.
	 * @param noResponseTimeout Timeout for started API requests.
	 *                          If this timeout is reached, the request queue is closed
	 *                          and the underlying Vast AI instance is destroyed.
	 *                          Default = infinite (not recommended, unless you have your own monitoring process in place).
	 * @param idleShutdownThreshold A time threshold, at which completely idle processes are stopped.
	 *                              Default = 15 min.
	 * @param label Custom label given to the rented Vast AI instance. Default = "chat-executor".
	 * @param logDir Directory where debug log entries will be placed (optional)
	 * @param startsLazily Whether this executor should only start when the first request is received.
	 *                     Default = false = instances are acquired immediately.
	 * @param chooseImage A function for choosing the image or Vast AI template to use.
	 *                    Accepts the selected offer and the applied maximum context size, yields:
	 *                          1. Instance-creation settings
	 *                          1. Expected initial vLLM service state at the remote instance
	 *                          1. Name of the model to start vLLM with.
	 *                             Optional if vLLM is started automatically by the image / template.
	 * @param thinks A function used for determining, which of the used models should be marked as "thinking"
	 * @param exc Implicit execution context
	 * @param vastAiClient Implicit Vast AI -interfacing client
	 * @param log Implicit logging implementation
	 * @return a new executor interface
	 */
	def apply(selectOffer: SelectOffer, modelSize: LlmVramUse, assumedVram: ByteCount, coreInstanceCount: Int = 1,
	          maxInstanceCount: Int = 4, instanceActivationQueueSizeThreshold: Int = 24,
	          instanceActivationPendingTokensThreshold: TokenCount = 24000,
	          instanceAccelerationPendingTokensThreshold: TokenCount = 48000, maxConnectionsPerInstance: Int = 28,
	          additionalReservedDisk: ByteCount = 5.gb, defaultContextSize: Option[TokenCount] = None,
	          contextSafetyMargin: TokenCount = 64,
	          backupExecutor: Option[BufferingChatRequestExecutor[BufferedOpenAiReply]] = None,
	          recorder: Option[VastAiVllmProcessRecorder], installScriptPath: Option[Path] = None,
	          remotePort: Int = 8000, maxGpuUtil: Double = 0.9, setupTimeout: Duration = 15.minutes,
	          recoveryTimeout: Duration = 60.seconds, noResponseTimeout: Duration = 10.minutes,
	          idleShutdownThreshold: Duration = 15.minutes, partialUseShutdownThreshold: Duration = 25.minutes,
	          label: String = "chat-executor", logDir: Option[Path] = None, startsLazily: Boolean = false)
	         (chooseImage: (Offer, TokenCount) => (NewInstanceFoundation, ServiceState, String))
	         (thinks: String => Boolean)
	         (implicit exc: ExecutionContext, vastAiClient: VastAiApiClient, log: Logger) =
		new VastAiVllmChatExecutor(selectOffer, modelSize, assumedVram, coreInstanceCount, maxInstanceCount,
			instanceActivationQueueSizeThreshold, instanceActivationPendingTokensThreshold,
			instanceAccelerationPendingTokensThreshold, maxConnectionsPerInstance, additionalReservedDisk,
			defaultContextSize, contextSafetyMargin, backupExecutor, recorder, installScriptPath, remotePort,
			maxGpuUtil, setupTimeout, recoveryTimeout, noResponseTimeout, idleShutdownThreshold,
			partialUseShutdownThreshold, label, logDir, startsLazily)(chooseImage)(thinks)
}

/**
 * Executes buffered chat requests using multiple Vast AI instances + vLLM servers.
 * @param selectOffer Logic for selecting Vast AI offers to take
 * @param modelSize Size of the used model, including how much VRAM should be reserved for requests.
 *                  If multiple models are used, the size of the largest model should be returned.
 * @param coreInstanceCount Number of Vast AI instance to reserve immediately (or when the first request is received).
 *                          Default = 1.
 * @param maxInstanceCount Maximum number of instances that may be run at once. Default = 4.
 * @param instanceActivationQueueSizeThreshold Number of queued requests,
 *                                             at which an additional instance should be acquired.
 *                                             Default = 24.
 * @param instanceActivationPendingTokensThreshold Number of pending request tokens, at which point an additional
 *                                                 instance should be acquired.
 *                                                 Default = 24K.
 * @param instanceAccelerationPendingTokensThreshold Number of pending request tokens, at which point an additional
 *                                                   instance should be acquired, even while there's already an
 *                                                   instance being prepared.
 *                                                   Default = 48K.
 * @param maxConnectionsPerInstance Maximum number of HTTP connections allowed to a single instance.
 *                                  May limit parallelism in case of smaller requests. Default = 28.
 * @param additionalReservedDisk Disk space reserved in addition to the model size. Default = 5 GB.
 * @param defaultContextSize Context size to assume when no context size is specified in the request.
 *                           Default = None = no context size is assumed,
 *                           but the backup solution is used instead (if applicable)
 *
 *                           NB: It is always recommended to specify the context size for the requests.
 *                               This can be done, for example, by utilizing a StatelessBufferedReplyGenerator.
 * @param contextSafetyMargin Safety margin added to context calculations.
 *                            If the request is at the edge a pool's threshold,
 *                            may transfer it to a larger pool instead.
 *                            Default = 64 tokens.
 * @param backupExecutor Request executor used for requests that exceed the largest allowed context size,
 *                       and for those which don't have a context size specified (if 'defaultContextSize' is None).
 *
 *                       If left empty (default), such requests will be immediately failed.
 * @param recorder An interface which receives records of completed Vast AI processes.
 *                 None (default) if no recording should be performed.
 * @param installScriptPath Path to a script for installing vLLM on the rented device.
 *                          Used (and required), only if 'chooseImage' indicates that vLLM should be installed.
 *                          Default = None.
 * @param remotePort Port at which the vLLM API is served at the remote instance. Default = 8000.
 *                   Note: If vLLM is auto-hosted by the image / template, make sure to specify the correct port.
 * @param maxGpuUtil Maximum GPU utilization, as a fraction between 0 and 1. Used when/if starting vLLM. Default = 0.9.
 * @param setupTimeout Timeout for the setup process.
 *                     If the API doesn't become usable before this timeout, the instance is destroyed.
 *                     Default = infinite (not recommended).
 * @param recoveryTimeout Timeout for recovering from SSH and/or vLLM failures. Default = 60 seconds.
 * @param noResponseTimeout Timeout for started API requests.
 *                          If this timeout is reached, the request queue is closed
 *                          and the underlying Vast AI instance is destroyed.
 *                          Default = infinite (not recommended, unless you have your own monitoring process in place).
 * @param idleShutdownThreshold A time threshold, at which completely idle processes are stopped.
 *                              Default = 15 min.
 * @param label Custom label given to the rented Vast AI instance. Default = "chat-executor".
 * @param logDir Directory where debug log entries will be placed (optional)
 * @param startsLazily Whether this executor should only start when the first request is received.
 *                     Default = false = instances are acquired immediately.
 * @param chooseImage A function for choosing the image or Vast AI template to use.
 *                    Accepts the selected offer and the applied maximum context size, yields:
 *                          1. Instance-creation settings
 *                          1. Expected initial vLLM service state at the remote instance
 *                          1. Name of the model to start vLLM with.
 *                             Optional if vLLM is started automatically by the image / template.
 * @param thinks A function used for determining, which of the used models should be marked as "thinking"
 * @param exc Implicit execution context
 * @param vastAiClient Implicit Vast AI -interfacing client
 * @param log Implicit logging implementation
 * @author Mikko Hilpinen
 * @since 03.03.2026, v1.5
 */
class VastAiVllmChatExecutor(selectOffer: SelectOffer, modelSize: LlmVramUse, assumedVram: ByteCount,
                             coreInstanceCount: Int = 1, maxInstanceCount: Int = 4,
                             instanceActivationQueueSizeThreshold: Int = 24,
                             instanceActivationPendingTokensThreshold: TokenCount = 24000,
                             instanceAccelerationPendingTokensThreshold: TokenCount = 48000,
                             maxConnectionsPerInstance: Int = 28, additionalReservedDisk: ByteCount = 5.gb,
                             defaultContextSize: Option[TokenCount] = None, contextSafetyMargin: TokenCount = 64,
                             backupExecutor: Option[BufferingChatRequestExecutor[BufferedOpenAiReply]] = None,
                             recorder: Option[VastAiVllmProcessRecorder], installScriptPath: Option[Path] = None,
                             remotePort: Int = 8000, maxGpuUtil: Double = 0.9, setupTimeout: Duration = 15.minutes,
                             recoveryTimeout: Duration = 60.seconds, noResponseTimeout: Duration = 10.minutes,
                             idleShutdownThreshold: Duration = 15.minutes,
                             partialUseShutdownThreshold: Duration = 25.minutes, label: String = "chat-executor",
                             logDir: Option[Path] = None, startsLazily: Boolean = false)
                            (chooseImage: (Offer, TokenCount) => (NewInstanceFoundation, ServiceState, String))
                            (thinks: String => Boolean)
                            (implicit exc: ExecutionContext, vastAiClient: VastAiApiClient, log: Logger)
	extends BufferingChatRequestExecutor[BufferedOpenAiReply] with Breakable
{
	// ATTRIBUTES   -----------------------
	
	/**
	 * Maximum context size applied by default / before there are any instances reserved.
	 * The actual maximum context size is based on [[modelSize]] and available VRAM.
	 */
	private val defaultMaxContextSize = contextSizeOn(assumedVram)
	
	/**
	 * Caches the generated LLM designators
	 */
	private val llmCache = Cache { model: String => LlmDesignator(model, thinks = thinks(model)) }
	/**
	 * Set to true once this executor should terminate
	 */
	private val stopFlag = Settable()
	/**
	 * A pointer for acquiring unique port numbers
	 */
	private val portCounter = {
		// Continuously generates new port numbers
		val p = Volatile(18001)
		Iterator.continually { p.getAndUpdate { _ + 1 } }
			// Makes sure only to yield available ports
			.filter { port => Try { new ServerSocket(port).close() }.isSuccess }
	}
	
	/**
	 * Counts the number of consecutive failures to acquire a Vast AI instance.
	 * If this reaches 50, this executor stops.
	 */
	private val consecutiveGetInstanceFailuresP = Volatile(0)
	
	/**
	 * Contains requests received while no processors were available.
	 * Cleared once processors become available.
	 */
	private val _queue = Volatile.eventful.emptySeq[QueuedTask]
	private val hasQueueFlag = _queue.nonEmptyFlag
	
	/**
	 * Contains the number of instances that should be started at any time.
	 * This is increased based on demand, and decreased when instances become idle.
	 */
	private val targetInstanceCountP = Volatile.eventful(if (startsLazily) 0 else coreInstanceCount)
	/**
	 * A decreasing counter for request tokens that triggers a target instance count update when it reaches 0.
	 */
	private val pendingTokensCheckThresholdP = Volatile(0)
	/**
	 * A decreasing counter for queued requests that triggers a target instance count update when it reaches 0.
	 */
	private val queueSizeCheckThresholdP = Volatile(0)
	
	private val gateway = Gateway(maxConnectionsPerRoute = maxConnectionsPerInstance,
		maxConnectionsTotal = maxConnectionsPerInstance * maxInstanceCount, disableTrustStoreVerification = true)
	
	/**
	 * Used for limiting instance-creation to one instance at a time
	 */
	private val createInstanceAccess = new AccessQueue(())
	
	/**
	 * Contains the utilized processors.
	 */
	private val processorsP = Volatile.eventful.emptySeq[Processor]
	/**
	 * A pointer that lists the processors that are/were usable.
	 * Updated (manually) whenever API-hosting starts or ends.
	 */
	private val usableProcessorsP = CopyOnDemand { processors.filter { _.usable } }
	
	/**
	 * A pointer that contains the maximum context size of the largest available Vast AI instance,
	 * or the default context size.
	 *
	 * Needs to be updated manually as processes become available or unavailable.
	 */
	private val maxContextSizeP = CopyOnDemand[TokenCount] {
		processors.iterator.filter { _.phase < Stopping }.flatMap { _.maxContextSize }.maxOption
			.getOrElse(defaultMaxContextSize)
	}
	/**
	 * A pointer that contains the maximum context size of the largest available Vast AI instance,
	 * or the default context size.
	 */
	// A public-facing version of maxContextSizeP. Omits the safety margin, which is added to the incoming requests.
	val maxContextSizePointer = maxContextSizeP.lightMap { _ - contextSafetyMargin * 2 - 1 }
	
	private val debugLogger = logDir.map { dir => KeptOpenWriter((dir/s"$Today-Vast-AI-log.txt").unique, 30.seconds) }
	
	/**
	 * A process that shuts down idle and partially used processors.
	 * None if no such process is needed.
	 * Must be restarted at times.
	 */
	private val idleShutdownProcess = {
		if (idleShutdownThreshold.isFinite || partialUseShutdownThreshold.isFinite)
			Some(LoopingProcess(View(idleShutdownThreshold)) { _ =>
				// Checks whether any processes are currently idle or only partially used
				val now = Now.toInstant
				val idleThreshold = now - idleShutdownThreshold
				val (loading, active) = processors.divideBy { p => p.phase == ApiHosting && !p.wasRequestedToStop }
					.toTuple
				val (remaining, idle) = active.divideBy { p => p.lastRequestTime < idleThreshold && p.isEmpty }.toTuple
				
				// Case: Some processes were idle => Stops them and reduces the target count accordingly
				if (idle.nonEmpty) {
					targetInstanceCountP.update { target => (target - idle.size - loading.size) max 0 }
					debugLog(s"Shutting down ${ idle.size } idle processors and ${ loading.size } loading processors")
					(idle.iterator ++ loading)
						.foreach { _.stop().forFailure { log(_, "Failure while stopping an idle processor") } }
					queueSizeCheckThresholdP.value = 0
					pendingTokensCheckThresholdP.value = 0
				}
				// Case: No idle processes => Checks whether some have not been fully utilized for some time
				else if (remaining.size > coreInstanceCount ||
					(loading.nonEmpty && remaining.size == coreInstanceCount))
				{
					lazy val partialUseThreshold = now - partialUseShutdownThreshold
					remaining.find { p => p.notFullyUtilized && p.lastPendingEndTime < partialUseThreshold }
						// Case: A partially used processor found
						.foreach { partiallyUsedProcessor =>
							// Shuts down the loading processes (we don't need more processes at this time)
							if (loading.nonEmpty) {
								debugLog(s"Shutting down ${ loading.size } loading processors (not enough demand)")
								targetInstanceCountP.update { target => (target - loading.size) max 0 }
								loading.foreach {
									_.stop().forFailure { log(_, "Failure while stopping a loading processor") }
								}
							}
							
							// Makes sure we're really above the core processor count
							// before shutting down active processes
							val shouldTerminateActive = targetInstanceCountP.mutate { target =>
								if (target > coreInstanceCount)
									true -> (target - 1)
								else
									false -> target
							}
							if (shouldTerminateActive) {
								debugLog("Shutting down a partially used processor")
								// Terminates the partially used instance
								partiallyUsedProcessor.stop()
									.forFailure { log(_, "Failure while stopping a partially used processor") }
								queueSizeCheckThresholdP.value = 0
								pendingTokensCheckThresholdP.value = 0
							}
						}
				}
				
				// Schedules the next check
				val nextCheck = Pair[Option[Instant]](
					remaining.iterator.map { _.lastRequestTime }.minOption.map { _ + idleShutdownThreshold },
					remaining.iterator.filter { _.notFullyUtilized }.map { _.lastPendingEndTime }.minOption
						.map { _ + partialUseShutdownThreshold })
					.iterator.flatten.minOption.getOrElse { now + idleShutdownThreshold }
				Some(nextCheck)
			})
		// Case: Idle shutdown is disabled => No process needed
		else
			None
	}
	
	/**
	 * Contains the future of the current / latest queue-clearance process.
	 * Used for limiting clearing to a single process.
	 */
	private val clearQueueFutureP = Volatile(Future.successful(true))
	/**
	 * A listener for usable processors, which activates queue-clearing, if possible & appropriate.
	 */
	private val clearQueueListener: ChangeListener[Seq[Processor]] = ChangeListener[Seq[Processor]] { e =>
		// Case: Processors are now available => Prepares to clear the queue
		if (e.newValue.exists { _.usable })
			clearQueueFutureP.mutate { f =>
				// Case: Previous clearance had completed => Starts a new clearance process
				if (f.isCompleted) {
					debugLog("Starts clearing the queue")
					val newFuture = clearQueue()
					newFuture.forFailure { log(_, "Unexpected failure while clearing the queue") }
					Some(newFuture) -> newFuture
				}
				// Case: Clearance already pending => Won't do anything
				else
					None -> f
			} match {
				// Case: New clearance => Schedules another if it fails (unless stopped)
				case Some(clearanceCompletion) =>
					Detach.and(After) {
						clearanceCompletion.onComplete { result =>
							debugLog(s"Queue clearance completed with $result")
							result.logWithMessage("Unexpected failure while clearing the queue")
							if (result.toOption.forall { !_ } && stopFlag.isNotSet)
								scheduleQueueClearance()
						}
					}
				case None => Detach
			}
		// Case: Clients are not yet available => Continues listening
		else
			Continue
	}
	
	private val regeneratorWaitLock = new AnyRef
	private val regenerator = LoopingProcess(waitLock = regeneratorWaitLock) { hurryFlag =>
		// Case: Stopped => Exits this loop
		if (stopFlag.isSet || hurryFlag.value)
			None
		else {
			// Checks how much capacity there is for new processes
			val capacity = cleanProcesses()
			
			// Case: No capacity => Waits until the next call (or checks after 5 mins)
			if (capacity <= 0) {
				debugLog("No capacity for generating")
				Some(WaitDuration(5.minutes).breakable)
			}
			// Case: Capacity available => Starts a new instance
			else {
				// Waits until the new instance has been created
				debugLog("Generating")
				val continueDelay = startNewInstance().waitFor() match {
					case Success(process) =>
						process.completionFuture.onComplete { _ => WaitUtils.notify(regeneratorWaitLock) }
						10.seconds
					case Failure(error) =>
						log(error, "Failed to start a Vast AI instance")
						30.seconds
				}
				
				// Schedules the next loop
				// Case: More capacity is available => Continues to generate the next instance after a short delay
				if (cleanProcesses() > 0)
					Some(WaitDuration(continueDelay))
				// Case: No more capacity => Waits until one of the processes completes (checks after 5 mins anyway)
				else
					Some(WaitDuration(5.minutes).breakable)
			}
		}
	}
	
	
	// INITIAL CODE -----------------------
	
	registerToStopOnceJVMCloses()
	
	// Whenever the target instance count increases (which may be immediately), starts preparing new instances
	targetInstanceCountP.addListenerAndSimulateEvent(0) { e =>
		if (stopFlag.isSet)
			Detach
		else {
			if (e.newValue > e.oldValue) {
				WaitUtils.notify(regeneratorWaitLock)
				regenerator.runAsync()
			}
			Continue
		}
	}
	
	// Whenever the queue starts filling, schedules clearance
	hasQueueFlag.addListener { e =>
		if (e.newValue)
			scheduleQueueClearance()
		Continue
	}
	
	idleShutdownProcess.foreach { _.runAsync() }
	
	// If debug logging is enabled, starts tracking various pointers
	debugLogger.foreach { debug =>
		_queue.addContinuousListener { e => debugLogUsing(debug, s"${ e.newValue.size } requests are queued now") }
		targetInstanceCountP.addContinuousListener { e =>
			debugLogUsing(debug, s"Now targeting ${ e.newValue } instances instead of the previous ${ e.oldValue }")
		}
		processorsP.addContinuousListener { e => debugLogUsing(debug, s"Now using ${ e.newValue.size } processors") }
		usableProcessorsP.addContinuousListener { e =>
			debugLogUsing(debug, s"${ e.newValue.size } processors are now fully usable")
		}
		
		// Also prints the status regularly
		Loop.after(1.minutes) {
			debug { writer =>
				writer.println(s"${ Now.toLocalTime }: Status:")
				status.processorStates.foreach { status =>
					writer.println(s"\t- ${ status.phase.name }: ${ status.activeTokens } active + ${
						status.pendingTokens } pending")
				}
			}
			if (stopFlag.isSet && processors.isEmpty)
				None
			else
				Some(1.minutes)
		}
	}
	
	
	// COMPUTED ---------------------------
	
	/**
	 * @return The current status of this executor
	 */
	def status = VastAiVllmChatExecutorStatus(processors.map { _.status }, _queue.size)
	
	/**
	 * Maximum request (context) size that is possible to handle without using backup executors
	 */
	def maxContextSize = safeMaxContextSize - contextSafetyMargin * 2 - 1
	/**
	 * @return Maximum context size used internally
	 */
	private def safeMaxContextSize = maxContextSizeP.value
	
	/**
	 * @return Whether there's at least one Vast AI instance ready to process incoming requests
	 */
	def usable = usableProcessorsP.value.exists { _.usable }
	
	private def processors = processorsP.value
	
	
	// IMPLEMENTED  -----------------------
	
	override def apply(params: ChatParams): Future[RequestResult[BufferedOpenAiReply]] = {
		// Checks the required context size
		// Also adds a safety margin
		val requiredContext = params(ContextTokens).int match {
			case Some(context) => Some(context + contextSafetyMargin)
			case None => defaultContextSize
		}
		requiredContext match {
			case Some(tokens) =>
				// Case: Too large a request => Uses the backup executor, if available
				if (tokens > safeMaxContextSize)
					delegateToBackup(params, Response.Failure(BadRequest,
						s"Maximum context size of $safeMaxContextSize is exceeded by $tokens"))
				// Case: Suitable context size => Delegates processing to one of available clients
				else
					push(Request(params, tokens))
			
			// Case: No context size known (never recommended) => Uses the backup executor, if available
			case None =>
				delegateToBackup(params,
					RequestSendingFailed(new IllegalArgumentException("Request context size was not specified")))
		}
	}
	
	override def stop(): Future[Any] = {
		debugLog("Stop called")
		val regeneratorStopFuture = regenerator.stop()
		if (stopFlag.set()) {
			val processorStopFutures = processors.map { _.stop() }
			_queue.popAll().foreach { _.fail() }
			(processorStopFutures :+ regeneratorStopFuture).future
		}
		else
			regeneratorStopFuture
	}
	
	
	// OTHER    ---------------------------
	
	private def delegateToBackup(request: ChatParams, failure: => RequestFailure) =
		backupExecutor match {
			case Some(backup) => backup(request)
			case None => Future.successful(failure)
		}
	
	// Notice: This request is pretty deeply recursive, being called from Processor
	private def push(request: Request): Future[RequestResult[BufferedOpenAiReply]] = {
		// Case: Already stopped => Fails
		if (stopFlag.isSet)
			Future.successful(RequestSendingFailed(
				new IllegalStateException("This request-execution interface was closed")))
		else {
			val processors = this.processors
			
			// Attempts to give the request to one of the processors, preferring those least used
			processors.sortBy { _.pendingToActiveRatio }
				.findMap { processor =>
					processor.tryPush(request).map { resultFuture =>
						// Activates more instances, if appropriate
						adjustInstanceTargetIfAppropriate(pendingTokensCheckThresholdP, request.tokens.value,
							instanceActivationPendingTokensThreshold.value, 5000,
							processors.iterator.map { _.pendingTokens }.sum.toInt) {
							currentTarget =>
								if (processors.size < currentTarget || processors.exists { _.phase < ApiHosting })
									currentTarget * instanceAccelerationPendingTokensThreshold.value
								else
									currentTarget * instanceActivationPendingTokensThreshold.value
						}
						
						// Checks whether it's possible to handle cases where the context window couldn't
						// fit into the server's maximum context (400 error, because of token-counting issues)
						processor.usableContextSize.flatMap { currentContextSize =>
							// Attempts to reserve at least 20% more space
							val nextContextSize = (currentContextSize * 1.2) min safeMaxContextSize
							if (processors.exists { _.usableContextSize.exists { _ >= nextContextSize } })
								Some(nextContextSize)
							else
								None
						} match {
							// Case: A processor with a larger max context size is available
							//       => Applies backup logic for 400 context size issues
							case Some(nextContextSize) =>
								resultFuture.flatMap {
									case failure: Response.Failure if failure.status == BadRequest =>
										debugLog(s"${ failure.message } => Delegates the request to a processor with a larger context window")
										log(s"Warning: ${ failure.message } => Delegated the request to a processor with a larger context window")
										request.asRetryWithIfPossible(failure.cause) match {
											case Some(retry) => push(retry.withTokens(nextContextSize))
											case None => Future.successful(failure)
										}
									case result => Future.successful(result)
								}
							// Case: No backup processor is available => Won't add recovery processes
							case None => resultFuture
						}
					}
				}
				// Case: No processors are available => Queues this request
				.getOrElse {
					val task = new QueuedTask(request)
					val queueSize = _queue.updateAndGet { _ :+ task }.size
					
					// Activates more instances, if appropriate
					adjustInstanceTargetIfAppropriate(queueSizeCheckThresholdP, 1,
						instanceActivationQueueSizeThreshold, 5, queueSize) { _ * instanceActivationQueueSizeThreshold }
					
					task.future
				}
		}
	}
	/**
	 * Actually executes a request using a specific Vast AI instance.
	 * Called from individual processors.
	 * @param request Request to process
	 * @param client Client that should handle this request
	 * @param model Model hosted by the utilized instance
	 * @param maxContextSize Maximum context size of the utilized instance
	 * @return Future of the eventual request result
	 */
	private def push(request: Request, client: RequestQueue, model: String,
	                 maxContextSize: TokenCount): Future[RequestResult[BufferedOpenAiReply]] =
	{
		// Converts the request into a full chat request
		val apiRequest = BufferedOpenAiChatCompletionRequest(
			request.params.toLlm(llmCache(model)).mapSetting(ContextTokens) { _.int match {
				case Some(maxTokens) => maxTokens min maxContextSize.value
				case None => maxContextSize.value
			} })
		
		// Adds handling for situations where instance-closing leads to request deprecation
		// Causes such requests to be attempted again
		client.push(apiRequest).future.flatMap {
			// Case: Request was marked as deprecated at a lower process level => Attempts that request again
			case RequestWasDeprecated if !apiRequest.deprecated && stopFlag.isNotSet =>
				request.asRetryIfPossible match {
					case Some(retry) => push(retry)
					// Case: Can't retry anymore => Returns a failure or a deprecation, depending on earlier results
					case None =>
						request.firstError match {
							case Some(error) => Future.successful(RequestSendingFailed(error))
							case None => Future.successful(RequestWasDeprecated)
						}
				}
			// Case: Request completed or terminated for another reason, or this system stopped => Finishes
			case result => Future.successful(result)
		}
	}
	
	/**
	 * Adjusts [[targetInstanceCountP]], if that is deemed appropriate
	 * @param checkThresholdP A pointer that's used for limiting these checks
	 * @param adjustment Adjustment that should be applied to 'checkThresholdP'
	 * @param thresholdIncrement Increase to 'checkThresholdP' after a check has been applied (call-by-name)
	 * @param thresholdAtMaxInstances Next 'checkThresholdP' value applied in case we're already at max instances
	 *                                (call-by-name)
	 * @param current Current instance utilization value (either pending tokens or queue size, depending on the context).
	 *                Call-by-name.
	 * @param thresholdFrom A function for calculating the next target instance increase threshold.
	 *                      Receives the current target instance count.
	 */
	private def adjustInstanceTargetIfAppropriate(checkThresholdP: Pointer[Int], adjustment: Int,
	                                              thresholdIncrement: => Int, thresholdAtMaxInstances: => Int,
	                                              current: => Int)
	                                             (thresholdFrom: Int => Int) =
		if (checkThresholdP.updateAndGet { _ - adjustment } <= 0) {
			// Checks whether new instances should be generated
			checkThresholdP.value = targetInstanceCountP.mutate { currentTarget =>
				// Case: Already at max instances => Keeps the current target
				if (currentTarget >= maxInstanceCount)
					thresholdAtMaxInstances -> currentTarget
				// Case: Below the core instance level => Activates the core instances
				else if (currentTarget < coreInstanceCount)
					thresholdIncrement -> coreInstanceCount
				else {
					// If there are instances which are getting ready, uses a different activation threshold
					val threshold = thresholdFrom(currentTarget)
					val overThreshold = current - threshold
					
					// Case: Enough for activation => Adjusts the target instance count
					if (overThreshold >= 0)
						(thresholdIncrement - overThreshold, currentTarget + 1)
					// Case: Not enough => Keeps the current target
					else
						-overThreshold -> currentTarget
				}
			}
		}
	
	/**
	 * Schedules the next queue-clearance, which is initiated when at least one processor becomes usable.
	 */
	private def scheduleQueueClearance(): Unit =
		usableProcessorsP.addListenerAndSimulateEvent(Empty)(clearQueueListener)
	
	/**
	 * A recursive process for clearing the request queue
	 * @return A future that resolves once either:
	 *              - No processors are available
	 *              - The queue becomes empty
	 *
	 *         Yields whether the queue is now empty.
	 */
	// TODO: More tasks should be given to processors with less pending tokens
	private def clearQueue(): Future[Boolean] = {
		// Checks the usable processors
		val processors = {
			val usable = this.processors.filter { _.usable }
			// If there are multiple processors, selects those least used
			val selected = {
				if (usable.hasSize <= 3)
					usable
				else {
					val sorted = usable.sortBy { _.pendingTokens }
					// Selects all processors that have no pending tasks
					val free = sorted.takeWhile { _.pendingTokens <= 0 }
					val freeCount = free.size
					// Always selects at least 3 processors
					if (freeCount >= 3)
						free
					else
						free ++ sorted.slice(freeCount, 3)
				}
			}
			selected.sortBy { _.maxContextSize.getOrElse(TokenCount.zero) }
		}
		processors.lastOption.flatMap { _.maxContextSize } match {
			case Some(maxProcessorSize) =>
				// Collects the tasks to process
				// If there are tasks that are too large for the current processors, delays them
				val tasks = _queue.mutate { queue =>
					val iter = queue.iterator
					val keepBuilder = OptimizedIndexedSeq.newBuilder[QueuedTask]
					val processBuilder = OptimizedIndexedSeq.newBuilder[QueuedTask]
					var remainingCapacity = processors.size
					
					while (remainingCapacity > 0 && iter.hasNext) {
						val next = iter.next()
						if (next.tokens > maxProcessorSize)
							keepBuilder += next
						else {
							processBuilder += next
							remainingCapacity -= 1
						}
					}
					
					processBuilder.result().sortBy { _.tokens } -> (iter ++ keepBuilder.result()).toOptimizedSeq
				}
				// Case: No tasks to process => Completes
				if (tasks.isEmpty) {
					// If there were too large tasks, fails them
					_queue.popAll().foreach { _.fail() }
					Future.successful(true)
				}
				else {
					// Assigns the tasks to the available processors
					debugLog(s"Resolves the next ${ tasks.size } queued requests")
					tasks.iterator.zipWithIndex.foreach { case (task, i) =>
						processors.view.drop(i).find { _.maxContextSize.exists { _ >= task.tokens } } match {
							case Some(processor) => task.resolveUsing(processor)
							case None => task.fail()
						}
					}
					
					// After a short delay, continues to unqueue more tasks
					// Applies a longer delay, if all processors are busy
					val delay = {
						if (processors.exists { p => p.maxContextSize.exists { p.pendingTokens < _.value } })
							1.seconds
						else
							10.seconds
					}
					Delay.future(delay) { clearQueue() }
				}
				
			// Case: No clients are usable => Completes
			case None => Future.successful(_queue.isEmpty)
		}
	}
	
	/**
	 * Starts a new Vast AI instance, adding it to the processor pool.
	 * Also handles automatic shutdown & removal if the instance becomes unstable, etc.
	 * @return A future that resolves when the Vast AI instance is available (although not usable),
	 *         or if failed to acquire said instance.
	 */
	private def startNewInstance() = createInstanceAccess { _ =>
		debugLog("Starting a new instance")
		// Creates and starts the process of setting up vLLM on Vast AI
		val process = VastAiVllmProcess(selectOffer, modelSize.modelSize, additionalReservedDisk, gateway,
			installScriptPath, localPort = portCounter.next(), remotePort = remotePort, maxGpuUtil = maxGpuUtil,
			setupTimeout = setupTimeout, recoveryTimeout = recoveryTimeout, noResponseTimeout = noResponseTimeout,
			instanceLabel = label, debugLogger = debugLogger) {
			offer =>
				val maxContextSize = contextSizeOn(offer.gpu.ram)
				val (image, initialVllmState, model) = chooseImage(offer, maxContextSize)
				(image, initialVllmState, maxContextSize, model)
		}
		processorsP :+= new Processor(process)
		debugLog(s"Now at ${ processors.size } instance processes")
		process.runAsync()
		
		// Updates the usableClients when API-hosting starts or ends
		process.detailedStatePointer.addListenerAndSimulateEvent(NotStarted) { e =>
			// Checks whether the instance became available => Updates max context, if so
			if (e.values.isAsymmetricBy { _.isInstanceAvailable })
				maxContextSizeP.update()
			
			// Checks for client-usability
			val clientStates = e.values.map {
				case HostingApi(instance, client, model, _) if instance.status.instanceShouldBeUsed =>
					Some(client -> model)
				case _ => None
			}
			if (clientStates.isAsymmetric) {
				debugLog(s"Updating client count (${ e.newValue.phase })")
				usableProcessorsP.update()
			}
			
			// Case: Stop process initiated => Updates max context size & stops listening
			if (e.newValue.phase >= Stopping) {
				maxContextSizeP.update()
				debugLog("Stops listening to the Vast AI process")
				Detach
			}
			else
				Continue
		}
		
		// Informs the recorder once this process finishes, and when/if an API is successfully hosted
		recorder.foreach { recorder =>
			process.detailedStatePointer.addListenerAndSimulateEvent(NotStarted) { e =>
				e.newValue match {
					case HostingApi(instance, _, _, _) =>
						recorder.onApiSetup(instance, process.startTime)
						Detach
					case state if state.phase > ApiHosting => Detach
					case _ => Continue
				}
			}
			process.recordFuture.foreach(recorder.onProcessCompleted)
		}
		
		// The next instance may be acquired once the process is in loading state / instance has been acquired
		process.detailedStatePointer
			.futureWhere { state => state.isInstanceAvailable || state.phase >= Stopping }
			.flatMap { state =>
				if (state.isInstanceAvailable) {
					consecutiveGetInstanceFailuresP.value = 0
					Future.successful(process)
				}
				// Case: Failed to acquire an instance
				//       => Waits for a while before attempting to get the next instance
				else {
					log("Warning: Failed to acquire an instance")
					// Checks whether there were too many instance-acquisition failures
					val consecutiveFailures = consecutiveGetInstanceFailuresP.updateAndGet { _ + 1 }
					// Case: Too many failures => Stops this whole system
					if (consecutiveFailures > 50) {
						debugLog("Too many failures to acquire an instance => Stops the whole system")
						stop()
						Future.successful(process)
					}
					else
						Delay(10.seconds) { process }
				}
			}
	}
	
	/**
	 * Removes completed processes from the pool
	 * @return Currently remaining capacity (i.e. how many more processes may be started)
	 */
	private def cleanProcesses() = processorsP.mutate { processes =>
		val remaining = processes.filterNot { _.terminated }
		debugLogger.foreach { log =>
			val removed = processes.filterNot(remaining.contains)
			if (removed.nonEmpty) {
				log { writer =>
					writer.println(s"${ Now.toLocalTime }: Removed ${ removed.size } completed Vast AI processes:")
					removed.foreach { p => writer.println(s"\t- ${ p.instanceId.mkString }: ${ p.vastAiState }") }
				}
			}
		}
		(targetInstanceCountP.value - remaining.size) -> remaining
	}
	
	/**
	 * Calculates the maximum context size to apply on different devices
	 * @param vram Amount of VRAM on the machine
	 * @return Maximum context size to apply
	 */
	private def contextSizeOn(vram: ByteCount) =
		modelSize.maxContextSizeOn(vram * maxGpuUtil) - contextSafetyMargin
		
	private def debugLog(entry: => String) = debugLogger.foreach { debugLogUsing(_, entry) }
	private def debugLogUsing(logger: KeptOpenWriter, entry: String) =
		logger { _.println(s"${ Now.toLocalTime }: $entry") }
	
	
	// NESTED   ---------------------------
	
	/**
	 * Manages / utilizes an individual Vast AI process for request-handling
	 * @param process The utilized Vast AI process
	 */
	private class Processor(process: VastAiVllmProcess) extends Breakable with MaybeEmpty[Processor]
	{
		// ATTRIBUTES   ------------------
		
		private val started = Now.toInstant
		private var stopped = false
		private var _lastRequestTime = started
		private var _lastPendingStartTime = started
		private var _lastPendingEndTime = started
		
		// Once / if a vLLM server becomes available, creates a funnel to wrap it
		private val funnelP = process.clientPointer.map { _.map { _.map { case (queue, model, maxContextSize) =>
			// Updates the last request -time now that the API is available.
			// Also, makes sure the idle-clearing process is running.
			_lastRequestTime = Now
			_lastPendingStartTime = _lastRequestTime
			_lastPendingEndTime = _lastRequestTime
			idleShutdownProcess.foreach { _.runAsync() }
			
			// Prepares a funnel for the incoming requests
			val safeMaxContextSize = maxContextSize - contextSafetyMargin
			debugLog(s"Starting a funnel of $safeMaxContextSize tokens")
			val funnel = MappingFunnel(safeMaxContextSize.value) {
				request: Request => request.tokens.value } {
				request =>
					if (process.detailedState.isUsable) {
						_lastRequestTime = Now
						push(request, queue, model.name, safeMaxContextSize)
					}
					else
						request.asRetryWith(
							new IllegalStateException(s"Unusable process state: ${ process.detailedState }")) match
						{
							case Success(retry) => push(retry)
							case Failure(error) => Future.successful(RequestSendingFailed(error))
						}
			}
			
			// Takes notice when the funnel becomes full or only partially filled
			funnel.pendingFlag.addListener { e =>
				if (e.newValue)
					_lastPendingStartTime = Now
				else
					_lastPendingEndTime = Now
				
				if (stopped) Detach else Continue
			}
			
			funnel -> safeMaxContextSize
		} } }
		
		
		// COMPUTED ---------------------
		
		def instanceId = process.instanceId
		
		def phase = process.detailedState.phase
		def vastAiState = process.vastAiState
		def instanceStatus = process.instanceStatus
		
		/**
		 * @return Whether this processor is capable of handling (more) requests at this time
		 */
		def usable = !stopped && process.detailedState.isUsable && funnelP.value.exists { _.isSuccess }
		def terminated = process.state.isFinal
		
		def maxContextSize = process.maxContextSize
		def usableContextSize: Option[TokenCount] = {
			if (stopped || process.detailedState.isUnusable)
				None
			else
				funnel.map { _._2 }
		}
		
		def pendingTokens = funnel match {
			case Some((funnel, _)) => funnel.queuedCapacity
			case None => 0.0
		}
		def pendingToActiveRatio = funnel match {
			case Some((funnel, maxContextSize)) => funnel.queuedCapacity / maxContextSize.value
			case None => 1000000
		}
		
		def notFullyUtilized = funnel.exists { !_._1.containsPendingActions }
		
		def wasRequestedToStop = stopped
		def lastRequestTime = _lastRequestTime
		def lastPendingEndTime = _lastPendingEndTime
		
		def status = {
			val (active, pending) = funnel match {
				case Some((funnel, _)) => funnel.activeUtilization.toInt -> funnel.queuedCapacity.toInt
				case None => 0 -> 0
			}
			VastAiVllmProcessorStatus(phase, instanceStatus, active, pending,
				maxContextSize.getOrElse(defaultMaxContextSize), started, _lastRequestTime, _lastPendingStartTime,
				_lastPendingEndTime)
		}
		
		/**
		 * @return The currently available funnel, if applicable.
		 *         None if no funnel is available at this time.
		 */
		private def funnel: Option[(MappingFunnel[Request, RequestResult[BufferedOpenAiReply]], TokenCount)] =
			funnelP.value.flatMap { _.toOption }
		
		
		// IMPLEMENTED  ------------------
		
		override def self: Processor = this
		
		override def isEmpty: Boolean = funnel.forall { _._1.isEmpty }
		
		override def stop(): Future[Any] = {
			stopped = true
			val state = process.detailedState
			// Case: Not currently hosting an API => Requests the Vast AI process to stop
			if (state.phase != ApiHosting)
				process.stop()
			// Case: Hosting an API
			//       => Makes sure the queued requests get resolved first, before requesting the instance to stop
			else
				funnel match {
					case Some((funnel, _)) => funnel.emptyFuture.flatMap { _ => process.stop() }
					case None => process.stop()
				}
		}
		
		
		// OTHER    ----------------------
		
		/**
		 * Attempts to give a request for this processor to handle
		 * @param request Request to process
		 * @return Request result future. None if this processor can't handle that request at this time.
		 */
		def tryPush(request: Request) =
			funnel
				// Makes sure the funnel can handle this request
				.filter { case (_, maxContextSize) =>
					!stopped && request.tokens <= maxContextSize && process.detailedState.isUsable }
				.map { _._1.push(request) }
	}
	
	private class QueuedTask(request: Request)
	{
		// ATTRIBUTES   ---------------------------
		
		private val promise = Promise[Option[Processor]]()
		val future: Future[RequestResult[BufferedOpenAiReply]] = promise.future.flatMap {
			// Case: A processor became available => Gives it this request
			case Some(processor) =>
				processor.tryPush(request).getOrElse {
					log(s"The prepared processor couldn't receive the queued request of $tokens tokens")
					
					// Case: The processor couldn't receive this request (unexpected)
					//       => Attempts again with another processor (recursive)
					if (stopFlag.isSet)
						Future.successful(RequestSendingFailed(
							new IllegalStateException("This interface was stopped before this request could be handled")))
					else
						request.asRetryWith(
							new IllegalStateException("The activated processor rejected this request")) match
						{
							case Success(retry) => push(retry)
							case Failure(error) => Future.successful(RequestSendingFailed(error))
						}
				}
			// Case: No processor could handle this request => Fails
			case None =>
				val failure = {
					if (stopFlag.isSet)
						new IllegalStateException("This interface was stopped before this request could be handled")
					else
						new IllegalArgumentException(
							s"No processor was able to receive this request of $tokens tokens")
				}
				Future.successful(RequestSendingFailed(failure))
		}
		
		
		// COMPUTED ------------------------------
		
		def tokens = request.tokens
		
		
		// OTHER    ------------------------------
		
		def resolveUsing(processor: Processor) = promise.success(Some(processor))
		def fail() = promise.success(None)
	}
	
	// TODO: Possibly chain the errors
	private case class Request(params: ChatParams, tokens: TokenCount, pastRetries: Int = 0,
	                           firstError: Option[Throwable] = None)
	{
		// COMPUTED ----------------------
		
		/**
		 * @return Whether this request may still be retried
		 */
		def mayBeRetried = pastRetries < maxRetries
		
		/**
		 * Creates a copy of this request, representing another attempt
		 * @return A copy of this request, marked as a retry. Failure if too many failures were encountered.
		 */
		def asRetry = {
			if (pastRetries >= maxRetries)
				Failure(firstError.getOrElse { new IllegalStateException("Too many retries") })
			else
				Success(copy(pastRetries = pastRetries + 1))
		}
		/**
		 * @return If this request may still be retried, returns a copy marked as a retry
		 */
		def asRetryIfPossible = if (mayBeRetried) Some(copy(pastRetries = pastRetries + 1)) else None
		
		
		// OTHER    ----------------------
		
		/**
		 * Creates a copy of this request, representing another attempt
		 * @param error Error to fail with, if maximum number of retries was encountered (call-by-name)
		 * @return A copy of this request, marked as a retry. Failure if too many failures were encountered.
		 */
		def asRetryWith(error: => Throwable) = {
			if (mayBeRetried)
				Success(copy(pastRetries = pastRetries + 1, firstError = firstError.orElse(Some(error))))
			else
				Failure(firstError.getOrElse(error))
		}
		/**
		 * @param error Error to record as the first encountered error, if applicable (call-by-name)
		 * @return If this request may still be retried, returns a copy marked as a retry
		 */
		def asRetryWithIfPossible(error: => Throwable) = {
			if (mayBeRetried)
				Some(copy(pastRetries = pastRetries + 1, firstError = firstError.orElse(Some(error))))
			else
				None
		}
		
		def withTokens(tokens: TokenCount) = copy(tokens = tokens)
	}
}
