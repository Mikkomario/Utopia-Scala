package utopia.annex.controller

import utopia.annex.model.request.ApiRequest
import utopia.flow.container.FileContainer
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.util.CollectionExtensions._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

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
	protected def requestContainer: FileContainer[Vector[Model[Constant]]]
	
	
	// IMPLEMENTED	---------------------
	
	override def push(request: ApiRequest) =
	{
		// Saves the requests when they are send, if necessary
		request.persistingModel match
		{
			case Some(model) =>
				requestContainer.current :+= model
				val result = super.push(request)
				// Removes the model once result has been received
				result.onComplete { _ => removePersistedRequest(model) }
				result
			case None => super.push(request)
		}
	}
	
	
	// OTHER	------------------------
	
	/**
	  * @param handlers Request handlers which are used for parsing persisted requests and handling their responses
	  * @return A list of errors that occurred while parsing the requests
	  */
	protected def start(handlers: Iterable[PersistedRequestHandler]) =
	{
		requestContainer.current.flatMap { requestModel =>
			handlers.view.filter { _.shouldHandle(requestModel) }.tryFindMap { h => h.factory(requestModel)
				.map { r => h -> r } } match
				{
					case Success((handler, request)) =>
						super.push(request).onComplete { result =>
							// Once result is received, removes the persisted request and lets the handler
							// handle the response
							removePersistedRequest(requestModel)
							result.foreach(handler.handle)
						}
						None
					case Failure(error) => Some(error)
				}
		}
	}
	
	private def removePersistedRequest(request: Model[Constant]) =
		requestContainer.pointer.update { _.filterNot { _ == request } }
}
