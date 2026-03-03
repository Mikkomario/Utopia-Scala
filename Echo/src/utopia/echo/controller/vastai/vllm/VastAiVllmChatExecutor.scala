package utopia.echo.controller.vastai.vllm

import utopia.annex.controller.RequestQueue
import utopia.annex.util.RequestResultExtensions._
import utopia.disciple.controller.Gateway
import utopia.echo.controller.chat.BufferingChatRequestExecutor
import utopia.echo.controller.client.VastAiApiClient
import utopia.echo.controller.vastai.SelectOffer
import utopia.echo.model.enumeration.ModelParameter.ContextTokens
import utopia.echo.model.enumeration.ServiceState
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.request.ChatParams
import utopia.echo.model.request.openai.BufferedOpenAiChatCompletionRequest
import utopia.echo.model.response.openai.{BufferedOpenAiReply, OpenAiModelInfo}
import utopia.echo.model.unit.ByteCount
import utopia.echo.model.unit.ByteCountExtensions._
import utopia.echo.model.vastai.instance.NewInstanceFoundation
import utopia.echo.model.vastai.instance.offer.Offer
import utopia.echo.model.vastai.process.VastAiVllmProcessRecord
import utopia.echo.model.vastai.process.VastAiVllmProcessState.HostingApi
import utopia.echo.model.vastai.process.VastAiVllmProcessState.VastAiVllmProcessPhase.{NotStarted, Stopping}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.TryFuture
import utopia.flow.async.context.AccessQueue
import utopia.flow.async.process.{Breakable, Delay}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.event.model.ChangeResponsePriority.After
import utopia.flow.time.Duration
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.util.result.TryExtensions._
import utopia.flow.util.result.TryCatch
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.Settable
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.CopyOnDemand

import java.nio.file.Path
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

object VastAiVllmChatExecutor
{
	/**
	 * Creates a new chat executor that utilizes multiple parallel Vast AI instances when executing requests
	 * @param selectOffer Logic for selecting Vast AI offers to take
	 * @param modelSize Size of the used model.
	 *                  If multiple models are used, the size of the largest model should be returned.
	 * @param additionalReservedDisk Disk space reserved in addition to the model size. Default = 5 GB.
	 * @param instanceCounts Numbers of Vast AI instances to run. Each entry contains the following 4 values:
	 *                          1. Applied maximum context size
	 *                          1. Number of clients run in parallel
	 *                          1. Number of parallel requests per client
	 *                          1. Number of requests that may be queued per client,
	 *                             without bleeding to another context size category
	 *
	 *                       The default value is:
	 *                          1. 4 clients with context size of 4096 and 8 parallel requests
	 *                          1. 3 clients with context size of 8192 and 4 parallel requests
	 *                          1. 2 clients with context size of 16384 and 2 parallel requests
	 * @param defaultContextSize Context size to assume when no context size is specified in the request.
	 *                           Default = None = no context size is assumed,
	 *                           but the backup solution is used instead (if applicable)
	 *
	 *                           NB: It is always recommended to specify the context size for the requests.
	 *                               This can be done, for example, by utilizing a StatelessBufferedReplyGenerator.
	 * @param backupExecutor Request executor used for requests that exceed the largest allowed context size,
	 *                       and for those which don't have a context size specified (if 'defaultContextSize' is None).
	 *
	 *                       If left empty (default), such requests will be immediately failed.
	 * @param recorder An interface which receives records of completed Vast AI processes.
	 *                 None (default) if no recording should be performed.
	 * @param installScriptPath Path to a script for installing vLLM on the rented device.
	 *                          Used (and required), only if 'chooseImage' indicates that vLLM should be installed.
	 *                          Default = None.
	 * @param maxGpuUtil Maximum GPU utilization, as a fraction between 0 and 1. Used when/if starting vLLM. Default = 0.9.
	 * @param setupTimeout Timeout for the setup process.
	 *                     If the API doesn't become usable before this timeout, the instance is destroyed.
	 *                     Default = infinite (not recommended).
	 * @param recoveryTimeout Timeout for recovering from SSH and/or vLLM failures. Default = 60 seconds.
	 * @param noResponseTimeout Timeout for started API requests.
	 *                          If this timeout is reached, the request queue is closed
	 *                          and the underlying Vast AI instance is destroyed.
	 *                          Default = infinite (not recommended, unless you have your own monitoring process in place).
	 * @param label Custom label given to the rented Vast AI instance. Default = "chat-executor".
	 *              This label will be appended by the applied max context size.
	 * @param startLazily Whether this executor should only start when the first request is received.
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
	def apply(selectOffer: SelectOffer, modelSize: ByteCount, additionalReservedDisk: ByteCount = 5.gb,
	          instanceCounts: Iterable[(Int, Int, Int, Int)] = Vector((4096, 4, 8, 8), (8192, 3, 4, 4), (16384, 2, 2, 2)),
	          defaultContextSize: Option[Int] = None,
	          backupExecutor: Option[BufferingChatRequestExecutor[BufferedOpenAiReply]] = None,
	          recorder: Option[VastAiVllmProcessRecord => Unit], installScriptPath: Option[Path] = None,
	          maxGpuUtil: Double = 0.9, setupTimeout: Duration = 15.minutes,
	          recoveryTimeout: Duration = 60.seconds, noResponseTimeout: Duration = 10.minutes,
	          label: String = "chat-executor", startLazily: Boolean = false)
	         (chooseImage: (Offer, Int) => (NewInstanceFoundation, ServiceState, String))
	         (thinks: String => Boolean)
	         (implicit exc: ExecutionContext, vastAiClient: VastAiApiClient, log: Logger) =
		new VastAiVllmChatExecutor(selectOffer, modelSize, additionalReservedDisk, instanceCounts, defaultContextSize,
			backupExecutor, recorder, installScriptPath, maxGpuUtil, setupTimeout, recoveryTimeout, noResponseTimeout,
			label, startLazily)(chooseImage)(thinks)
}

/**
 * Executes buffered chat requests using multiple Vast AI instances + vLLM servers.
 * @param selectOffer Logic for selecting Vast AI offers to take
 * @param modelSize Size of the used model.
 *                  If multiple models are used, the size of the largest model should be returned.
 * @param additionalReservedDisk Disk space reserved in addition to the model size. Default = 5 GB.
 * @param instanceCounts Numbers of Vast AI instances to run. Each entry contains the following 4 values:
 *                          1. Applied maximum context size
 *                          1. Number of clients run in parallel
 *                          1. Number of parallel requests per client
 *                          1. Number of requests that may be queued per client,
 *                             without bleeding to another context size category
 *
 *                       The default value is:
 *                          1. 4 clients with context size of 4096 and 8 parallel requests
 *                          1. 3 clients with context size of 8192 and 4 parallel requests
 *                          1. 2 clients with context size of 16384 and 2 parallel requests
 * @param defaultContextSize Context size to assume when no context size is specified in the request.
 *                           Default = None = no context size is assumed,
 *                           but the backup solution is used instead (if applicable)
 *
 *                           NB: It is always recommended to specify the context size for the requests.
 *                               This can be done, for example, by utilizing a StatelessBufferedReplyGenerator.
 * @param backupExecutor Request executor used for requests that exceed the largest allowed context size,
 *                       and for those which don't have a context size specified (if 'defaultContextSize' is None).
 *
 *                       If left empty (default), such requests will be immediately failed.
 * @param recorder An interface which receives records of completed Vast AI processes.
 *                 None (default) if no recording should be performed.
 * @param installScriptPath Path to a script for installing vLLM on the rented device.
 *                          Used (and required), only if 'chooseImage' indicates that vLLM should be installed.
 *                          Default = None.
 * @param maxGpuUtil Maximum GPU utilization, as a fraction between 0 and 1. Used when/if starting vLLM. Default = 0.9.
 * @param setupTimeout Timeout for the setup process.
 *                     If the API doesn't become usable before this timeout, the instance is destroyed.
 *                     Default = infinite (not recommended).
 * @param recoveryTimeout Timeout for recovering from SSH and/or vLLM failures. Default = 60 seconds.
 * @param noResponseTimeout Timeout for started API requests.
 *                          If this timeout is reached, the request queue is closed
 *                          and the underlying Vast AI instance is destroyed.
 *                          Default = infinite (not recommended, unless you have your own monitoring process in place).
 * @param label Custom label given to the rented Vast AI instance. Default = "chat-executor".
 *              This label will be appended by the applied max context size.
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
class VastAiVllmChatExecutor(selectOffer: SelectOffer, modelSize: ByteCount, additionalReservedDisk: ByteCount = 5.gb,
                             instanceCounts: Iterable[(Int, Int, Int, Int)] = Vector((4096, 4, 8, 8), (8192, 3, 4, 4), (16384, 2, 2, 2)),
                             defaultContextSize: Option[Int] = None,
                             backupExecutor: Option[BufferingChatRequestExecutor[BufferedOpenAiReply]] = None,
                             recorder: Option[VastAiVllmProcessRecord => Unit], installScriptPath: Option[Path] = None,
                             maxGpuUtil: Double = 0.9, setupTimeout: Duration = 15.minutes,
                             recoveryTimeout: Duration = 60.seconds, noResponseTimeout: Duration = 10.minutes,
                             label: String = "chat-executor", startsLazily: Boolean = false)
                            (chooseImage: (Offer, Int) => (NewInstanceFoundation, ServiceState, String))
                            (thinks: String => Boolean)
                            (implicit exc: ExecutionContext, vastAiClient: VastAiApiClient, log: Logger)
	extends BufferingChatRequestExecutor[BufferedOpenAiReply] with Breakable
{
	// ATTRIBUTES   -----------------------
	
	/**
	 * Maximum request (context) size that is possible to handle without using backup executors
	 */
	val maxContextSize = instanceCounts.iterator.map { _._1 }.maxOption.getOrElse(-1)
	
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
	private val portCounter = Volatile(18001)
	
	/**
	 * Used for limiting instance-creation to one instance at a time
	 */
	private val createInstanceAccess = new AccessQueue(())
	/**
	 * Pools that actually handle the requests
	 */
	private val processPool = instanceCounts.toOptimizedSeq.sortBy { _._1 }
		.map { case (maxContextSize, maxInstances, maxRequestsPerInstance, queuedLayers) =>
			new ProcessPool(maxContextSize, maxInstances, maxRequestsPerInstance, queuedLayers)
		}
	
	
	// INITIAL CODE -----------------------
	
	registerToStopOnceJVMCloses()
	
	
	// IMPLEMENTED  -----------------------
	
	override def apply(params: ChatParams): Future[Try[BufferedOpenAiReply]] = {
		// Checks the required context size
		params(ContextTokens).int.orElse(defaultContextSize) match {
			case Some(tokens) =>
				// Case: Too large a request => Uses the backup executor, if available
				if (tokens > maxContextSize)
					delegateToBackup(params, new IllegalArgumentException(
						s"Maximum context size of $maxContextSize is exceeded by $tokens"))
				// Case: Suitable context size => Delegates processing to one of the processor pools
				else
					_apply(params, tokens)
			
			// Case: No context size known (never recommended) => Uses the backup executor, if available
			case None =>
				delegateToBackup(params, new IllegalArgumentException("Request context size was not specified"))
		}
	}
	
	override def stop(): Future[Any] = {
		if (stopFlag.set())
			processPool.foreach { _.stopAll() }
		processPool.iterator.flatMap { _.lazyCompletionFuture.current }.future
	}
	
	
	// OTHER    ---------------------------
	
	private def _apply(params: ChatParams, tokens: Int): Future[Try[BufferedOpenAiReply]] = {
		// Acquires the pools that can theoretically handle this request
		val options = processPool.dropWhile { _.maxContextSize < tokens }
		// Gives the request to one of the process pools, depending on pool capacity
		// Prefers smaller token limit pools
		options
			// Option 1: Finds a pool with immediate capacity
			.findMap { pool =>
				if (pool.hasImmediateCapacity)
					pool.tryPush(params)
				else
					None
			}
			// Option 2: Finds a pool that is not overfilled
			.orElse { options.findMap { _.tryPush(params) } }
			// Option 3: Finds a pool that has at least one active client
			.orElse { options.findMap { _.tryPush(params, force = true) } }
			// Option 4: Queues the request to the first pool, until a client is acquired
			.getOrElse { options.head.queue(params) }
			.toTryFuture
	}
	
	private def delegateToBackup(request: ChatParams, failure: => Throwable) =
		backupExecutor match {
			case Some(backup) => backup(request)
			case None => TryFuture.failure(failure)
		}
	
	
	// NESTED   ---------------------------
	
	private class ProcessPool(val maxContextSize: Int, maxInstances: Int, maxParallelRequestsPerInstance: Int,
	                          queuedLayers: Int)
	{
		// ATTRIBUTES   -------------------
		
		private val _queue = Volatile.eventful.emptySeq[Promise[(RequestQueue, String)]]
		private val hasQueueFlag = _queue.nonEmptyFlag
		
		private val processesP = Volatile.emptySeq[VastAiVllmProcess]
		
		// Starts filling the instance pool either lazily or immediately
		val lazyCompletionFuture = if (startsLazily) Lazy { regenerate() } else Lazy.initialized(regenerate())
		
		private val usableClientsP = CopyOnDemand { processesP.value.flatMap { _.usableClient } }
		
		private val clearQueueFutureP = Volatile(Future.successful(true))
		private val clearQueueListener: ChangeListener[Seq[(RequestQueue, OpenAiModelInfo, Int)]] =
			ChangeListener[Seq[(RequestQueue, OpenAiModelInfo, Int)]] { e =>
				// Case: Clients are now available => Prepares to clear the queue
				if (e.newValue.nonEmpty)
					clearQueueFutureP.mutate { f =>
						if (f.isCompleted) {
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
			
		
		// INITIAL CODE -------------------
		
		// Whenever the queue starts filling, schedules clearance
		hasQueueFlag.addListener { e =>
			if (e.newValue)
				scheduleQueueClearance()
			Continue
		}
		
		
		// COMPUTED -----------------------
		
		def hasImmediateCapacity = clients.exists { _._1.pendingRequestCount < maxParallelRequestsPerInstance }
		
		private def clients = usableClientsP.value
		
		
		// OTHER    -----------------------
		
		def tryPush(request: ChatParams, force: Boolean = false) = {
			// Starts this processor, if not started already
			if (startsLazily && lazyCompletionFuture.nonInitialized)
				lazyCompletionFuture.value
			
			// Finds a client that has capacity (or the one with most capacity, if 'force' is enabled)
			clients.iterator.map { case (client, model, _) => (client, model, client.pendingRequestCount) }
				.minByOption { _._3 }.filter { force || _._3 < queuedLayers }
				.map { case (client, model, _) => push(request, client, model.name) }
		}
		def queue(request: ChatParams) = {
			val clientPromise = Promise[(RequestQueue, String)]()
			_queue :+= clientPromise
			clientPromise.future.flatMap { case (client, model) => push(request, client, model) }
		}
		
		/**
		 * Requests all currently running processes to stop
		 */
		def stopAll() = processesP.value.foreach { _.stop() }
		
		private def scheduleQueueClearance(): Unit =
			usableClientsP.addListenerAndSimulateEvent(Empty)(clearQueueListener)
		
		private def clearQueue(): Future[Boolean] = {
			// Checks the usable clients
			val clients = usableClientsP.value
			// Case: No clients are usable => Completes
			if (clients.isEmpty)
				Future.successful(_queue.isEmpty)
			else {
				// Collects the tasks to process
				val tasks = _queue.pop(clients.size)
				// Case: No tasks to process => Completes
				if (tasks.isEmpty)
					Future.successful(true)
				else {
					// Assigns the tasks to the available clients
					val pairedTasks = {
						if (tasks.size == clients.size)
							tasks.zip(clients)
						else
							tasks.zip(clients.sortBy { _._1.pendingRequestCount })
					}
					pairedTasks.foreach { case (promise, (queue, model, _)) => promise.success(queue -> model.name) }
					
					// After a short delay, continues to unqueue more tasks
					Delay.future(5.seconds) { clearQueue() }
				}
			}
		}
		
		private def push(request: ChatParams, client: RequestQueue, model: String) =
			client.push(BufferedOpenAiChatCompletionRequest(request.toLlm(llmCache(model)))).future
		
		/**
		 * Fills the process pool with new processes, until 'maxInstances' is reached.
		 * Continues recursively, keeping the pool filled.
		 * @return A future that resolves once all processes have completed
		 *         (usually some time after stop() has been called)
		 */
		private def regenerate(): Future[Unit] = {
			// Checks how much capacity there is for new processes
			val capacity = cleanProcesses()
			// Case: No capacity => Returns immediately
			if (capacity <= 0)
				Future.unit
			// Case: Capacity for a single process
			//       => Starts it and prepares to apply recursion when that process completes
			else if (capacity == 1) {
				// Starts a new process, when possible
				val processFuture = startNewInstance()
				processFuture.flatMap { process =>
					// Once the process completes, cleans the process pool and attempts to generate more processes
					process.completionFuture.flatMap { _ =>
						if (cleanProcesses() > 0 && stopFlag.isNotSet) regenerate() else Future.unit
					}
				}
			}
			// Case: Capacity for multiple processes => Starts them sequentially and waits until all have been started
			else
				Iterator.continually { startNewInstance() }.take(capacity).future.flatMap {
					// Case: All processes were started => Prepares to apply recursion as they are completed
					case TryCatch.Success(newProcesses, failures) =>
						failures.foreach { error =>
							log(error, "Unexpected partial failure while creating new instances")
						}
						newProcesses
							// Cleans the process pool and tests for recursion after every completion
							.map { _.completionFuture.flatMap { _ =>
								if (cleanProcesses() > 0 && stopFlag.isNotSet) regenerate() else Future.unit
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
					case TryCatch.Failure(error) => Future.failed(error)
				}
		}
		
		private def startNewInstance() = createInstanceAccess { _ =>
			// Always uses a different port number
			val port = portCounter.getAndUpdate { _ + 1 }
			// Creates and starts the process of setting up vLLM on Vast AI
			val process = VastAiVllmProcess(selectOffer, modelSize, additionalReservedDisk,
				Gateway(maxConnectionsPerRoute = maxParallelRequestsPerInstance,
					maxConnectionsTotal = maxParallelRequestsPerInstance),
				installScriptPath, port, maxGpuUtil = maxGpuUtil, setupTimeout = setupTimeout,
				recoveryTimeout = recoveryTimeout, noResponseTimeout = noResponseTimeout,
				instanceLabel = s"$label-$maxContextSize") {
				offer =>
					val (image, initialVllmState, model) = chooseImage(offer, maxContextSize)
					(image, initialVllmState, maxContextSize, model)
			}
			processesP :+ process
			process.runAsync()
			
			// Updates the usableClients when API-hosting starts or ends
			process.detailedStatePointer.addListenerAndSimulateEvent(NotStarted) { e =>
				val clientStates = e.values.map {
					case HostingApi(instance, client, model, _) if instance.status.instanceShouldBeUsed =>
						Some(client -> model)
					case _ => None
				}
				if (clientStates.isAsymmetric)
					usableClientsP.update()
				
				if (e.newValue.phase >= Stopping)
					Detach
				else
					Continue
			}
			
			// Informs the "recorder" once this process finishes
			recorder.foreach { recorder => process.recordFuture.foreach(recorder) }
			
			// The next instance may be acquired once the process is in loading state / instance has been acquired
			process.detailedStatePointer.findMapFuture { state =>
				if (state.isInstanceAvailable || state.phase >= Stopping) Some(process) else None
			}
		}
		
		/**
		 * Removes completed processes from the pool
		 * @return Currently remaining capacity (i.e. how many more processes may be started)
		 */
		private def cleanProcesses() = processesP.mutate { processes =>
			val remaining = processes.filterNot { _.state.isFinal }
			(maxInstances - remaining.size) -> remaining
		}
	}
}
