package utopia.echo.controller.vastai.vllm

import utopia.access.model.enumeration.Status.BadRequest
import utopia.annex.controller.RequestQueue
import utopia.annex.model.response.RequestNotSent.{RequestSendingFailed, RequestWasDeprecated}
import utopia.annex.model.response.{RequestResult, Response}
import utopia.annex.util.RequestResultExtensions._
import utopia.disciple.controller.Gateway
import utopia.echo.controller.chat.BufferingChatRequestExecutor
import utopia.echo.controller.client.VastAiApiClient
import utopia.echo.controller.vastai.SelectOffer
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
import utopia.echo.model.vastai.process.{VastAiVllmChatExecutorStatus, VastAiVllmProcessRecord, VastAiVllmProcessorStatus}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.TryFuture
import utopia.flow.async.context.{AccessQueue, MappingFunnel}
import utopia.flow.async.process.{Breakable, Delay, LoopingProcess}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Pair, Single}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.event.model.ChangeResponsePriority.After
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.operator.MaybeEmpty
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.{Duration, Now}
import utopia.flow.util.logging.Logger
import utopia.flow.util.result.TryCatch
import utopia.flow.util.result.TryExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.CopyOnDemand
import utopia.flow.view.mutable.{Pointer, Settable}

import java.net.ServerSocket
import java.nio.file.Path
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

object VastAiVllmChatExecutor
{
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
	          recorder: Option[VastAiVllmProcessRecord => Unit], installScriptPath: Option[Path] = None,
	          remotePort: Int = 8000, maxGpuUtil: Double = 0.9, setupTimeout: Duration = 15.minutes,
	          recoveryTimeout: Duration = 60.seconds, noResponseTimeout: Duration = 10.minutes,
	          idleShutdownThreshold: Duration = 15.minutes, partialUseShutdownThreshold: Duration = 25.minutes,
	          label: String = "chat-executor", startsLazily: Boolean = false)
	         (chooseImage: (Offer, TokenCount) => (NewInstanceFoundation, ServiceState, String))
	         (thinks: String => Boolean)
	         (implicit exc: ExecutionContext, vastAiClient: VastAiApiClient, log: Logger) =
		new VastAiVllmChatExecutor(selectOffer, modelSize, assumedVram, coreInstanceCount, maxInstanceCount,
			instanceActivationQueueSizeThreshold, instanceActivationPendingTokensThreshold,
			instanceAccelerationPendingTokensThreshold, maxConnectionsPerInstance, additionalReservedDisk,
			defaultContextSize, contextSafetyMargin, backupExecutor, recorder, installScriptPath, remotePort,
			maxGpuUtil, setupTimeout, recoveryTimeout, noResponseTimeout, idleShutdownThreshold,
			partialUseShutdownThreshold, label, startsLazily)(chooseImage)(thinks)
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
                             recorder: Option[VastAiVllmProcessRecord => Unit], installScriptPath: Option[Path] = None,
                             remotePort: Int = 8000, maxGpuUtil: Double = 0.9, setupTimeout: Duration = 15.minutes,
                             recoveryTimeout: Duration = 60.seconds, noResponseTimeout: Duration = 10.minutes,
                             idleShutdownThreshold: Duration = 15.minutes,
                             partialUseShutdownThreshold: Duration = 25.minutes, label: String = "chat-executor",
                             startsLazily: Boolean = false)
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
	private val _queue = Volatile.eventful.emptySeq[Promise[Processor]]
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
	val maxContextSizePointer = maxContextSizeP.lightMap { _ - contextSafetyMargin }
	
	/**
	 * Contains futures of active regeneration processes.
	 */
	private val regenerateFuturesP = Volatile.emptySeq[Future[Unit]]
	
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
				val (remaining, idle) = processors.iterator
					.filter { p => p.phase == ApiHosting && !p.wasRequestedToStop }
					.divideToSeqsBy { p => p.lastRequestTime < idleThreshold && p.isEmpty }.toTuple
				
				// Case: Some processes were idle => Stops them and reduces the target count accordingly
				if (idle.nonEmpty) {
					targetInstanceCountP.update { target => (target - idle.size) max 0 }
					idle.foreach { _.stop().forFailure { log(_, "Failure while stopping an idle processor") } }
					queueSizeCheckThresholdP.value = 0
					pendingTokensCheckThresholdP.value = 0
				}
				// Case: No idle processes & above core processor count
				//       => Checks whether some have not been fully utilized for some time
				else if (remaining.size > coreInstanceCount) {
					lazy val partialUseThreshold = now - partialUseShutdownThreshold
					remaining.find { p => p.notFullyUtilized && p.lastPendingEndTime < partialUseThreshold }
						.foreach { partiallyUsedProcessor =>
							// Makes sure we're really above the core processor count before shutting down anything
							val shouldTerminate = targetInstanceCountP.mutate { target =>
								if (target > coreInstanceCount)
									true -> (target - 1)
								else
									false -> target
							}
							if (shouldTerminate) {
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
					println("Starts clearing the queue")
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
							println(s"Queue clearance completed with $result")
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
	
	
	// INITIAL CODE -----------------------
	
	registerToStopOnceJVMCloses()
	
	// Whenever the target instance count increases (which may be immediately), starts preparing new instances
	targetInstanceCountP.addListenerAndSimulateEvent(0) { e =>
		if (stopFlag.isSet)
			Detach
		else {
			if (e.newValue > e.oldValue)
				regenerate()
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
	
	
	// COMPUTED ---------------------------
	
	/**
	 * @return The current status of this executor
	 */
	def status = VastAiVllmChatExecutorStatus(processors.map { _.status }, _queue.size)
	
	/**
	 * Maximum request (context) size that is possible to handle without using backup executors
	 */
	def maxContextSize = safeMaxContextSize - contextSafetyMargin
	/**
	 * @return Maximum context size used internally
	 */
	private def safeMaxContextSize = maxContextSizeP.value
	
	private def processors = processorsP.value
	
	
	// IMPLEMENTED  -----------------------
	
	override def apply(params: ChatParams): Future[Try[BufferedOpenAiReply]] = {
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
					delegateToBackup(params, new IllegalArgumentException(
						s"Maximum context size of $safeMaxContextSize is exceeded by $tokens"))
				// Case: Suitable context size => Delegates processing to one of available clients
				else
					push(params, tokens).toTryFuture
			
			// Case: No context size known (never recommended) => Uses the backup executor, if available
			case None =>
				delegateToBackup(params, new IllegalArgumentException("Request context size was not specified"))
		}
	}
	
	override def stop(): Future[Any] = {
		if (stopFlag.set()) {
			val processorStopFutures = processors.map { _.stop() }
			// TODO: Handle failures more gracefully
			_queue.popAll().foreach { _.failure(new InterruptedException("This process pool was stopped")) }
			(processorStopFutures ++ regenerateFuturesP.value).future
		}
		else
			regenerateFuturesP.value.future
	}
	
	
	// OTHER    ---------------------------
	
	private def delegateToBackup(request: ChatParams, failure: => Throwable) =
		backupExecutor match {
			case Some(backup) => backup(request)
			case None => TryFuture.failure(failure)
		}
	
	// Notice: This request is pretty deeply recursive, being called from Processor
	private def push(request: ChatParams, tokens: TokenCount): Future[RequestResult[BufferedOpenAiReply]] = {
		// Case: Already stopped => Fails
		if (stopFlag.isSet)
			Future.successful(RequestSendingFailed(
				new IllegalStateException("This request-execution interface was closed")))
		else {
			val processors = this.processors
			
			// Attempts to give the request to one of the processors, preferring those least used
			processors.sortBy { _.pendingToActiveRatio }
				.findMap { processor =>
					processor.tryPush(request, tokens).map { resultFuture =>
						// Activates more instances, if appropriate
						adjustInstanceTargetIfAppropriate(pendingTokensCheckThresholdP, tokens.value,
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
									case Response.Failure(status, message, _) if status == BadRequest =>
										log(s"Warning: $message => Delegated the request to a processor with a larger context window")
										push(request, nextContextSize)
									
									case result => Future.successful(result)
								}
							// Case: No backup processor is available => Won't add recovery processes
							case None => resultFuture
						}
					}
				}
				// Case: No processors are available => Queues this request
				.getOrElse {
					val clientPromise = Promise[Processor]()
					val queueSize = _queue.updateAndGet { _ :+ clientPromise }.size
					
					// Activates more instances, if appropriate
					adjustInstanceTargetIfAppropriate(queueSizeCheckThresholdP, 1,
						instanceActivationQueueSizeThreshold, 5, queueSize) { _ * instanceActivationQueueSizeThreshold }
					
					// Waits (async) until a processor becomes available
					clientPromise.future.flatMap { processor =>
						// Case: A processor became available => Gives it this request
						processor.tryPush(request, tokens).getOrElse {
							// Case: The processor couldn't receive this request (unexpected)
							//       => Attempts again with another processor (recursive)
							push(request, tokens)
						}
					}
				}
		}
	}
	/**
	 * Actually executes a request using a specific Vast AI instance.
	 * Called from individual processors.
	 * @param request Request to process
	 * @param tokens Tokens required for processing this request
	 * @param client Client that should handle this request
	 * @param model Model hosted by the utilized instance
	 * @param maxContextSize Maximum context size of the utilized instance
	 * @return Future of the eventual request result
	 */
	private def push(request: ChatParams, tokens: TokenCount, client: RequestQueue, model: String,
	                 maxContextSize: TokenCount): Future[RequestResult[BufferedOpenAiReply]] =
	{
		// Converts the request into a full chat request
		val apiRequest = BufferedOpenAiChatCompletionRequest(
			request.toLlm(llmCache(model)).mapSetting(ContextTokens) { _.int match {
				case Some(maxTokens) => maxTokens min maxContextSize.value
				case None => maxContextSize.value
			} })
		
		// Adds handling for situations where instance-closing leads to request deprecation
		// Causes such requests to be attempted again
		client.push(apiRequest).future.flatMap {
			// Case: Request was marked as deprecated at a lower process level => Attempts that request again
			case RequestWasDeprecated if !apiRequest.deprecated && stopFlag.isNotSet => push(request, tokens)
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
	private def clearQueue(): Future[Boolean] = {
		// Checks the usable processors
		val processors = this.processors.filter { _.usable }
		// Case: No clients are usable => Completes
		if (processors.isEmpty)
			Future.successful(_queue.isEmpty)
		else {
			// Collects the tasks to process
			val tasks = _queue.pop(processors.size)
			// Case: No tasks to process => Completes
			if (tasks.isEmpty)
				Future.successful(true)
			else {
				// Assigns the tasks to the available processors
				val pairedTasks = {
					if (tasks.size == processors.size)
						tasks.zip(processors)
					else
						tasks.zip(processors.sortBy { _.pendingToActiveRatio })
				}
				pairedTasks.foreach { case (promise, processor) => promise.success(processor) }
				
				// After a short delay, continues to unqueue more tasks
				Delay.future(5.seconds) { clearQueue() }
			}
		}
	}
	
	/**
	 * An entry function for the recursive [[_regenerate]].
	 */
	private def regenerate() = regenerateFuturesP.update { previous =>
		val result = _regenerate()
		result.forFailure { log(_, "Instance regeneration failed") }
		OptimizedIndexedSeq.concat(previous.view.filterNot { _.isCompleted }, Single(result))
	}
	/**
	 * Fills the process pool with new processes, until targeted instance count is reached.
	 * Continues recursively, keeping the pool filled.
	 * @return A future that resolves once all processes have completed
	 *         (usually some time after [[stop]]() has been called)
	 */
	private def _regenerate(): Future[Unit] = {
		println("Generating")
		// Checks how much capacity there is for new processes
		val capacity = cleanProcesses()
		// Case: No capacity => Returns immediately
		if (capacity <= 0) {
			println("No capacity")
			Future.unit
		}
		// Case: Capacity for a single process
		//       => Starts it and prepares to apply recursion when that process completes
		else if (capacity == 1) {
			// Starts a new process, when possible
			val processFuture = startNewInstance()
			processFuture.flatMap { process =>
				// Once the process completes, cleans the process pool and attempts to generate more processes
				process.completionFuture.flatMap { _ =>
					if (cleanProcesses() > 0 && stopFlag.isNotSet) _regenerate() else Future.unit
				}
			}
		}
		// Case: Capacity for multiple processes => Starts them sequentially and waits until all have been started
		else {
			println(s"Starts $capacity new Vast AI instances")
			Iterator.continually { startNewInstance() }.take(capacity).future.flatMap {
				// Case: All processes were started => Prepares to apply recursion as they are completed
				case TryCatch.Success(newProcesses, failures) =>
					failures.foreach { error =>
						log(error, "Unexpected partial failure while creating new instances")
					}
					println(s"Acquired ${ newProcesses.size } Vast AI instances")
					newProcesses
						// Cleans the process pool and tests for recursion after every completion
						.map { _.completionFuture.flatMap { _ =>
							if (cleanProcesses() > 0 && stopFlag.isNotSet) _regenerate() else Future.unit
						} }
						.future
						// Combines the recursion results
						.map {
							case TryCatch.Success(_, failures) =>
								failures.foreach { error =>
									log(error, "Unexpected partial failure during wide regenerate()")
								}
							case TryCatch.Failure(error) => throw error
						}
				// Case: Process starting failed unexpectedly => Fails
				case TryCatch.Failure(error) =>
					println(s"Failed to acquire Vast AI instances: ${ error.getMessage }")
					Future.failed(error)
			}
		}
	}
	
	/**
	 * Starts a new Vast AI instance, adding it to the processor pool.
	 * Also handles automatic shutdown & removal if the instance becomes unstable, etc.
	 * @return A future that resolves when the Vast AI instance is available (although not usable),
	 *         or if failed to acquire said instance.
	 */
	private def startNewInstance() = createInstanceAccess { _ =>
		println("Starting a new instance")
		// Creates and starts the process of setting up vLLM on Vast AI
		val process = VastAiVllmProcess(selectOffer, modelSize.modelSize, additionalReservedDisk, gateway,
			installScriptPath, localPort = portCounter.next(), remotePort = remotePort, maxGpuUtil = maxGpuUtil,
			setupTimeout = setupTimeout, recoveryTimeout = recoveryTimeout, noResponseTimeout = noResponseTimeout,
			instanceLabel = label) {
			offer =>
				val maxContextSize = contextSizeOn(offer.gpu.ram)
				val (image, initialVllmState, model) = chooseImage(offer, maxContextSize)
				(image, initialVllmState, maxContextSize, model)
		}
		processorsP :+= new Processor(process)
		println(s"Now at ${ processors.size } instance processes")
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
				println(s"Updating client count (${ e.newValue.phase })")
				usableProcessorsP.update()
			}
			
			// Case: Stop process initiated => Updates max context size & stops listening
			if (e.newValue.phase >= Stopping) {
				maxContextSizeP.update()
				println("Stops listening to the Vast AI process")
				Detach
			}
			else
				Continue
		}
		
		// Informs the "recorder" once this process finishes
		recorder.foreach { recorder => process.recordFuture.foreach(recorder) }
		
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
						println("Too many failures to acquire an instance => Stops the whole system")
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
		val removed = processes.filterNot(remaining.contains)
		if (removed.nonEmpty) {
			println(s"Removed ${ removed.size } completed Vast AI processes:")
			removed.foreach { p => println(s"\t- ${ p.instanceId.mkString }: ${ p.vastAiState }") }
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
			val funnel = MappingFunnel(safeMaxContextSize.value) {
				requestAndSize: (ChatParams, TokenCount) => requestAndSize._2.value } {
				case (request, tokens) =>
					if (process.detailedState.isUsable) {
						_lastRequestTime = Now
						push(request, tokens, queue, model.name, safeMaxContextSize)
					}
					else
						push(request, tokens)
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
		private def funnel: Option[(MappingFunnel[(ChatParams, TokenCount), RequestResult[BufferedOpenAiReply]], TokenCount)] =
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
		 * @param tokens Number of tokens / max context required
		 * @return Request result future. None if this processor can't handle that request at this time.
		 */
		def tryPush(request: ChatParams, tokens: TokenCount) =
			funnel
				// Makes sure the funnel can handle this request
				.filter { case (_, maxContextSize) =>
					!stopped && tokens <= maxContextSize && process.detailedState.isUsable }
				.map { _._1.push(request -> tokens) }
	}
}
