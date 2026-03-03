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
import utopia.flow.view.mutable.Settable
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.CopyOnDemand

import java.nio.file.Path
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

/**
 * Executes buffered chat requests using multiple Vast AI instances + vLLM servers.
 * @author Mikko Hilpinen
 * @since 03.03.2026, v1.5
 */
// TODO: Remember to mark instances as either usable or unusable
// (chooseImage: Offer => (NewInstanceFoundation, ServiceState, Int, String))
class VastAiVllmChatExecutor(selectOffer: SelectOffer, modelSize: ByteCount, additionalReservedDisk: ByteCount = 5.gb,
                             instanceCounts: Iterable[(Int, Int, Int, Int)] = Vector((4096, 3, 8, 8), (8192, 2, 4, 4), (16384, 1, 2, 2)),
                             defaultContextSize: Option[Int] = None,
                             backupExecutor: Option[BufferingChatRequestExecutor[BufferedOpenAiReply]] = None,
                             installScriptPath: Option[Path] = None,
                             maxGpuUtil: Double = 0.9, setupTimeout: Duration = 15.minutes,
                             recoveryTimeout: Duration = 60.seconds, noResponseTimeout: Duration = 10.minutes,
                             label: String = "chat-executor")
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
		processPool.iterator.map { _.completionFuture }.future
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
		
		// Immediately starts filling the instance pool
		// TODO: Should we start immediately or lazily?
		val completionFuture = regenerate()
		
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
