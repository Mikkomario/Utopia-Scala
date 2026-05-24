package utopia.nexus.controller.api.context

import utopia.access.model.enumeration.Status.BadRequest
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.EitherExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.caching.Lazy
import utopia.nexus.model.request.Request.StreamedRequest
import utopia.nexus.model.request.{RequestContext, StreamOrReader}
import utopia.nexus.model.response.{RequestResult, ResponseContent}

import scala.util.{Failure, Success, Try}

object PostContext
{
	// OTHER    -------------------------
	
	/**
	  * Creates a new post context
	  * @param request Request to wrap
	  * @param log Implicit logging implementation. Used for logging stream-closing failures.
	  * @param jsonParser JSON parser used for interpreting request JSON content (implicit)
	  * @return A new request context
	  */
	def apply(request: StreamedRequest)(implicit log: Logger, jsonParser: JsonParser): PostContext =
		new PostContext(request)
}

/**
  * A request context that parses a post body from JSON, XML or text into a Value,
  * offering functions for processing those parsed values.
 *
  * Basically, using this context allows one to bypass the following phases in request processing:
  *     1. Validating that there is a request body present
  *     1. Checking the request body content type and parsing it into a value, accordingly
  *     1. Handling the cases where value to expected post object parsing fails
  *
 * @param request The request being processed
 * @param log Implicit logging implementation. Used for logging stream-closing failures.
 * @param jsonParser The JSON parser used in request parsing
 *
 * @author Mikko Hilpinen
  * @since 13.10.2022, v1.9
  */
// TODO: Add optional logging for parsing failures
class PostContext(override val request: StreamedRequest)(implicit log: Logger, jsonParser: JsonParser)
	extends RequestContext[StreamOrReader]
{
	// ATTRIBUTES   ------------------------
	
	/**
	 * Parses the request body into a value, caching the result.
	 * The value is initialized lazily, on-demand.
	 *
	 * Contains either:
	 *      - Right: The parsed request body as a [[Value]]
	 *      - Left: Parsing failure as a [[RequestResult]]
	 */
	val lazyParsedRequestBody = Lazy {
		request.bufferedValue match {
			case Success(body) => Right(body)
			case Failure(error) =>
				Left(RequestResult(ResponseContent(error.getMessage, "Failed to parse the request body"),
					BadRequest))
		}
	}
	
	
	// IMPLEMENTED  ------------------------
	
	override def close(): Unit = request.body.close()
	
	
	// OTHER    ----------------------------
	
	/**
	 * Processes the request body in three parts:
	 *     1. Preprocessing that modifies the input model
	 *     1. Parsing that attempts to parse a model into some item (possibly failing)
	 *     1. Processing that interacts with the parsed item and yields the final result
	 * @param preProcess A function that receives the post body as a (potentially empty) model and returns the
	 *                   model that will be passed to the 'parser'
	 * @param parser A factory that parses the pre-processed model into an item, possibly failing to do so
	 * @param handle A function that accepts a successfully parsed item, along with the pre-processed model and
	 *               yields the final response.
	 * @return Failure result if parsing failed. Otherwise, the result of the 'handle' function.
	 */
	@deprecated("Deprecated for removal. Please use .parseBody(...) using parser.preparingWith(...), or use .interceptAndParseBody(...)", "v2.0.1")
	def handleInterceptedPost[A](preProcess: Model => Model)(parser: FromModelFactory[A])
	                            (handle: (A, Model) => RequestResult) =
		interceptAndParseBody[Model, A] { v => preProcess(v.getModel) }(parser.apply)(handle)
	/**
	 * Processes the request body in three parts:
	 *     1. Preprocessing that modifies the body value
	 *     1. Parsing that attempts to parse the preprocessed body
	 *     1. Processing that interacts with the parsed item and yields the final result
	 * @param preprocess A function that receives the post body as a (potentially empty) value and yields a
	 *                   pre-processed value
	 * @param parse A function that accepts the pre-processed value and attempts to parse it into another type.
	 *              Yields a success or a failure.
	 * @param handle A function that accepts a successfully parsed value, along with the pre-processed input and
	 *               yields the final response.
	 * @tparam P Type of pre-processed input
	 * @tparam R Type of parsed value when parsing succeeds
	 * @return Failure result if parsing failed. Otherwise, the result of the 'handle' function.
	 */
	def interceptAndParseBody[P, R](preprocess: Value => P)(parse: P => Try[R])
	                               (handle: (R, P) => RequestResult): RequestResult =
		withPossiblyEmptyBody { input =>
			val prepared = preprocess(input)
			parse(prepared) match {
				case Success(parsed) => handle(parsed, prepared)
				case Failure(error) => BadRequest -> error.getMessage
			}
		}
	@deprecated("Renamed to .withParsedInterceptedBody(...)", "v2.0.1")
	def handleInterceptedValuePost[P, R](preProcess: Value => P)(parse: P => Try[R])
	                                    (handle: (R, P) => RequestResult): RequestResult =
		interceptAndParseBody(preProcess)(parse)(handle)
	
	/**
	 * Parses the request body (object) into a specific data type.
	 * @param parser Model parser used
	 * @param f A function that receives the parsed request body, and yields the result to send to the client
	 * @tparam A Type of the parsed body
	 * @return Result of 'f', or a failure if the body was empty or couldn't be parsed
	 */
	def parseBody[A](parser: FromModelFactory[A])(f: A => RequestResult): RequestResult =
		withBody { value =>
			value.tryModel match {
				case Success(model) =>
					parser(model) match {
						// Gives the parsed model to specified function
						case Success(parsed) => f(parsed)
						case Failure(error) => BadRequest -> error.getMessage
					}
				case Failure(error) =>
					RequestResult(
						ResponseContent(error.getMessage, "The request body couldn't be parsed into an object"),
						BadRequest)
			}
		}
	@deprecated("Renamed to .parseBody(...)", "v2.0.1")
	def handlePost[A](parser: FromModelFactory[A])(f: A => RequestResult): RequestResult = parseBody(parser)(f)
	/**
	 * Accesses the request body. Yields a failure if no request body was specified.
	 * @param f A function that receives the request body as a Value (not empty)
	 *          and yields the result to send to the client.
	 * @return Result of 'f', or a failure if request body was not specified or couldn't be parsed.
	 */
	def withBody(f: Value => RequestResult) = withPossiblyEmptyBody { value =>
		// Fails on empty value
		if (value.isEmpty)
			BadRequest -> "Please specify a request body"
		else
			f(value)
	}
	@deprecated("Renamed to .withBody(...)", "v2.0.1")
	def handleValuePost(f: Value => RequestResult) = withBody(f)
	
	/**
	 * Parses n objects from the request body.
	 * @param parser Parser used for parsing models/objects into a specific data type
	 * @param f A function called if parsing succeeds for every included object.
	 *          Yields the result to send to the client.
	 * @tparam A Type of parsed items.
	 * @return Result of 'f', or a failure if parsing failed at any point.
	 */
	def parseArrayBody[A](parser: FromModelFactory[A])(f: Seq[A] => RequestResult) =
		withArrayBody { values =>
			values.tryMapAll { v => parser(v.getModel) } match {
				case Success(parsed) => f(parsed)
				case Failure(error) => BadRequest -> error.getMessage
			}
		}
	@deprecated("Renamed to .parseArrayBody(...)", "v2.0.1")
	def handleModelArrayPost[A](parser: FromModelFactory[A])(f: Seq[A] => RequestResult) =
		parseArrayBody(parser)(f)
	/**
	 * Accesses the request body as a Vector of values. Non-array bodies are wrapped in a Vector.
	 * Yields a failure if request-body parsing fails.
	 * @param f A function that receives the parsed values from the response body.
	 *          Receives an empty array, if there was no request body.
	 *          Yields the request result to send to the client.
	 * @return Result of 'f', or a failure if body-parsing failed
	 */
	def withArrayBody(f: Seq[Value] => RequestResult) = withPossiblyEmptyBody { v: Value => f(v.getVector) }
	@deprecated("Renamed to .withArrayBody(...)", "v2.0.1")
	def handleArrayPost(f: Seq[Value] => RequestResult) = withArrayBody(f)
	
	/**
	 * Accesses the request body, if possible. Yields a failure on parse failures.
	 * @param f A function that receives the request body as a value, which is empty if no request body was specified.
	 *          Returns the request result to yield.
	 * @return Result of 'f', or a failure if body-parsing failed.
	 */
	def withPossiblyEmptyBody(f: Value => RequestResult) = lazyParsedRequestBody.value.leftOrMap(f)
	@deprecated("Renamed to .withBodyIfDefined(...)", "v2.0.1")
	def handlePossibleValuePost(f: Value => RequestResult) = withPossiblyEmptyBody(f)
}
