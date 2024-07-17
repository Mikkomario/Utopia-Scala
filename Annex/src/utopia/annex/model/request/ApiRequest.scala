package utopia.annex.model.request

import utopia.access.http.Method
import utopia.annex.controller.ApiClient.PreparedRequest
import utopia.annex.model.response.RequestResult
import utopia.flow.generic.model.immutable.Value

import scala.concurrent.Future

object ApiRequest
{
	// TYPES    ---------------------------
	
	/**
	  * A function for finalizing the request-sending process.
	  * Accepts a prepared request and yields a future with a correctly processed / parsed request result.
	  *
	  * Typically these functions apply response-parsing using the helper functions in [[PreparedRequest]].
	  */
	type Send[+A] = PreparedRequest => Future[RequestResult[A]]
	
	
	// COMPUTED ---------------------------
	
	/**
	  * @return An accessor to functions for creating persisting API-requests
	  */
	def persisting = PersistingApiRequest
	
	
	// OTHER    ---------------------------
	
	/**
	  * Creates a new API request
	  * @param method Method used in this request
	  * @param path Path to the targeted server-side resource
	  * @param body Response body to apply. Default = empty.
	  * @param deprecationCondition A function which yields true when/if this request should be retracted,
	  *                             if not yet sent.
	  *                             Default = always false.
	  * @param send A function which accepts a prepared request and finalizes the sending process,
	  *             applying correct response-parsing, etc.
	  * @tparam A Type of parsed response values
	  * @return A new API request
	  */
	def apply[A](method: Method, path: String, body: Value = Value.empty, deprecationCondition: => Boolean = false)
	            (send: Send[A]): ApiRequest[A] =
		new _ApiRequest[A](method, path, body, deprecationCondition)(send)
	
	/**
	  * Creates a new GET request
	  * @param path Path to the targeted server-side resource
	  * @param deprecationCondition A function which yields true when/if this request should be retracted,
	  *                             if not yet sent.
	  *                             Default = always false.
	  * @param send A function which accepts a prepared request and finalizes the sending process,
	  *             applying correct response-parsing, etc.
	  * @tparam A Type of parsed response values
	  * @return A new GET request
	  */
	def get[A](path: String, deprecationCondition: => Boolean = false)(send: Send[A]) =
		GetRequest(path, deprecationCondition)(send)
	
	/**
	  * Creates a new GET request, which doesn't parse / post-process responses.
	  * @param path Path to the targeted server-side resource
	  * @param deprecationCondition A function which yields true when/if this request should be retracted,
	  *                             if not yet sent.
	  *                             Default = always false.
	  * @return A new GET request for retrieving responses in Value format
	  */
	def getValue(path: String, deprecationCondition: => Boolean = false) =
		GetRequest.value(path, deprecationCondition)
	
	
	// NESTED   ---------------------------
	
	private class _ApiRequest[A](override val method: Method, override val path: String, override val body: Value,
	                             testDeprecation: => Boolean)
	                            (f: Send[A])
		extends ApiRequest[A]
	{
		override def deprecated: Boolean = testDeprecation
		
		override def send(prepared: PreparedRequest) = f(prepared)
	}
}

/**
  * Represents a request that may be sent out using an [[utopia.annex.controller.ApiClient]]
  * @tparam A type of the parsed request response body
  * @author Mikko Hilpinen
  * @since 16.6.2020, v1
  */
trait ApiRequest[+A] extends Retractable
{
	// ABSTRACT ----------------------------
	
	/**
	  * @return Request method
	  */
	def method: Method
	/**
	  * @return Request path (root path not included)
	  */
	def path: String
	/**
	  * @return Request body value. Empty value if no body should be sent
	  */
	def body: Value
	
	/**
	  * Finalizes a sending process (for this request), determining how the response body is handled
	  * @param prepared A prepared version of this request
	  * @return Future which resolves into a request result of teh correct type
	  */
	def send(prepared: PreparedRequest): Future[RequestResult[A]]
	
	
	// COMPUTED ---------------------------
	
	@deprecated("Deprecated for removal. Renamed to .deprecated.", "v1.7")
	def isDeprecated = deprecated
	
	
	// IMPLEMENTED  -----------------------
	
	override def toString = s"$method $path"
}
