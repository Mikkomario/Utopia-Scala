package utopia.nexus.rest

import utopia.access.http.ContentCategory.{Application, Text}
import utopia.access.http.Status.BadRequest
import utopia.access.http.error.ContentTypeException
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.EitherExtensions._
import utopia.nexus.http.{Request, ServerSettings}
import utopia.nexus.result.{Result, ResultParser, UseRawJson}

import scala.util.{Failure, Success, Try}

object PostContext
{
	// OTHER    -------------------------
	
	/**
	  * Creates a new post context
	  * @param request Request to wrap
	  * @param resultParser Parser that determines what server responses should look like.
	  *                     Default = Use simple json bodies and http statuses.
	  * @param serverSettings Applied server settings (implicit)
	  * @param jsonParser Json parser used for interpreting request json content (implicit)
	  * @return A new request context
	  */
	def apply(request: Request, resultParser: ResultParser = UseRawJson)
	         (implicit serverSettings: ServerSettings, jsonParser: JsonParser): PostContext =
		new _PostContext(request, resultParser)
	
	
	// NESTED   -------------------------
	
	private class _PostContext(override val request: Request,
	                           override val resultParser: ResultParser = UseRawJson)
	                          (implicit override val settings: ServerSettings,
	                           override val jsonParser: JsonParser)
		extends PostContext
	{
		override def close() = ()
	}
}

/**
  * A request context that parses a post body from json, xml or text into a Value,
  * offering functions for processing those parsed values.
  * Basically, using this context allows one to bypass the following phases in request processing:
  * 1) Validating that there is a request body present
  * 2) Checking the request body content type and parsing it into a value, accordingly
  * 3) Handling the cases where value to expected post object parsing fails
  * @author Mikko Hilpinen
  * @since 13.10.2022, v1.9
  */
abstract class PostContext extends Context
{
	// ATTRIBUTES   ------------------------
	
	// Request body is cached, since streamed request bodies can only be read once
	// Either[Failure, Value]
	private lazy val parsedRequestBody = {
		request.body.headOption match {
			case Some(body) =>
				// Accepts json, xml and text content types
				val value = body.contentType.subType.toLowerCase match {
					case "json" => body.bufferedJson(jsonParser).contents
					case "xml" => body.bufferedXml.contents.map[Value] { _.toSimpleModel }
					case _ =>
						body.contentType.category match {
							case Text => body.bufferedToString.contents.map[Value] { s => s }
							case _ => Failure(ContentTypeException.notAccepted(body.contentType,
								Vector(Application.json, Application.xml, Text.plain)))
						}
				}
				value match {
					case Success(value) => Right(value)
					case Failure(error) => Left(Result.Failure(BadRequest, error.getMessage))
				}
			// Case: No request body specified => Uses an empty value
			case None => Right(Value.empty)
		}
	}
	
	
	// ABSTRACT ----------------------------
	
	/**
	  * @return The json parser used by this context
	  */
	def jsonParser: JsonParser
	
	
	// OTHER    ----------------------------
	
	/**
	  * Parses a value from the request body and uses it to produce a response
	  * @param f Function that will be called if the value was successfully read.
	  *          Accepts the read value, which may be empty. Returns a http result.
	  * @return Function result
	  */
	def handlePossibleValuePost(f: Value => Result) = parsedRequestBody.leftOrMap(f)
	/**
	  * Parses a value from the request body and uses it to produce a response
	  * @param f Function that will be called if the value was successfully read and not empty.
	  *          Returns an http result.
	  * @return Function result or a failure result if no value could be read.
	  */
	def handleValuePost(f: Value => Result) = {
		handlePossibleValuePost { value =>
			// Fails on empty value
			if (value.isEmpty)
				Result.Failure(BadRequest, "Please specify a body in the request")
			else
				f(value)
		}
	}
	/**
	  * Parses a model from the request body and uses it to produce a response
	  * @param parser Model parser
	  * @param f Function that will be called if the model was successfully parsed. Returns an http result.
	  * @tparam A Type of parsed model
	  * @return Function result or a failure result if no model could be parsed.
	  */
	def handlePost[A](parser: FromModelFactory[A])(f: A => Result): Result = {
		handleValuePost { value =>
			value.model match {
				case Some(model) =>
					parser(model) match {
						// Gives the parsed model to specified function
						case Success(parsed) => f(parsed)
						case Failure(error) => Result.Failure(BadRequest, error.getMessage)
					}
				case None => Result.Failure(BadRequest, "Please provide a json object in the request body")
			}
		}
	}
	/**
	  * Parses request body into a vector of values and handles them using the specified function.
	  * For non-array bodies, wraps the body in a vector.
	  * @param f Function that will be called if a json body was present. Accepts a vector of values. Returns result.
	  * @return Function result or a failure if no value could be read
	  */
	def handleArrayPost(f: Seq[Value] => Result) = handleValuePost { v: Value =>
		if (v.isEmpty)
			f(Empty)
		else
			v.vector match {
				case Some(vector) => f(vector)
				// Wraps the value into a vector if necessary
				case None => f(Single(v))
			}
	}
	/**
	  * Parses request body into a vector of models and handles them using the specified function. Non-vector bodies
	  * are wrapped in vectors, non-object elements are ignored.
	  * @param parser Parser function used for parsing models into objects
	  * @param f Function called if all parsing succeeds
	  * @tparam A Type of parsed item
	  * @return Function result or failure in case of parsing failures
	  */
	def handleModelArrayPost[A](parser: Model => Try[A])(f: Seq[A] => Result) = handleArrayPost { values =>
		values.tryMap { v => parser(v.getModel) } match {
			case Success(parsed) => f(parsed)
			case Failure(error) => Result.Failure(BadRequest, error.getMessage)
		}
	}
	/**
	  * Parses request body into a vector of models and handles them using the specified function. Non-vector bodies
	  * are wrapped in vectors, non-object elements are ignored.
	  * @param parser Parser used for parsing models into objects
	  * @param f Function called if all parsing succeeds
	  * @tparam A Type of parsed item
	  * @return Function result or failure in case of parsing failures
	  */
	def handleModelArrayPost[A](parser: FromModelFactory[A])(f: Seq[A] => Result): Result =
		handleModelArrayPost[A] { m: Model => parser(m) }(f)
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
	  * @return Failure result if parsing failed. Otherwise the result of the 'handle' function.
	  */
	def handleInterceptedValuePost[P, R](preProcess: Value => P)(parse: P => Try[R])(handle: (R, P) => Result): Result =
		handlePossibleValuePost { input =>
			val pre = preProcess(input)
			parse(pre) match {
				case Success(res) => handle(res, pre)
				case Failure(error) => Result.Failure(BadRequest, error.getMessage)
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
	  * @return Failure result if parsing failed. Otherwise the result of the 'handle' function.
	  */
	def handleInterceptedPost[A](preProcess: Model => Model)(parser: FromModelFactory[A])
	                            (handle: (A, Model) => Result) =
		handleInterceptedValuePost[Model, A] { v => preProcess(v.getModel) }(parser.apply)(handle)
}
