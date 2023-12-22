package utopia.annex.controller

import utopia.annex.model.request.{Persisting, RequestQueueable}
import utopia.annex.model.response.RequestResult
import utopia.flow.async.context.ActionQueue
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.file.container.SaveTiming.OnJvmClose
import utopia.flow.parse.file.container.{FileContainer, ModelsFileContainer, SaveTiming}
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.eventful.Flag

import java.nio.file.Path
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

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
			 (implicit exc: ExecutionContext, jsonParser: JsonParser, logger: Logger): (PersistingRequestQueue, Vector[Throwable]) =
	{
		val queue = new SimpleQueue(fileLocation, master, width, saveLogic)
		val errors = queue.start(handlers)
		queue -> errors
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
trait PersistingRequestQueue extends RequestQueue
{
	// ABSTRACT	-------------------------
	
	/**
	  * @return Execution context used for removing persisted requests once the results arrive
	  */
	protected implicit def exc: ExecutionContext
	
	/**
	  * @return A container used for holding the persisted requests
	  */
	protected def requestContainer: FileContainer[Vector[Model]]
	
	
	// IMPLEMENTED	---------------------
	
	override def push(request: RequestQueueable) = {
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
	  * @param handlers Request handlers which are used for parsing persisted requests and handling their responses
	  * @return A list of errors that occurred while parsing the requests
	  */
	protected def start(handlers: Iterable[PersistedRequestHandler]) = {
		val persistedModels = requestContainer.current
		val errors = persistedModels.flatMap { requestModel =>
			handlers.view.filter { _.shouldHandle(requestModel) }.tryFindMap { h => h.factory(requestModel)
				.map { r => h -> r } } match {
					case Success((handler, request)) =>
						super.push(request).onComplete { result =>
							// Once result is received, removes the persisted request and lets the handler
							// handle the response
							removePersistedRequest(requestModel)
							result.foreach { handler.handle(requestModel, request, _) }
						}
						None
					case Failure(error) => Some(error)
				}
		}
		// If some requests were removed from the container, saves its status immediately
		if (persistedModels.nonEmpty)
			persistRequests()
		errors
	}
	
	private def pushPersisting(request: Persisting)(pushRequest: => Future[RequestResult]) = {
		// Uses pointers to determine when the data should be persisted and in which form
		val sentFlag = Flag()
		val modelPointer = request.persistingModelPointer
		val storedModelPointer = modelPointer
			.lightMergeWithUntil(sentFlag) { (model, sent) =>
				if (sent) None else model
			} { (_, sent, _) => sent }
		
		// Pushes the request to the queue
		val resultFuture = pushRequest
		
		// Stores the persisted model in the container while appropriate
		storedModelPointer.addListenerAndSimulateEvent(None) { e =>
			e.oldValue.foreach(removePersistedRequest)
			e.newValue.foreach { requestContainer.current :+= _ }
		}
		
		resultFuture
	}
	
	private def removePersistedRequest(request: Model) =
		requestContainer.pointer.update { _.filterNot { _ == request } }
}
