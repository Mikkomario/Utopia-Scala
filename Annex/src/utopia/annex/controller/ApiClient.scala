package utopia.annex.controller

import utopia.access.http.Method.{Get, Post}
import utopia.access.http.{Headers, Method, Status}
import utopia.annex.controller.ApiClient.PreparedRequest
import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.RequestNotSent.RequestSendingFailed
import utopia.annex.model.response.{RequestResult, Response}
import utopia.annex.util.ResponseParseExtensions._
import utopia.disciple.apache.Gateway
import utopia.disciple.http.request.{Body, Request, Timeout}
import utopia.disciple.http.response.ResponseParser
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object ApiClient
{
	/**
	  * Represents a request that has been prepared and may be sent.
	  * Provides an interface for receiving the response in various alternative forms.
	  * @param wrapped Wrapped request
	  */
	class PreparedRequest(api: ApiClient, wrapped: Request)(implicit exc: ExecutionContext, log: Logger)
	{
		// COMPUTED -----------------------
		
		/**
		  * Sends out this request and receives the response as one containing a [[Value]]
		  * @return Future which eventually resolves into a parsed response or a request failure
		  */
		def getValue = send(api.valueResponseParser)
		/**
		  * Sends out this request and receives the response as one containing a vector of [[Value]]s.
		  * @return Future which eventually resolves into a parsed response or a request failure
		  */
		def getValues = send(api.valueResponseParser.mapSuccess { _.getVector })
		
		/**
		  * Sends out this request and receives the response as one containing a [[Model]] (parsed from a [[Value]])
		  * @return Future which eventually resolves into a parsed response or a request failure
		  */
		def getModel = parseValue { _.tryModel }
		/**
		  * Sends out this request and receives the response as one containing a vector of [[Model]]s
		  * (parsed from a [[Value]]).
		  * @return Future which eventually resolves into a parsed response or a request failure
		  */
		def getModels = parseValue { _.tryVectorWith { _.tryModel } }
		/**
		  * Sends out this request and receives the response as one containing a [[Model]] (parsed from a [[Value]]).
		  * Empty values are replaced with None.
		  * @return Future which eventually resolves into a parsed response or a request failure
		  */
		def getModelOption = parseValueIfNotEmpty { _.tryModel }
		
		
		// OTHER    ---------------------------
		
		/**
		  * Sends out this request and receives the response as one containing a single parsed item.
		  * Parse failures are logged and converted into failure responses.
		  * @param parser A parser which converts read model into the desired data type
		  * @return Future which eventually resolves into a parsed response or a request failure
		  */
		def getOne[A](parser: FromModelFactory[A]) = send(api.parserFrom(parser))
		/**
		  * Sends out this request and receives the response as one containing a a vector of parsed items.
		  * Parse failures are logged and converted into failure responses.
		  * @param parser A parser which converts read models into the desired data type
		  * @param allowPartialFailures Whether the resulting Response should be a Success,
		  *                             even if some of the parse-operations fail.
		  *                             If all parse-operations fail, the response will always be a failure.
		  *                             Default = false = all parse-operations must succeed in order for
		  *                             the Response to be a Success.
		  * @return Future which eventually resolves into a parsed response or a request failure
		  */
		def getMany[A](parser: FromModelFactory[A], allowPartialFailures: Boolean = false) =
			send(api.multiParserFrom(parser, allowPartialFailures))
		/**
		  * Sends out this request and receives the response as one containing a single parsed item.
		  * Parse failures are logged and converted into failure responses.
		  * Empty responses are not parsed, but yield None instead.
		  * @param parser A parser which converts read model into the desired data type
		  * @return Future which eventually resolves into a parsed response, an empty response, or a request failure
		  */
		def getOption[A](parser: FromModelFactory[A]) =
			parseValueIfNotEmpty { _.tryModel.flatMap { parser(_) } }
		
		/**
		  * Sends out this request and receives a mapped response
		  * @param f A function which maps the successful response from type [[Value]] to the final type
		  * @tparam A Type of the final mapping result
		  * @return Future which eventually resolves into a parsed response or a request failure
		  */
		def mapValue[A](f: Value => A) = send(api.valueResponseParser.mapSuccess(f))
		/**
		  * Sends out this request and receives a mapped response.
		  * Expects the response to contain 0-n values which will then be mapped.
		  * @param f A function which maps a value from a successful response to the final type
		  * @tparam A Type of mapping results
		  * @return Future which eventually resolves into a parsed response or a request failure
		  */
		def mapValues[A](f: Value => A) =
			send(api.valueResponseParser.mapSuccess { _.getVector.map(f) })
		/**
		  * Sends out this request and receives a mapped response.
		  * Empty responses are not mapped, but yield None instead.
		  * @param f A function which maps the successful response from type [[Value]] to the final type.
		  *          Only called on non-empty values.
		  * @tparam A Type of the final mapping result
		  * @return Future which eventually resolves into a parsed response or a request failure
		  */
		def mapValueIfNotEmpty[A](f: Value => A) =
			send(api.valueResponseParser.mapSuccess { _.notEmpty.map(f) })
			
		/**
		  * Sends out this request and receives a parsed response.
		  * Parsing failures are converted to failure responses.
		  * @param parse A function which maps the successful response from type [[Value]] to the final type.
		  *              May yield a failure, in which case the response is converted into a failure response.
		  * @tparam A Type of the final mapping result, when successful
		  * @return Future which eventually resolves into a parsed response or a request failure
		  */
		def parseValue[A](parse: Value => Try[A]) = send(api.tryMapValueParser(parse))
		/**
		  * Sends out this request and receives a parsed response.
		  * Expects the response to contain 0-n values, which will then be parsed.
		  * Parsing failures are converted to failure responses.
		  * @param parse A function which maps individual values from a successful response.
		  *              May yield a failure, in which case the response is converted into a failure response.
		  * @tparam A Type of the final mapping result, when successful
		  * @return Future which eventually resolves into a parsed response or a request failure
		  */
		def parseValues[A](parse: Value => Try[A]) =
			parseValue { _.tryVectorWith(parse) }
		/**
		  * Sends out this request and receives a parsed response.
		  * Parsing failures are converted to failure responses.
		  * Empty responses are not parsed, but yield None instead.
		  * @param parse A function which maps the successful response from type [[Value]] to the final type.
		  *              May yield a failure, in which case the response is converted into a failure response.
		  *              Will only be called for non-empty values.
		  * @tparam A Type of the final mapping result, when successful
		  * @return Future which eventually resolves into a parsed response or a request failure
		  */
		def parseValueIfNotEmpty[A](parse: Value => Try[A]) =
			parseValue { value => if (value.isEmpty) Success(None) else parse(value).map { Some(_) } }
		
		/**
		  * Sends out this request and receives a parsed response.
		  * Converts the response body into a model and then applies the specified mapping function.
		  * @param f A function which maps the successful response from a [[Model]] to the final type
		  * @tparam A Type of the final mapping result
		  * @return Future which eventually resolves into a parsed response or a request failure
		  */
		def mapModel[A](f: Model => A) = parseValue { _.tryModel.map(f) }
		/**
		  * Sends out this request and receives a parsed response.
		  * Expects the response to contain 0-n json objects.
		  * Converts the response body into 0-n models
		  * and then applies the specified mapping function for each one of them.
		  * @param f A function which maps the successful response from a [[Model]] to the final type
		  * @tparam A Type of the final mapping result
		  * @return Future which eventually resolves into a parsed response or a request failure
		  */
		def mapModels[A](f: Model => A) =
			parseValue { _.tryVectorWith { _.tryModel.map(f) } }
		/**
		  * Sends out this request and receives a parsed response.
		  * Converts the response body into a model, unless the body is empty,
		  * and then applies the specified mapping function.
		  * Empty responses yield None.
		  * @param f A function which maps the successful response from a [[Model]] to the final type.
		  *          Will only be called for non-empty models.
		  * @tparam A Type of the final mapping result
		  * @return Future which eventually resolves into a parsed response or a request failure
		  */
		def mapModelIfPresent[A](f: Model => A) =
			parseValueIfNotEmpty { _.tryModel.map(f) }
		
		/**
		  * Sends out this request and receives a parsed response.
		  * Parsing failures are converted to failure responses.
		  * @param parser The parser used for pre-processing the response contents
		  * @param parse A function which maps the successful response from the pre-processed value to the final type.
		  *              May yield a failure, in which case the response is converted into a failure response.
		  * @param extractErrorMessage A function which extracts an error message from the pre-processed value.
		  *                            Only called for failure responses (where status is 400-599).
		  * @tparam A Type of the preliminary parsing result
		  * @tparam B Type of the final mapping result, when successful
		  * @return Future which eventually resolves into a parsed response or a request failure
		  */
		def tryParseSuccess[A, B](parser: ResponseParser[A])(parse: A => Try[B])(extractErrorMessage: A => String) =
			send(PreparingResponseParser.tryMap(parser, api.responseParseFailureStatus) {
				parse(_) }(extractErrorMessage))
		
		/**
		  * Sends this request to the server and acquires a response
		  * @param parser Parser used for processing the response
		  * @return Future which resolves into the eventual request send result
		  */
		def send[A](parser: ResponseParser[RequestResult[A]]) = {
			// Sends the request and handles possible request sending failures
			api.gateway.responseFor(wrapped)(parser).map {
				case Success(response) => response.body
				case Failure(error) => RequestSendingFailed(error)
			}
		}
		
		/**
		  * Sends out this request, not expecting the response to contain a response body.
		  * Handles failure message -extraction using instructions defined in the [[ApiClient]].
		  * @return Future which eventually resolves into the acquired response or a request failure
		  */
		def send(): Future[RequestResult[Unit]] = send(api.emptyResponseParser)
	}
}

/**
  * An interface used for sending requests to a server
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1 (rewritten in 14.7.2024, v1.8)
  */
trait ApiClient
{
	// ABSTRACT	-------------------------
	
	/**
	  * @return Implicit execution context used in asynchronous operations (mostly in asynchronous requests)
	  */
	protected implicit def exc: ExecutionContext
	/**
	  * @return A logging implementation used for recording parsing failures and non-critical errors
	  */
	protected implicit def log: Logger
	/**
	  * @return Json parsing implementation used when parsing response contents
	  */
	protected implicit def jsonParser: JsonParser
	
	/**
	  * @return A [[Gateway]] instance to use when making http requests
	  */
	protected def gateway: Gateway
	
	/**
	  * @return Domain address + the initial path common to all requests
	  */
	protected def rootPath: String
	
	/**
	  * @return Status assigned to responses where response body-parsing fails
	  */
	protected def responseParseFailureStatus: Status
	
	/**
	  * @return A response parser used for converting response contents into generic values
	  */
	def valueResponseParser: ResponseParser[Response[Value]]
	/**
	  * @return A response parser used when the responses are not expected to contain important content.
	  *         Should, however, implement error handling in case of failed responses.
	  */
	def emptyResponseParser: ResponseParser[Response[Unit]]
	
	/**
	  * This function is called to modify all outgoing headers.
	  * For example, this client may add authorization or some other information to these headers
	  * @param original Original, unmodified headers
	  * @return Modified copy of these headers
	  */
	protected def modifyOutgoingHeaders(original: Headers): Headers
	
	/**
	  * @param bodyContent Request body content (non-empty)
	  * @return A request body wrapping the specified content
	  */
	protected def makeRequestBody(bodyContent: Value): Body
	
	
	// OTHER	-------------------------
	
	private def uri(path: String) =
		if (path.startsWith("/") || rootPath.endsWith("/")) s"$rootPath$path" else s"$rootPath/$path"
	
	/**
	  * Prepares a GET request.
	  * Note: The request must still be sent before it can impact the server or retrieve data.
	  * @param path Path to the targeted resource on the server (server root path will be prepended to this)
	  * @param params Included parameters (default = empty)
	  * @param headers Headers to send out with the request
	  *                (standard modifications will be applied, also) (default = empty)
	  * @param timeout Connection timeout duration (default = maximum timeout)
	  * @return Prepared GET request
	  */
	def get(path: String, params: Model = Model.empty, headers: Headers = Headers.empty,
	        timeout: Duration = Duration.Inf) =
		prepareRequest(Get, path, params = params, headers = headers, timeout = timeout)
	/**
	  * Prepares a POST request (or a similar request, such as PUT).
	  * Note: The request must still be sent before it can impact the server or retrieve data.
	  * @param path Path to the targeted resource on the server (server root path will be prepended to this)
	  * @param body Body to assign to the outgoing request (default = empty)
	  * @param method The method used (default = POST)
	  * @param headers Headers to send out with the request
	  *                (standard modifications will be applied, also) (default = empty)
	  * @param timeout Connection timeout duration (default = maximum timeout)
	  * @return Prepared POST request
	  */
	def post(path: String, body: Either[Value, Body] = Left(Value.empty), method: Method = Post,
	         headers: Headers = Headers.empty, timeout: Duration = Duration.Inf) =
		prepareRequest(method, path, body, headers = headers, timeout = timeout)
	/**
	  * Prepares a POST request (or a similar request, such as PUT).
	  * Note: The request must still be sent before it can impact the server or retrieve data.
	  * @param path Path to the targeted resource on the server (server root path will be prepended to this)
	  * @param body Body to assign to the outgoing request (default = empty)
	  * @param method The method used (default = POST)
	  * @param headers Headers to send out with the request
	  *                (standard modifications will be applied, also) (default = empty)
	  * @param timeout Connection timeout duration (default = maximum timeout)
	  * @return Prepared POST request
	  */
	def postValue(path: String, body: Value = Value.empty, method: Method = Post, headers: Headers = Headers.empty,
	              timeout: Duration = Duration.Inf) =
		post(path, Left(body), method, headers, timeout)
	
	/**
	  * Sends out a request
	  * @param request Request to send out
	  * @return Future which resolves into the eventual request send result
	  */
	def send[A](request: ApiRequest[A]): Future[RequestResult[A]] =
		request.send(prepareRequest(request.method, request.path, request.body))
	/**
	  * Prepares a request for sending
	  * @param request Request to send out (will be modified)
	  * @return Prepared request
	  */
	def apply(request: Request) = new PreparedRequest(this, request.copy(
		requestUri = uri(request.requestUri), headers = modifyOutgoingHeaders(request.headers)))
	
	/**
	  * Converts a from model factory into a response parser.
	  * Utilizes the default response-to-value parser of this client.
	  *
	  * Parse failures are converted into failure responses, also logging them.
	  *
	  * @param fromModelParser A parser which converts read models into the desired data type
	  * @tparam A Type of the successful parsing results
	  * @return A response parser which yields responses with fully parsed items.
	  *         Parse failures are converted into failed responses.
	  */
	def parserFrom[A](fromModelParser: FromModelFactory[A]) =
		tryMapValueParser { _.tryModel.flatMap(fromModelParser.apply) }
	/**
	  * Converts a from model factory into a response parser which handles model arrays.
	  * Utilizes the default response-to-value parser of this client.
	  *
	  * Parse failures are converted into failure responses, also logging them.
	  *
	  * @param fromModelParser A parser which converts read models into the desired data type
	  * @param allowPartialFailures Whether the resulting Response should be a Success,
	  *                             even if some of the parse-operations fail.
	  *                             If all parse-operations fail, the response will always be a failure.
	  *                             Default = false = all parse-operations must succeed in order for
	  *                             the Response to be a Success.
	  * @tparam A Type of the successful parsing results
	  * @return A response parser which yields responses with 0-n fully parsed items each.
	  *         Parse failures are converted into failed responses.
	  */
	def multiParserFrom[A](fromModelParser: FromModelFactory[A],
	                       allowPartialFailures: Boolean = false) =
	{
		// Case: Partial failures allowed => Only logs errors, unless all parsing operations fail
		if (allowPartialFailures)
			tryMapValueParser { value =>
				value.tryVector.flatMap { values =>
					values.map { _.tryModel.flatMap(fromModelParser.apply) }
						.toTryCatch.logToTryWithMessage("Failed to parse some, but not all, of the response values")
				}
			}
		// Case: Partial failures not allowed => Fails if any parsing operation fails
		else
			tryMapValueParser { _.tryVectorWith { _.tryModel.flatMap(fromModelParser.apply) } }
	}
	
	/**
	  * Converts the default response-to-value parser of this interface into a further processing parser.
	  * Converts parsing failures to failed responses, also logging them.
	  * @param parse A function which accepts a generic [[Value]] and yields either the parsed item or
	  *              a failure, if parsing fails.
	  * @tparam A Type of successful parsing results
	  * @return A new parser which first processes the response into a value
	  *         and then post-processes it using the specified parsing function.
	  */
	def tryMapValueParser[A](parse: Value => Try[A]) =
		tryMapParser(valueResponseParser)(parse)
	/**
	  * Converts the default response-to-value parser of this interface into a further processing parser.
	  * Converts parsing failures to failed responses, also logging them.
	  * @param parser The original parser to convert
	  * @param parse A function which accepts a parsed value from the original and yields either the parsed item or
	  *              a failure, if parsing fails.
	  * @tparam A Type of original response values
	  * @tparam B Type of successful parsing results
	  * @return A new parser which first processes the response into a value
	  *         and then post-processes it using the specified parsing function.
	  */
	def tryMapParser[A, B](parser: ResponseParser[Response[A]])(parse: A => Try[B]) = {
		// Further modifies the acquire responses
		parser.map[Response[B]] {
			// Case: Successful response => Attempts to parse it
			case Response.Success(value, status, headers) =>
				parse(value) match {
					// Case: Parsing succeeded => Wraps the parse results
					case Success(parsed) => Response.Success(parsed, status, headers)
					
					// Case: Parsing failed => Converts into a failure response instead
					case Failure(error) =>
						// Logs the error, also
						val contentTypeStr = headers.contentType match {
							case Some(cType) => s"; Content-Type = $cType"
							case None => ""
						}
						log(error,
							s"Failed to parse the response contents into the desired data type; Status = $status$contentTypeStr")
						Response.Failure(responseParseFailureStatus, error.getMessage, headers)
				}
			
			// Case: Failure response => Won't attempt parsing
			case failure: Response.Failure => failure
		}
	}
	
	/**
	  * Sends a request to the server and wraps the response
	  * @param method Method used
	  * @param path Path to the targeted resources. Server root address will be prepended to this.
	  * @param body Request body (default = empty)
	  * @param params Request parameters (default = empty)
	  * @param headers Headers to send out with the request
	  *                (standard modifications will be applied, also) (default = empty)
	  * @param timeout Request timeout (default = infinite)
	  *
	  * @return Asynchronous server result
	  */
	protected def prepareRequest(method: Method, path: String, body: Either[Value, Body] = Left(Value.empty),
	                             params: Model = Model.empty, headers: Headers = Headers.empty,
	                             timeout: Duration = Duration.Inf) =
	{
		// Timeout is generated from the specified single duration
		val fullTimeout = timeout.finite match {
			case Some(time) => Timeout(time, time * 3, time * 6)
			case None => Timeout.empty
		}
		// Body may or may not be specified
		val preparedBody = body match {
			case Left(value) => value.notEmpty.map(makeRequestBody)
			case Right(body) => Some(body)
		}
		apply(Request(path, method, params, headers, preparedBody, fullTimeout))
	}
}
