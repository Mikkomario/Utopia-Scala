package utopia.annex.controller

import utopia.access.http.{Headers, Method}
import utopia.access.http.Method.{Get, Post}
import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.Response
import utopia.disciple.apache.Gateway
import utopia.disciple.http.request.{Body, Request, Timeout}
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.util.TimeExtensions._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

/**
  * An interface used for sending requests to a server
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
trait Api
{
	// ABSTRACT	-------------------------
	
	/**
	  * @return Domain address + the initial path common to all requests
	  */
	protected def rootPath: String
	
	/**
	  * @return Headers used by default
	  */
	protected def headers: Headers
	
	/**
	  * @param bodyContent Request body content (non-empty)
	  * @return A request body wrapping the specified content
	  */
	protected def makeRequestBody(bodyContent: Model[Constant]): Body
	
	
	// OTHER	-------------------------
	
	private def uri(path: String) = if (path.startsWith("/")) rootPath + path else s"$rootPath/$path"
	
	/**
	  * Reads data from server
	  * @param path Targeted path (after base uri)
	  * @param timeout Connection timeout duration (default = maximum timeout)
	  * @param params Included paramters (optional)
	  * @param headersMod A modifier function for passed headers (optional)
	  * @param context Execution context
	  * @return Response from server (asynchronous)
	  */
	def get[A](path: String, timeout: Duration = Duration.Inf, params: Model[Constant] = Model.empty,
			   headersMod: Headers => Headers = h => h)(implicit context: ExecutionContext) =
		makeRequest(Get, path, timeout, params = params, modHeaders = headersMod)
	
	/**
	  * Posts data to server side
	  * @param path Targeted path (after base uri)
	  * @param body Sent json body
	  * @param timeout Connection timeout duration (default = maximum timeout)
	  * @param method Request method used (default = Post)
	  * @param context Execution context
	  * @return Response from server (asynchronous)
	  */
	def post[A](path: String, body: Model[Constant], timeout: Duration = Duration.Inf, method: Method = Post)
			   (implicit context: ExecutionContext) = makeRequest(method, path, timeout, body)
	
	/**
	  * @param request Request to send
	  * @param exc Implicit execution context
	  * @return Response from server (asynchronous)
	  */
	def sendRequest(request: ApiRequest)(implicit exc: ExecutionContext): Future[Try[Response]] =
		makeRequest(request.method, request.path, body = request.body)
	
	/**
	  * Performs a request from server side
	  * @param request A request
	  * @param exc Execution context (implicit)
	  * @return Response from server (asynchronous)
	  */
	def sendRequest(request: Request)(implicit exc: ExecutionContext) =
		Gateway.valueResponseFor(request).map { _.map(Response.from) }
	
	private def makeRequest(method: Method, path: String, timeout: Duration = Duration.Inf,
							body: Model[Constant] = Model.empty, params: Model[Constant] = Model.empty,
							modHeaders: Headers => Headers = h => h)(implicit context: ExecutionContext) =
	{
		// Timeout is generated from the specified single duration
		val fullTimeout = timeout.finite match
		{
			case Some(time) => Timeout(time, time * 3, time * 6)
			case None => Timeout.empty
		}
		// Body may or may not be specified
		val request = Request(uri(path), method, params, modHeaders(headers),
			if (body.isEmpty) None else Some(makeRequestBody(body)), fullTimeout)
		sendRequest(request)
	}
}
