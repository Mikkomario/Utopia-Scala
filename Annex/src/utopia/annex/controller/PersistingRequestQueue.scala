package utopia.annex.controller

import utopia.annex.model.request.{Persisting, RequestQueueable}
import utopia.annex.model.response.RequestResult
import utopia.flow.async.context.ActionQueue
import utopia.flow.async.context.ActionQueue.QueuedAction
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.file.container.SaveTiming.OnJvmClose
import utopia.flow.parse.file.container.{FileContainer, ModelsFileContainer, SaveTiming}
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.eventful.Flag

import java.nio.file.Path
import scala.concurrent.ExecutionContext

object PersistingRequestQueue
{
	// OTHER	-------------------------
	
	/**
	  * Creates a new request queue that persists some requests
	  * @param master Parent queue system used
	  * @param fileLocation Path to the file where persisted requests should be stored
	  * @param handlers Handlers used when parsing persisted requests
	  * @param width How many requests can be sent at once (default = 1)
	  * @param saveLogic Timing logic used when backing persisted requests to local file system
	  *                  (default = save on jvm close)
	  * @param exc Implicit execution context
	  * @param jsonParser Parser used when reading responses and persisted data
	  * @return A new request queue + possible errors that occurred while parsing previously persisted requests
	  */
	def apply(master: QueueSystem, fileLocation: Path, handlers: Iterable[PersistedRequestHandler],
	          width: Int = 1, saveLogic: SaveTiming = OnJvmClose)
	         (implicit exc: ExecutionContext, jsonParser: JsonParser, logger: Logger): PersistingRequestQueue =
	{
		val queue = new SimpleQueue(fileLocation, master, width, saveLogic)
		val unprocessed = queue.processPersistedRequestsUsing(handlers)
		
		// Logs an error if some of the requests were not fully processed
		unprocessed.headOption.foreach { model =>
			logger(s"None of the specified ${ handlers.size } was able to process ${
				unprocessed.size } persisted request models. Example: $model")
		}
		
		queue
	}
	
	
	// NESTED	-------------------------
	
	private class SimpleQueue(fileLocation: Path, override val master: QueueSystem, width: Int = 1,
	                          saveLogic: SaveTiming = OnJvmClose)
	                         (implicit val exc: ExecutionContext, jsonParser: JsonParser, logger: Logger)
		extends PersistingRequestQueue
	{
		// ATTRIBUTES	----------------
		
		override protected val queue = new ActionQueue(width)
		override protected val requestContainer = new ModelsFileContainer(fileLocation, saveLogic)
	}
}

/**
  * This version of the request queue is able to persist the requests when necessary
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
trait PersistingRequestQueue extends SystemRequestQueue
{
	// ABSTRACT	-------------------------
	
	/**
	  * @return Execution context used for removing persisted requests once the results arrive
	  */
	protected implicit def exc: ExecutionContext
	
	/**
	  * @return A container used for holding the persisted requests
	  */
	protected def requestContainer: FileContainer[Seq[Model]]
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return An interface to this queue which skips the request persistence process
	  */
	def withoutPersisting: RequestQueue = SkipPersistenceQueue
	
	
	// IMPLEMENTED	---------------------
	
	override def push[A](request: RequestQueueable[A]) = {
		request match {
			// Case: Request seed => Always persists
			case Left(seed) => pushPersisting(seed) { super.push(request) }
			// Case: Prepared request => Only persists persisting requests
			case Right(request) =>
				request match {
					// Case: Persisting request
					case persistingRequest: Persisting =>
						pushPersisting(persistingRequest) { super.push(Right(persistingRequest)) }
					// Case: Non-persisting request
					case nonPersisting => super.push(Right(nonPersisting))
				}
		}
	}
	
	
	// OTHER	------------------------
	
	/**
	  * @return Saves the current request status locally
	  */
	def persistRequests() = requestContainer.saveStatus()
	
	/**
	  * Processes all previously persisted requests using the specified request handlers.
	  * This function should be called before this queue's 'push(...)' is called even once.
	  * @param handlers Request handlers which will process the previously persisted requests
	  * @return Requests for which there was not a handler in the specified set of handlers
	  */
	def processPersistedRequestsUsing(handlers: Iterable[PersistedRequestHandler]) = {
		// Clears the persisted models for processing
		val persistedModels = requestContainer.pointer.getAndSet(Empty)
		// If some requests were removed from the container, saves its status immediately
		if (persistedModels.nonEmpty)
			persistRequests()
		
		// Processes the persisted requests
		val unprocessed = persistedModels.filter { requestModel =>
			// Finds the handler which should handle this request
			handlers.find { _.shouldHandle(requestModel) } match {
				// Case: Handler found => Lets the handler deal with the request
				case Some(handler) =>
					handler.handle(requestModel, this)
					false
					
				// Case: No handler found => Records as an unprocessed request
				case None => true
			}
		}
		
		unprocessed
	}
	@deprecated("Please use .processPersistedRequestsUsing(Iterable[PersistedRequestHandler]) instead", "v1.8")
	def start(handlers: Iterable[PersistedRequestHandler]) = processPersistedRequestsUsing(handlers)
	
	private def pushPersisting[A](request: Persisting)(pushRequest: => QueuedAction[RequestResult[A]]) =
	{
		// Uses pointers to determine when the data should be persisted and in which form
		val sentFlag = Flag()
		val modelPointer = request.persistingModelPointer
		val storedModelPointer = modelPointer
			.lightMergeWithUntil(sentFlag) { (model, sent) =>
				if (sent) None else model
			} { (_, sent, _) => sent }
		
		// Pushes the request to the queue
		val resultFuture = pushRequest
		
		// Updates the sent flag once the request completes
		// TODO: Could also log possible errors, but no logging implementation is available here
		resultFuture.onComplete { _ => sentFlag.set() }
		
		// Stores the persisted model in the container while appropriate
		storedModelPointer.addListenerAndSimulateEvent(None) { e =>
			e.oldValue.foreach(removePersistedRequest)
			e.newValue.foreach { requestContainer.current :+= _ }
		}
		
		resultFuture
	}
	
	private def removePersistedRequest(request: Model) =
		requestContainer.pointer.update { _.filterNot { _ == request } }
		
	
	// NESTED   --------------------------
	
	private object SkipPersistenceQueue extends RequestQueue
	{
		override def push[A](request: RequestQueueable[A]): QueuedAction[RequestResult[A]] =
			PersistingRequestQueue.super.push(request)
	}
}
