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
	  * @param jsonParser Json parser used for interpreting request json content (implicit)
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
		if (request.body.isEmpty)
			Right(Value.empty)
		else
			request.body.buffered match {
				case Success(body) => Right(body.value)
				case Failure(error) =>
					Left(RequestResult(ResponseContent(error.getMessage, "Failed to parse the request body"),
						BadRequest))
			}
	}
	
	
	// IMPLEMENTED  ------------------------
	
	override def close(): Unit = request.body.value.close()
	
	
	// OTHER    ----------------------------
	
	/**
	  * Parses a value from the request body and uses it to produce a response
	  * @param f Function that will be called if the value was successfully read.
	  *          Accepts the read value, which may be empty. Returns a http result.
	  * @return Function result
	  */
	def handlePossibleValuePost(f: Value => RequestResult) = lazyParsedRequestBody.value.leftOrMap(f)
	/**
	  * Parses a value from the request body and uses it to produce a response
	  * @param f Function that will be called if the value was successfully read and not empty.
	  *          Returns an http result.
	  * @return Function result or a failure result if no value could be read.
	  */
	def handleValuePost(f: Value => RequestResult) = handlePossibleValuePost { value =>
		// Fails on empty value
		if (value.isEmpty)
			BadRequest -> "Please specify a body in the request"
		else
			f(value)
	}
	/**
	  * Parses a model from the request body and uses it to produce a response
	  * @param parser Model parser
	  * @param f Function that will be called if the model was successfully parsed. Returns an http result.
	  * @tparam A Type of parsed model
	  * @return Function result or a failure result if no model could be parsed.
	  */
	def handlePost[A](parser: FromModelFactory[A])(f: A => RequestResult): RequestResult =
		handleValuePost { value =>
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
	/**
	  * Parses request body into a vector of values and handles them using the specified function.
	  * For non-array bodies, wraps the body in a vector.
	  * @param f Function that will be called if a json body was present. Accepts a vector of values. Returns result.
	  * @return Function result or a failure if no value could be read
	  */
	def handleArrayPost(f: Seq[Value] => RequestResult) =
		handlePossibleValuePost { v: Value => f(v.getVector) }
	/**
	 * Parses request body into a vector of models and handles them using the specified function. Non-vector bodies
	 * are wrapped in vectors, non-object elements are ignored.
	 * @param parser Parser used for parsing models into objects
	 * @param f Function called if all parsing succeeds
	 * @tparam A Type of parsed item
	 * @return Function result or failure in case of parsing failures
	 */
	def handleModelArrayPost[A](parser: FromModelFactory[A])(f: Seq[A] => RequestResult) =
		handleArrayPost { values =>
			values.tryMap { v => parser(v.getModel) } match {
				case Success(parsed) => f(parsed)
				case Failure(error) => BadRequest -> error.getMessage
			}
		}
	/**
	  * Processes the request body in three parts:
	  * 1) Preprocessing that modifies the input
	  * 2) Parsing that attempts to parse the input into a processed value (possibly failing)
	  * 3) Processing that interacts with the parsed value and yields the final result
	  * @param preProcess A function that receives the post body as a (potentially empty) value and yields a
	  *                   pre-processed value
	  * @param parse A function that accepts the pre-processed value and attempts to parse it into another type.
	  *              Yields a success or a failure.
	  * @param handle A function that accepts a successfully parsed value, along with the pre-processed input and
	  *               yields the final response.
	  * @tparam P Type of pre-processed input
	  * @tparam R Type of parsed value when parsing succeeds
	  * @return Failure result if parsing failed. Otherwise, the result of the 'handle' function.
	  */
	def handleInterceptedValuePost[P, R](preProcess: Value => P)(parse: P => Try[R])
	                                    (handle: (R, P) => RequestResult): RequestResult =
		handlePossibleValuePost { input =>
			val pre = preProcess(input)
			parse(pre) match {
				case Success(res) => handle(res, pre)
				case Failure(error) => BadRequest -> error.getMessage
			}
		}
	/**
	  * Processes the request body in three parts:
	  * 1) Preprocessing that modifies the input model
	  * 2) Parsing that attempts to parse a model into some item (possibly failing)
	  * 3) Processing that interacts with the parsed item and yields the final result
	  * @param preProcess A function that receives the post body as a (potentially empty) model and returns the
	  *                   model that will be passed to the 'parser'
	  * @param parser A factory that parses the pre-processed model into an item, possibly failing to do so
	  * @param handle A function that accepts a successfully parsed item, along with the pre-processed model and
	  *               yields the final response.
	  * @return Failure result if parsing failed. Otherwise, the result of the 'handle' function.
	  */
	def handleInterceptedPost[A](preProcess: Model => Model)(parser: FromModelFactory[A])
	                            (handle: (A, Model) => RequestResult) =
		handleInterceptedValuePost[Model, A] { v => preProcess(v.getModel) }(parser.apply)(handle)
}
