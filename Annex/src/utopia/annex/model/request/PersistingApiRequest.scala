package utopia.annex.model.request

import utopia.access.http.Method
import utopia.annex.controller.ApiClient.PreparedRequest
import utopia.annex.controller.{PersistedRequestHandler, PersistingRequestQueue}
import utopia.annex.model.request.ApiRequest.Send
import utopia.annex.model.response.RequestNotSent.RequestSendingFailed
import utopia.annex.model.response.RequestResult
import utopia.disciple.http.request.Body
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Constant, Model, ModelDeclaration, Value}
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.{ModelLike, Property}
import utopia.flow.view.immutable.eventful.AlwaysFalse
import utopia.flow.view.template.eventful.{Changing, Flag}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object PersistingApiRequest
{
	// ATTRIBUTES   -------------------------
	
	private lazy val schema = ModelDeclaration("method" -> StringType)
	
	
	// OTHER    -----------------------------
	
	/**
	  * Creates an interface for generating persisting API requests of a specific type +
	  * another interface for handling them after they have been read from their persisted state at the beginning
	  * of a session (i.e. to be used with [[PersistingRequestQueue]]).
	  * @param identifier Identifier assigned to all persisted models
	  * @param method Method used within these requests
	  * @param identifierPropName Name of the persisted property which is used for storing the identifier value.
	  *                           Default = "type".
	  * @param send A function which finalizes the request send process, determining response parsing, etc.
	  * @param handleResultOfPersisted A function which
	  *                                handles the result of a request that had been previously persisted.
	  *                                Should at least log errors, etc.
	  * @param exc Implicit execution context
	  * @tparam A Type of parsed response values
	  * @return Returns 2 interfaces:
	  *             1. An interface for creating new requests
	  *             1. An interface for handling previously persisted requests.
	  *                This should be added to a [[PersistingRequestQueue]].
	  */
	def apply[A](identifier: String, method: Method, identifierPropName: String = "type")
	            (send: Send[A])
	            (handleResultOfPersisted: RequestResult[A] => Unit)
	            (implicit exc: ExecutionContext) =
	{
		val identifierProp = Constant(identifierPropName, identifier)
		val factory = new PersistingApiRequestFactory[A](identifierProp, method)(send)
		val handler: PersistedRequestHandler =
			new PersistingApiRequestHandler[A](identifierProp)(send)(handleResultOfPersisted)
		
		factory -> handler
	}
	
	
	// NESTED   -----------------------------
	
	/**
	  * An interface for quickly constructing persisting API requests with similar logic
	  * @tparam A Type of parsed response values
	  */
	class PersistingApiRequestFactory[+A] private[PersistingApiRequest](persistingIdentifier: Constant,
	                                                                    method: Method)
	                                                                   (sendFunction: Send[A])
	{
		/**
		  * @param path Path to the targeted server-side resource
		  * @param body Assigned request body (default = empty)
		  * @param deprecationFlag A flag which contains true if this request gets deprecated
		  *                        and should be retracted (unless already sent).
		  *                        Default = always false.
		  * @return A new API request targeting the specified path and posting the specified body
		  */
		def apply(path: String, body: Value = Value.empty,
		          deprecationFlag: Flag = AlwaysFalse): PersistingApiRequest[A] =
			new _PersistingApiRequest[A](persistingIdentifier, method, path, body, deprecationFlag)(sendFunction)
	}
	
	private class PersistingApiRequestHandler[A](persistingIdentifier: Constant)
	                                            (sendFunction: Send[A])
	                                            (handleResult: RequestResult[A] => Unit)
	                                            (implicit exc: ExecutionContext)
		extends PersistedRequestHandler
	{
		// ATTRIBUTES   ----------------------------
		
		private lazy val parser = new PersistingApiRequestFromModel[A](sendFunction)
		
		
		// IMPLEMENTED  ----------------------------
		
		override def shouldHandle(requestModel: Model): Boolean =
			requestModel(persistingIdentifier.name) ~== persistingIdentifier.value
		
		override def handle(requestModel: Model, queue: PersistingRequestQueue): Unit = {
			parser(requestModel) match {
				case Success(request) => queue.push(request).foreach(handleResult)
				case Failure(error) => handleResult(RequestSendingFailed(error))
			}
		}
	}
	
	private class PersistingApiRequestFromModel[A](sendFunction: Send[A]) extends FromModelFactory[ApiRequest[A]]
	{
		override def apply(model: ModelLike[Property]): Try[ApiRequest[A]] = schema.validate(model).flatMap { model =>
			Method.parse(model("method").getString)
				.toTry {
					new IllegalArgumentException(s"Unsupported method ${ model("method") } in persisted request model")
				}
				// NB: These parsed models won't be persisted anymore
				.map { method =>
					ApiRequest(method, model("path").getString, model("body"))(sendFunction)
				}
		}
	}
	
	private class _PersistingApiRequest[+A](persistingIdentifier: Constant, override val method: Method,
	                                        override val path: String, bodyValue: Value,
	                                        deprecationFlag: Flag)
	                                       (sendFunction: Send[A])
		extends PersistingApiRequest[A]
	{
		// ATTRIBUTES   ----------------------
		
		private lazy val persistingModel: Model =
			persistingIdentifier +: Model.from("method" -> method.toString, "path" -> path, "body" -> bodyValue)
		
		override lazy val persistingModelPointer: Changing[Option[Model]] =
			deprecationFlag.map { deprecated => if (deprecated) None else Some(persistingModel) }
		
		
		// IMPLEMENTED  ----------------------
		
		override def body: Either[Value, Body] = Left(bodyValue)
		
		override def deprecated: Boolean = deprecationFlag.value
		
		override def send(prepared: PreparedRequest): Future[RequestResult[A]] = sendFunction(prepared)
	}
}

/**
  * Common trait for API requests which are persisted between sessions
  * @author Mikko Hilpinen
  * @since 16.07.2024, v1.8
  */
trait PersistingApiRequest[+A] extends ApiRequest[A] with Persisting