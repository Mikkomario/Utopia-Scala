package utopia.annex.controller

import utopia.access.http.{Headers, Method}
import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.{RequestResult, Response}
import utopia.disciple.http.request.Request
import utopia.disciple.http.response.ResponseParser
import utopia.flow.generic.model.immutable.{Model, Value}

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

/**
  * An interface used for sending requests to a server
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
@deprecated("Deprecated for removal. Please use ApiClient instead", "v1.8")
trait Api extends ApiClient
{
	// ABSTRACT	-------------------------
	
	/**
	  * @return Headers used by default
	  */
	protected def headers: Headers
	
	
	// IMPLEMENTED  ---------------------
	
	override def valueResponseParser: ResponseParser[Response[Value]] =
		PreparingResponseParser.wrap(ResponseParser.value.getOrElseLog(Value.empty))(errorMessageFromValue)
	
	override def emptyResponseParser: ResponseParser[Response[Unit]] =
		PreparingResponseParser.onlyRecordFailures(ResponseParser.value.map {
			case Success(v) => errorMessageFromValue(v)
			case Failure(error) =>
				log(error, "Response-parsing failed")
				"Failed to parse the response body"
		})
	
	override protected def modifyOutgoingHeaders(original: Headers): Headers = original ++ headers
	
	
	// OTHER	-------------------------
	
	/**
	  * @param request Request to send
	  * @return Response from server (asynchronous)
	  */
	def sendRequest(request: ApiRequest[Value]): Future[RequestResult[Value]] = send(request)
	/**
	  * Performs a request from server side
	  * @param request A request
	  * @return Response from server (asynchronous)
	  */
	def sendRequest(request: Request) = apply(request).getValue
	
	/**
	  * Sends a request to the server and wraps the response
	  * @param method Method used
	  * @param path Server address + uri targeted
	  * @param timeout Request timeout (default = infinite)
	  * @param body Request body (default = empty)
	  * @param params Request parameters (default = empty)
	  * @param modHeaders A function for modifying the request headers (default = no modification)
	  * @return Asynchronous server result
	  */
	protected def makeRequest(method: Method, path: String, timeout: Duration = Duration.Inf,
							  body: Value = Value.empty, params: Model = Model.empty,
							  modHeaders: Headers => Headers = h => h) =
		prepareRequest(method, path, body, params, headers, timeout).getValue
	
	private def errorMessageFromValue(body: Value) =
		body("error", "description", "message").stringOr(body.getString)
}
