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
import utopia.echo.model.unit.ByteCount
import utopia.echo.model.unit.ByteCountExtensions._
import utopia.echo.model.vastai.instance.NewInstanceFoundation
import utopia.echo.model.vastai.instance.offer.Offer
import utopia.echo.model.vastai.process.VastAiVllmProcessRecord
import utopia.echo.model.vastai.process.VastAiVllmProcessState.HostingApi
import utopia.echo.model.vastai.process.VastAiVllmProcessState.VastAiVllmProcessPhase.{ApiHosting, NotStarted, Stopping}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.TryFuture
import utopia.flow.async.context.{AccessQueue, MappingFunnel}
import utopia.flow.async.process.{Breakable, Delay, LoopingProcess}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Single}
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
	 *                    Accepts the selected offer, yields:
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
	          instanceActivationPendingTokensThreshold: Int = 24000,
	          instanceAccelerationPendingTokensThreshold: Int = 48000, maxConnectionsPerInstance: Int = 28,
	          additionalReservedDisk: ByteCount = 5.gb, defaultContextSize: Option[Int] = None,
	          contextSafetyMargin: Int = 64,
	          backupExecutor: Option[BufferingChatRequestExecutor[BufferedOpenAiReply]] = None,
	          recorder: Option[VastAiVllmProcessRecord => Unit], installScriptPath: Option[Path] = None,
	          remotePort: Int = 8000, maxGpuUtil: Double = 0.9, setupTimeout: Duration = 15.minutes,
	          recoveryTimeout: Duration = 60.seconds, noResponseTimeout: Duration = 10.minutes,
	          idleShutdownThreshold: Duration = 15.minutes, partialUseShutdownThreshold: Duration = 25.minutes,
	          label: String = "chat-executor", startsLazily: Boolean = false)
	         (chooseImage: Offer => (NewInstanceFoundation, ServiceState, String))
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
 *                    Accepts the selected offer, yields:
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
                             instanceActivationPendingTokensThreshold: Int = 24000,
                             instanceAccelerationPendingTokensThreshold: Int = 48000,
                             maxConnectionsPerInstance: Int = 28, additionalReservedDisk: ByteCount = 5.gb,
                             defaultContextSize: Option[Int] = None, contextSafetyMargin: Int = 64,
                             backupExecutor: Option[BufferingChatRequestExecutor[BufferedOpenAiReply]] = None,
                             recorder: Option[VastAiVllmProcessRecord => Unit], installScriptPath: Option[Path] = None,
                             remotePort: Int = 8000, maxGpuUtil: Double = 0.9, setupTimeout: Duration = 15.minutes,
                             recoveryTimeout: Duration = 60.seconds, noResponseTimeout: Duration = 10.minutes,
                             idleShutdownThreshold: Duration = 15.minutes,
                             partialUseShutdownThreshold: Duration = 25.minutes, label: String = "chat-executor",
                             startsLazily: Boolean = false)
                            (chooseImage: Offer => (NewInstanceFoundation, ServiceState, String))
                            (thinks: String => Boolean)
                            (implicit exc: ExecutionContext, vastAiClient: VastAiApiClient, log: Logger)
	extends BufferingChatRequestExecutor[BufferedOpenAiReply] with Breakable
{
	// ATTRIBUTES   -----------------------
	
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
	
	private val consecutiveGetInstanceFailuresP = Volatile(0)
	
	private val _queue = Volatile.eventful.emptySeq[Promise[Processor]]
	private val hasQueueFlag = _queue.nonEmptyFlag
	
	private val targetInstanceCountP = Volatile.eventful(if (startsLazily) 0 else coreInstanceCount)
	private val pendingTokensCheckThresholdP = Volatile(0)
	private val queueSizeCheckThresholdP = Volatile(0)
	
	private val gateway = Gateway(maxConnectionsPerRoute = maxConnectionsPerInstance,
		maxConnectionsTotal = maxConnectionsPerInstance * maxInstanceCount, disableTrustStoreVerification = true)
	
	/**
	 * Used for limiting instance-creation to one instance at a time
	 */
	private val createInstanceAccess = new AccessQueue(())
	
	private val processorsP = Volatile.eventful.emptySeq[Processor]
	private val usableProcessorsP = CopyOnDemand { processors.filter { _.usable } }
	
	private val growingFlag = CopyOnDemand { processors.exists { _.phase < ApiHosting } }
	
	private val maxContextSizeP = CopyOnDemand {
		processors.iterator.filter { _.phase < Stopping }.flatMap { _.maxContextSize }.maxOption
			.getOrElse(defaultMaxContextSize)
	}
	
	private val regenerateFuturesP = Volatile.emptySeq[Future[Unit]]
	
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
				// Case: No idle processes & above core processor count => Checks whether some are not fully utilized
				else if (remaining.size > coreInstanceCount) {
					val partialUseThreshold = now - partialUseShutdownThreshold
					remaining.find { _.lastPendingTime < partialUseThreshold }.foreach { partiallyUsedProcessor =>
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
				Some(remaining.iterator.map { _.lastRequestTime }.minOption match {
					case Some(earliestRequestTime) => earliestRequestTime + idleShutdownThreshold
					case None => now + idleShutdownThreshold
				})
			})
		// Case: Idle shutdown is disabled
		else
			None
	}
	
	private val clearQueueFutureP = Volatile(Future.successful(true))
	private val clearQueueListener: ChangeListener[Seq[Processor]] = ChangeListener[Seq[Processor]] { e =>
		// Case: Processors are now available => Prepares to clear the queue
		if (e.newValue.exists { _.usable })
			clearQueueFutureP.mutate { f =>
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
	
	// Updates the growing flag when applicable
	processorsP.addContinuousAnyChangeListener { growingFlag.update() }
	usableProcessorsP.addContinuousAnyChangeListener { growingFlag.update() }
	
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
	
	/*
	/**
	 * @return The current status of this pool
	 */
	def status = VastAiVllmProcessorPoolStatus(maxContextSize, maxParallelRequestsPerInstance,
		processes.map { process => (process.detailedState.phase, process.instanceStatus,
			process.usableClient match {
				case Some((client, _, _)) => client.pendingRequestCount
				case None => 0
			},
			process.startTime)
		},
		_queue.size)
	*/
	
	/**
	 * Maximum request (context) size that is possible to handle without using backup executors
	 */
	def maxContextSize = safeMaxContextSize + contextSafetyMargin
	
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
	
	private def push(request: ChatParams, tokens: Int): Future[RequestResult[BufferedOpenAiReply]] = {
		if (stopFlag.isSet)
			Future.successful(RequestSendingFailed(
				new IllegalStateException("This request-execution interface was closed")))
		else {
			val processors = this.processors
			
			// Attempts to give the request to one of the processors
			processors.sortBy { _.pendingToActiveRatio }
				.findMap { processor =>
					processor.tryPush(request, tokens).map { resultFuture =>
						// Activates more instances, if appropriate
						adjustInstanceTargetIfAppropriate(pendingTokensCheckThresholdP, tokens,
							instanceActivationPendingTokensThreshold, 5000,
							processors.iterator.map { _.pendingTokens }.sum.toInt) {
							currentTarget =>
								if (processors.size < currentTarget || processors.exists { _.phase < ApiHosting })
									currentTarget * instanceAccelerationPendingTokensThreshold
								else
									currentTarget * instanceActivationPendingTokensThreshold
						}
						
						// Checks whether it's possible to handle cases where the context window couldn't
						// fit into the server's maximum context (because of token-counting issues)
						processor.usableContextSize.flatMap { currentContextSize =>
							val nextContextSize = (currentContextSize * 1.2).ceil.toInt min safeMaxContextSize
							if (processors.exists { _.usableContextSize.exists { _ >= nextContextSize } })
								Some(nextContextSize)
							else
								None
						} match {
							case Some(nextContextSize) =>
								resultFuture.flatMap {
									case Response.Failure(status, message, _) if status == BadRequest =>
										log(s"Warning: $message => Delegated the request to a processor with a larger context window")
										push(request, nextContextSize)
									
									case result => Future.successful(result)
								}
							case None => resultFuture
						}
					}
				}
				.getOrElse {
					// Case: No processors are available => Queues this request
					val clientPromise = Promise[Processor]()
					val queueSize = _queue.updateAndGet { _ :+ clientPromise }.size
					
					// Activates more instances, if appropriate
					adjustInstanceTargetIfAppropriate(queueSizeCheckThresholdP, 1,
						instanceActivationQueueSizeThreshold, 5, queueSize) { _ * instanceActivationQueueSizeThreshold }
					
					clientPromise.future.flatMap { processor =>
						// Case: A processor became available => Gives it this request
						processor.tryPush(request, tokens).getOrElse {
							// Case: The processor couldn't receive this request (unexpected)
							//       => Attempts again with another processor
							push(request, tokens)
						}
					}
				}
		}
	}
	private def push(request: ChatParams, tokens: Int, client: RequestQueue, model: String,
	                 maxContextSize: Int): Future[RequestResult[BufferedOpenAiReply]] =
	{
		// Converts the request into a full chat request
		val apiRequest = BufferedOpenAiChatCompletionRequest(
			request.toLlm(llmCache(model)).mapSetting(ContextTokens) { _.int match {
				case Some(maxTokens) => maxTokens min maxContextSize
				case None => maxContextSize
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
	
	private def scheduleQueueClearance(): Unit =
		usableProcessorsP.addListenerAndSimulateEvent(Empty)(clearQueueListener)
	
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
	
	private def regenerate() = regenerateFuturesP.update { previous =>
		val result = _regenerate()
		result.forFailure { log(_, "Instance regeneration failed") }
		OptimizedIndexedSeq.concat(previous.view.filterNot { _.isCompleted }, Single(result))
	}
	/**
	 * Fills the process pool with new processes, until 'maxInstances' is reached.
	 * Continues recursively, keeping the pool filled.
	 * @return A future that resolves once all processes have completed
	 *         (usually some time after stop() has been called)
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
	
	private def startNewInstance() = createInstanceAccess { _ =>
		println("Starting a new instance")
		// Creates and starts the process of setting up vLLM on Vast AI
		val process = VastAiVllmProcess(selectOffer, modelSize.modelSize, additionalReservedDisk, gateway,
			installScriptPath, localPort = portCounter.next(), remotePort = remotePort, maxGpuUtil = maxGpuUtil,
			setupTimeout = setupTimeout, recoveryTimeout = recoveryTimeout, noResponseTimeout = noResponseTimeout,
			instanceLabel = label) {
			offer =>
				val (image, initialVllmState, model) = chooseImage(offer)
				(image, initialVllmState, contextSizeOn(offer.gpu.ram), model)
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
	
	private def contextSizeOn(vram: ByteCount) = modelSize.maxContextSizeOn(vram) - contextSafetyMargin
	
	
	// NESTED   ---------------------------
	
	private class Processor(process: VastAiVllmProcess) extends Breakable with MaybeEmpty[Processor]
	{
		// ATTRIBUTES   ------------------
		
		private var stopped = false
		private var _lastRequestTime = Now.toInstant
		private var _lastPendingTime = _lastRequestTime
		
		private val funnelP = process.clientPointer.map { _.map { _.map { case (queue, model, maxContextSize) =>
			// Updates the last request -time now that the API is available.
			// Also, makes sure the idle-clearing process is running.
			_lastRequestTime = Now
			_lastPendingTime = _lastRequestTime
			idleShutdownProcess.foreach { _.runAsync() }
			
			// Prepares a funnel for the incoming requests
			val safeMaxContextSize = maxContextSize - contextSafetyMargin
			val funnel = MappingFunnel(safeMaxContextSize) { requestAndSize: (ChatParams, Int) =>
				requestAndSize._2
			} { case (request, tokens) =>
				if (process.detailedState.isUsable) {
					_lastRequestTime = Now
					push(request, tokens, queue, model.name, safeMaxContextSize)
				}
				else
					push(request, tokens)
			}
			
			// Takes notice when the funnel becomes full
			funnel.pendingFlag.addListener { e =>
				if (e.newValue)
					_lastPendingTime = Now
					
				if (stopped) Detach else Continue
			}
			
			funnel -> safeMaxContextSize
		} } }
		
		
		// COMPUTED ---------------------
		
		def instanceId = process.instanceId
		def phase = process.detailedState.phase
		def vastAiState = process.vastAiState
		
		def usable = !stopped && process.detailedState.isUsable && funnelP.value.exists { _.isSuccess }
		def terminated = process.state.isFinal
		
		def maxContextSize = process.maxContextSize
		def usableContextSize: Option[Int] = {
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
			case Some((funnel, maxContextSize)) => funnel.queuedCapacity / maxContextSize
			case None => 1000000
		}
		
		def wasRequestedToStop = stopped
		def lastRequestTime = _lastRequestTime
		def lastPendingTime = _lastPendingTime
		
		private def funnel = funnelP.value.flatMap { _.toOption }
		
		
		// IMPLEMENTED  ------------------
		
		override def self: Processor = this
		
		override def isEmpty: Boolean = funnel.forall { _._1.isEmpty }
		
		override def stop(): Future[Any] = {
			stopped = true
			val state = process.detailedState
			if (state.phase != ApiHosting)
				process.stop()
			else
				funnel match {
					case Some((funnel, _)) => funnel.emptyFuture.flatMap { _ => process.stop() }
					case None => process.stop()
				}
		}
		
		
		// OTHER    ----------------------
		
		def tryPush(request: ChatParams, tokens: Int) =
			funnel
				// Makes sure the funnel can handle this request
				.filter { case (_, maxContextSize) =>
					!stopped && tokens <= maxContextSize && process.detailedState.isUsable }
				.map { _._1.push(request -> tokens) }
	}
}
