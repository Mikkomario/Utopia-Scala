package utopia.disciple.http.response

import utopia.access.http.ContentCategory.{Application, Text}
import utopia.access.http.StatusGroup.{ClientError, Redirect, ServerError}
import utopia.access.http.{ContentCategory, ContentType, Headers, Status, StatusGroup}
import utopia.disciple.http.response.ResponseParser.{DelegateEmptyResponseParser, EnhancingResponseParser, MappingResponseParser}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.parse.AutoClose._
import utopia.flow.parse.json.{JsonParser, JsonReader}
import utopia.flow.parse.xml.{XmlElement, XmlReader}
import utopia.flow.util.logging.Logger

import java.io.InputStream
import java.nio.charset.{Charset, StandardCharsets}
import scala.concurrent.ExecutionContext
import scala.io.{Codec, Source}
import scala.util.{Failure, Success, Try}

object ResponseParser
{
	// ATTRIBUTES   ------------------
	
	/**
	  * A response parser which doesn't perform any parsing and doesn't yield any result
	  */
	lazy val empty = static(())
	
	/**
	  * A response parser for extracting string contents.
	  * Assumes UTF-8 encoding when no other encoding has been specified
	  */
	lazy val string = encodedString(Codec.UTF8)
	/**
	  * A response parser for parsing the data to XML.
	  * Assumes UTF-8 encoding when no other encoding has been specified
	  */
	lazy val xml = encodedXml(StandardCharsets.UTF_8)
	
	
	// COMPUTED ----------------------
	
	/**
	  * @param log Implicit logging implementation to use for recording parsing failures
	  * @return A response parser for extracting string contents.
	  *         Assumes UTF-8 encoding when no other encoding has been specified.
	  *         Logs parsing failures and replaces them with an empty string.
	  */
	def stringOrLog(implicit log: Logger) = string.getOrElseLog("")
	
	/**
	  * @param jsonParser Implicit json parser to use
	  * @return A response parser which parses response contents into values.
	  *         Expects json responses, but also functions with XML and text-based responses.
	  */
	def value(implicit jsonParser: JsonParser) = jsonUsing(jsonParser)
	/**
	  * @param jsonParser Implicit json parser to use
	  * @param log Implicit logging implementation for recording parsing failures
	  * @return A response parser which parses response contents into values.
	  *         Expects json responses, but also functions with XML and text-based responses.
	  *         Logs parsing failures and replaces them with empty values.
	  */
	def valueOrLog(implicit jsonParser: JsonParser, log: Logger) =
		value.getOrElseLog(Value.empty)
	/**
	  * @param jsonParser Implicit json parser to use
	  * @return A response parser which parses response contents into 0-n values.
	  *         Expects json responses, but also functions with XML and text-based responses.
	  */
	def valueVector(implicit jsonParser: JsonParser) =
		value.map { _.flatMap { _.tryVector } }
	/**
	  * @param jsonParser Implicit json parser to use
	  * @param log Implicit logging implementation for recording parsing failures
	  * @return A response parser which parses response contents into 0-n values.
	  *         Expects json responses, but also functions with XML and text-based responses.
	  *         Logs parsing failures and replaces them with empty values.
	  */
	def valueVectorOrLog(implicit jsonParser: JsonParser, log: Logger) =
		valueVector.getOrElseLog(Vector.empty)
	
	/**
	  * @param jsonParser Implicit json parser to use
	  * @return A response parser which parses response contents into models.
	  *         Expects json responses, but also functions with XML responses.
	  */
	def model(implicit jsonParser: JsonParser) = value.map { _.flatMap { _.tryModel } }
	/**
	  * @param jsonParser Implicit json parser to use
	  * @param log Implicit logging implementation for recording parsing failures
	  * @return A response parser which parses response contents into models.
	  *         Expects json responses, but also functions with XML responses.
	  *         Logs parsing failures and replaces them with empty values.
	  */
	def modelOrLog(implicit jsonParser: JsonParser, log: Logger) =
		model.getOrElseLog(Model.empty)
	/**
	  * @param jsonParser Implicit json parser to use
	  * @return A response parser which parses response contents into 0-n models.
	  */
	def modelVector(implicit jsonParser: JsonParser) =
		value.map { _.flatMap { _.tryVectorWith { _.tryModel } } }
	/**
	  * @param jsonParser jsonParser Implicit json parser to use
	  * @param log Implicit logging implementation for recording parsing failures
	  * @return A response parser which parses response contents into 0-n models.
	  *         Logs failures.
	  */
	def modelVectorOrLog(implicit jsonParser: JsonParser, log: Logger) =
		mapValueVectorOrLog { _.tryModel }
	
	
	// OTHER    ----------------------
	
	/**
	  * Creates a new response parser by wrapping a function
	  * @param f A function that accepts 3 parameters:
	  *             1. Response status
	  *             1. Response headers
	  *             1. Response body as an input stream. None if the response is empty.
	  *
	  *          And yields a response parse result.
	  * @tparam A Type of the parsed results
	  * @return A new response parser which utilizes the specified function
	  */
	def apply[A](f: (Status, Headers, Option[InputStream]) => ResponseParseResult[A]): ResponseParser[A] =
		new _ResponseParser(f)
	
	/**
	  * Creates a new buffering response parser by wrapping a function.
	  * The received response is fully processed and cached within this parser.
	  * @param f A function that accepts 3 parameters:
	  *             1. Response status
	  *             1. Response headers
	  *             1. Response body as an input stream. None if the response is empty.
	  *
	  *          And yields the processed / buffered response value.
	  * @tparam A Type of the function result
	  * @return A new buffering response parser which utilizes the specified function
	  */
	def blocking[A](f: (Status, Headers, Option[InputStream]) => A) =
		apply { (status, headers, stream) => ResponseParseResult.buffered(f(status, headers, stream)) }
	/**
	  * Creates a new response parser by wrapping a function.
	  * The specified function is ran asynchronously.
	  * @param f A function that accepts 3 parameters:
	  *             1. Response status
	  *             1. Response headers
	  *             1. Response body as an input stream. None if the response is empty.
	  *
	  *          And yields a response parse result.
	  * @tparam A Type of the parsed results
	  * @return A new response parser which calls the specified function asynchronously.
	  */
	def async[A](f: (Status, Headers, Option[InputStream]) => A)(implicit exc: ExecutionContext) =
		apply { (status, headers, stream) => ResponseParseResult.async(f(status, headers, stream)) }
	
	/**
	  * Creates a new response parser that delegates successes to one parser and failures to another.
	  * Wraps the parse result into either Left (failure) or Right (success).
	  * @param successParser Parser used for processing successful responses
	  * @param failureParser Parser used for processing failure responses
	  * @tparam S Type of successful parse results
	  * @tparam F Type of failed parse results
	  * @return A response parser that parses the responses using either of these two parsers
	  */
	def eitherSuccessOrFailure[S, F](successParser: ResponseParser[S],
	                                 failureParser: ResponseParser[F]): ResponseParser[Either[F, S]] =
		new EitherSuccessOrFailureResponseParser(successParser, failureParser)
	
	/**
	  * Creates a response parser which delegates the parsing to one or more other response parsers
	  * @param f A function which accepts:
	  *             1. Response status
	  *             1. Response headers
	  *
	  *          And returns the parser which should be used in that context
	  * @tparam A Type of the parse results
	  * @return A response parser which uses the specified function to distributes response parsing between
	  *         other parsers.
	  */
	def delegating[A](f: (Status, Headers) => ResponseParser[A]): ResponseParser[A] =
		new DivergingResponseParser[A](f)
	
	/**
	  * @param defaultEncoding Encoding to assume when no encoding has been specified within the response
	  * @return A response parser which parses the response body into a string
	  */
	def encodedString(defaultEncoding: Codec): ResponseParser[Try[String]] = new ParseString(defaultEncoding)
	
	/**
	  * @param primaryParser Primary json parser to utilize
	  * @param moreParsers Additional json parsers to use for different encodings
	  * @param defaultEncoding Implicit encoding to assume when no encoding information
	  *                        has been specified in the response
	  * @return A response parser which parses response bodies into [[Value]]s.
	  *         Expects json content, but also functions with xml and text-based content.
	  */
	def jsonUsing(primaryParser: JsonParser, moreParsers: JsonParser*)
	             (implicit defaultEncoding: Codec): ResponseParser[Try[Value]] =
		new ParseValue(primaryParser +: moreParsers)
	
	/**
	  * @param defaultCharset Character set to assume when no encoding information is present in the response headers
	  * @return A response parser which parses the response bodies into XML
	  */
	def encodedXml(defaultCharset: Charset): ResponseParser[Try[XmlElement]] = new ParseXml(defaultCharset)
	
	/**
	  * Creates a response parser which converts read data to models (presumably from json)
	  * and parses those models using the specified parser.
	  * @param parser A parser which converts models into other data types
	  * @param jsonParser Implicit json parser to use
	  * @tparam A Type of the parsed values
	  * @return A response parser which yields items parsed from individual models
	  */
	def apply[A](parser: FromModelFactory[A])(implicit jsonParser: JsonParser): ResponseParser[Try[A]] =
		value.map { _.flatMap { _.tryModel.flatMap(parser.apply) } }
	/**
	  * Creates a response parser which converts read data to 0-n models per response (presumably from json)
	  * and parses those models using the specified parser.
	  * @param parser A parser which converts models into other data types
	  * @param jsonParser Implicit json parser to use
	  * @tparam A Type of the parsed values
	  * @return A response parser which yields 0-n items per response, parsed from models or model vectors
	  */
	def vector[A](parser: FromModelFactory[A])(implicit jsonParser: JsonParser) =
		value.map { _.flatMap { _.tryVectorWith { _.tryModel.flatMap(parser.apply) } } }
	/**
	  * Creates a response parser which converts read data to 0-n models per response (presumably from json)
	  * and parses those models using the specified parser.
	  * Logs parsing failures.
	  * @param parser A parser which converts models into other data types
	  * @param jsonParser jsonParser Implicit json parser to use
	  * @param log Implicit logging implementation for recording parsing failures
	  * @return A response parser which parses response contents into 0-n parsed items.
	  *         Logs failures.
	  */
	def vectorOrLog[A](parser: FromModelFactory[A])(implicit jsonParser: JsonParser, log: Logger) =
		mapValueVectorOrLog { _.tryModel.flatMap(parser.apply) }
	
	/**
	  * Converts the response body into a vector of values and finalizes the parsing by performing a mapping function.
	  * Logs parse failures. If parsing fails on an individual value, ignores that value.
	  * If parsing fails for the whole vector, replaces it with an empty vector instead.
	  * @param f A function which accepts a parsed value and converts it to the desired data type.
	  *          Yields a failure on parsing failures.
	  * @param jsonParser jsonParser Implicit json parser to use
	  * @param log Implicit logging implementation for recording parsing failures
	  * @return A response parser which parses response contents into 0-n parsed items.
	  *         Logs failures.
	  */
	def mapValueVectorOrLog[A](f: Value => Try[A])(implicit jsonParser: JsonParser, log: Logger) =
		value.map { value =>
			value.flatMap { _.tryVector } match {
				case Success(values) =>
					// Only logs parse failures and only returns successful parse results
					val (parseFailures, parsed) = values.map(f).divided
					parseFailures.headOption.foreach { log(_,
						s"Failed to parse ${ parseFailures.size }/${
							values.size } of the read values into the correct data type") }
					parsed
				
				// Case: The response body couldn't be even read into a value vector => Logs and returns an empty vector
				case Failure(error) =>
					log(error, "Failed to parse the response contents into a value vector")
					Vector.empty
			}
		}
	
	/**
	  * @param result Result which will be provided for all responses
	  * @tparam A Type of the result provided
	  * @return A response parser which provides a placeholder result for all responses
	  */
	def static[A](result: A): ResponseParser[A] = new StaticResponseParser[A](result)
	
	/**
	  * @param cause Cause of failure
	  * @tparam A Type of parse results, had they been successful
	  * @return A parser which always yields a failure, referring the specified cause
	  */
	def failure[A](cause: Throwable) = static(Failure[A](cause))
		
	
	// EXTENSIONS   ------------------
	
	implicit class TryResponseParser[A](val p: ResponseParser[Try[A]]) extends AnyVal
	{
		/**
		  * Handles possible parse failures using a logger
		  * @param default Default value to return in case of a parse failure
		  * @param log Implicit logging implementation to use
		  * @return Copy of this parser which logs and substitutes the default value in case of a parse failure
		  */
		def getOrElseLog(default: => A)(implicit log: Logger) = p.map { result =>
			result.getOrMap { error =>
				log(error, "Response-parsing failed")
				default
			}
		}
		
		/**
		  * @param f A mapping function applied to response parse results, if successful
		  * @tparam B Type of mapped response parse results
		  * @return Copy of this parser which also applies the specified mapping function on successes
		  */
		def mapSuccess[B](f: A => B) = p.map { _.map(f) }
		/**
		  * @param f A mapping function applied to response parse results, if successful.
		  *          May yield a failure.
		  * @tparam B Type of mapped response parse results, when successful
		  * @return Copy of this parser which also applies the specified mapping function on successes
		  */
		def flatMap[B](f: A => Try[B]) = p.map { _.flatMap(f) }
	}
	
	
	// NESTED   ----------------------
	
	private class _ResponseParser[+A](f: (Status, Headers, Option[InputStream]) => ResponseParseResult[A])
		extends ResponseParser[A]
	{
		override def apply(status: Status, headers: Headers, stream: Option[InputStream]) =
			f(status, headers, stream)
	}
	
	private class MappingResponseParser[A, B](original: ResponseParser[A], f: A => B) extends ResponseParser[B]
	{
		override def apply(status: Status, headers: Headers, stream: Option[InputStream]) =
			original(status, headers, stream).map(f)
	}
	
	private class EnhancingResponseParser[A, B](original: ResponseParser[A], f: (Status, Headers, A) => B)
		extends ResponseParser[B]
	{
		override def apply(status: Status, headers: Headers, stream: Option[InputStream]) =
			original(status, headers, stream).map { a => f(status, headers, a) }
	}
	
	private class StaticResponseParser[+A](staticResult: A) extends ResponseParser[A]
	{
		override def apply(status: Status, headers: Headers, stream: Option[InputStream]) =
			ResponseParseResult.buffered(staticResult)
	}
	
	private class DivergingResponseParser[A](f: (Status, Headers) => ResponseParser[A]) extends ResponseParser[A]
	{
		override def apply(status: Status, headers: Headers, stream: Option[InputStream]) =
			f(status, headers)(status, headers, stream)
	}
	
	private class EitherSuccessOrFailureResponseParser[S, F](successParser: ResponseParser[S],
	                                                         failureParser: ResponseParser[F])
		extends ResponseParser[Either[F, S]]
	{
		override def apply(status: Status, headers: Headers, stream: Option[InputStream]) =
		{
			if (status.isFailure)
				failureParser(status, headers, stream).map { Left(_) }
			else
				successParser(status, headers, stream).map { Right(_) }
		}
	}
	
	private class DelegateEmptyResponseParser[A](defaultParser: ResponseParser[A],
	                                             handleEmpty: (Status, Headers) => A)
		extends ResponseParser[A]
	{
		override def apply(status: Status, headers: Headers, stream: Option[InputStream]) = {
			if (stream.isEmpty)
				ResponseParseResult.buffered(handleEmpty(status, headers))
			else
				defaultParser(status, headers, stream)
		}
	}
	
	private class ParseString(defaultEncoding: Codec) extends ResponseParser[Try[String]]
	{
		override def apply(status: Status, headers: Headers, stream: Option[InputStream]) =
		{
			val res = stream match {
				case Some(stream) =>
					val encoding = headers.codec.getOrElse(defaultEncoding)
					Try { Source.fromInputStream(stream)(encoding).consume { _.mkString } }
				case None => Success("")
			}
			ResponseParseResult.buffered(res)
		}
	}
	
	private class ParseValue(jsonParsers: Iterable[JsonParser])
		extends ResponseParser[Try[Value]]
	{
		private lazy val defaultEncoding = jsonParsers.headOption match {
			case Some(parser) => parser.defaultEncoding
			case None => Codec.UTF8
		}
		
		override def apply(status: Status, headers: Headers, stream: Option[InputStream]) =
		{
			val value: Try[Value] = stream match {
				// Case: Non-empty response => Uses different parsing methods based on the response content type
				case Some(stream) =>
					val encoding = headers.codec.getOrElse(defaultEncoding)
					// Case: Json => Uses the json parser which matches the content's encoding
					if (headers.contentType.forall { _.subType ~== "json" }) {
						jsonParsers.find { _.defaultEncoding == encoding } match {
							case Some(parser) => parser(stream)
							case None => JsonReader(stream, encoding)
						}
					}
					// Case: Xml => Uses XmlReader to process the xml and then converts it into a model
					else if (headers.contentType.exists { _.subType ~== "xml" })
						XmlReader.parseStream(stream, encoding.charSet).map[Value] { _.toModel }
					// Case: Other => Converts the response body into a string
					else
						Try[Value] { Source.fromInputStream(stream)(encoding).consume { _.mkString } }
					
				// Case: Empty response => Returns an empty value
				case None => Success(Value.empty)
			}
			ResponseParseResult.buffered(value)
		}
	}
	
	private class ParseXml(defaultCharset: Charset) extends ResponseParser[Try[XmlElement]]
	{
		override def apply(status: Status, headers: Headers, stream: Option[InputStream]) =
		{
			val res = stream match {
				// Case: Non-empty response => Parses the response contents, assuming that its XML
				case Some(stream) =>
					val charset = headers.charset.getOrElse(defaultCharset)
					XmlReader.parseStream(stream, charset)
					
				// Case: Empty response => Fails
				case None => Failure(new IllegalArgumentException("Can't parse an empty response to XML"))
			}
			ResponseParseResult.buffered(res)
		}
	}
}

/**
  * An interface for processing streamed responses
  * @tparam A Type of parse results
  * @author Mikko Hilpinen
  * @since 12.07.2024, v1.7
  */
trait ResponseParser[+A]
{
	// ABSTRACT ----------------------
	
	/**
	  * Processes a response, parsing its contents.
	  * The parsing may be completed asynchronously or synchronously (i.e. blocking).
	  * @param status Response status
	  * @param headers Response headers
	  * @param stream Response body as a stream. None if the response was empty.
	  * @return 1) Immediately available result, and 2) a Future which resolves into the final result.
	  */
	def apply(status: Status, headers: Headers, stream: Option[InputStream] = None): ResponseParseResult[A]
	
	
	// OTHER    ---------------------
	
	/**
	  * Processes a response, parsing its contents.
	  * The parsing may be completed asynchronously or synchronously (i.e. blocking).
	  * @param status Response status
	  * @param headers Response headers
	  * @param stream Response body as a stream
	  * @return 1) Immediately available result, and 2) a Future which resolves into the final result.
	  */
	def apply(status: Status, headers: Headers, stream: InputStream): ResponseParseResult[A] =
		apply(status, headers, Some(stream))
	
	/**
	  * Creates a new parser which maps the results of this parser
	  * @param f A mapping function to apply to parse results
	  * @tparam B Type of mapping results
	  * @return Copy of this parser which further maps the parse results
	  */
	def map[B](f: A => B): ResponseParser[B] = new MappingResponseParser[A, B](this, f)
	/**
	  * Creates a new parser which maps the results of this parser,
	  * taking into consideration the response status and headers as well.
	  * @param f A mapping function applied to the parsing results of this parser.
	  *          Accepts 3 parameters:
	  *             1. Response status
	  *             1. Response headers
	  *             1. Result acquired from this parser
	  * @tparam B Type of the mapping results
	  * @return Copy of this parser which applies the specified mapping function on top of this parser's results
	  */
	def mapContextually[B](f: (Status, Headers, A) => B): ResponseParser[B] =
		new EnhancingResponseParser[A, B](this, f)
	
	/**
	  * Creates a copy of this parser which delegates empty responses to the specified function
	  * @param f A function for processing empty responses
	  * @tparam B Type of the function results
	  * @return Copy of this parser which, in case of an empty response, utilizes the specified function 'f'
	  */
	def handleEmptyWith[B >: A](f: (Status, Headers) => B): ResponseParser[B] =
		new DelegateEmptyResponseParser[B](this, f)
	/**
	  * Creates a copy of this parser which yields a default value in case of an empty response
	  * @param default Default value to yield in case of an empty response (call-by-name)
	  * @tparam B Type of the default value
	  * @return Copy of this parser which, in case of an empty response, yields the specified default value
	  */
	def withDefaultOnEmpty[B >: A](default: => B): ResponseParser[B] = handleEmptyWith { (_, _) => default }
	
	/**
	  * Creates a copy of this parser, which may delegate parsing to another parser instead,
	  * based on the response status and headers received.
	  * @param f A function which accepts the response status + headers and yields
	  *          a response parser to utilize or None, in case this parser should take on that response.
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which may delegate parsing to other parsers, using the specified function to
	  *         determine which parser will be used.
	  */
	def or[B >: A](f: (Status, Headers) => Option[ResponseParser[B]]): ResponseParser[B] =
		ResponseParser.delegating { (status, headers) => f(status, headers).getOrElse(this) }
	
	/**
	  * Creates a copy of this parser, which may delegate parsing to another parser instead,
	  * based on the response status.
	  * @param f A function which accepts the response status and yields
	  *          a response parser to utilize or None, in case this parser should take on that response.
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which may delegate parsing to other parsers, using the specified function to
	  *         determine which parser will be used.
	  */
	def usingStatusHandlers[B >: A](f: Status => Option[ResponseParser[B]]) =
		or { (status, _) => f(status) }
	
	/**
	  * Creates a copy of this parser, which delegates a certain response status group to a different parser instead
	  * @param statusGroup Http status group, for which the specified parser will be utilized
	  * @param parser Parser to use for responses with that status
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which delegates responses with the specified status group to the specified
	  *         parser instead of handling them directly
	  */
	def handleStatusGroupUsing[B >: A](statusGroup: StatusGroup, parser: ResponseParser[B]) =
		usingStatusHandlers { status => if (status.group == statusGroup) Some(parser) else None }
	/**
	  * Creates a copy of this parser, which delegates server-side error responses (5XX) to a different parser instead
	  * @param failureParser Parser to use for server-side error responses
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which delegates server-side error responses to the specified
	  *         parser instead of handling them directly
	  */
	def handleServerErrorsUsing[B >: A](failureParser: ResponseParser[B]) =
		handleStatusGroupUsing(ServerError, failureParser)
	/**
	  * Creates a copy of this parser, which delegates client-side error responses (4XX) to a different parser instead
	  * @param failureParser Parser to use for server-side error responses
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which delegates client-side error responses to the specified
	  *         parser instead of handling them directly
	  */
	def handleClientSideFailuresUsing[B >: A](failureParser: ResponseParser[B]) =
		handleStatusGroupUsing(ClientError, failureParser)
	/**
	  * Creates a copy of this parser, which delegates redirect responses (3XX) to a different parser instead
	  * @param redirectParser Parser to use for handling redirect responses
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which delegates redirect responses to the specified
	  *         parser instead of handling them directly
	  */
	def handleRedirectsUsing[B >: A](redirectParser: ResponseParser[B]) =
		handleStatusGroupUsing(Redirect, redirectParser)
	
	/**
	  * Creates a copy of this parser, which delegates responses with a specific status to a different parser instead
	  * @param status Http status, for which the specified parser will be utilized
	  * @param parser Parser to use for responses with that status
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which delegates responses with the specified status to the specified
	  *         parser instead of handling them directly
	  */
	def handleStatusUsing[B >: A](status: Status, parser: ResponseParser[B]) =
		usingStatusHandlers { s => if (s == status) Some(parser) else None }
	
	/**
	  * Creates a copy of this parser, which delegates failed requests (4XX-5XX) to a different parser instead
	  * @param failureParser Parser to use for failure responses
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which delegates failure responses to the specified
	  *         parser instead of handling them directly
	  */
	def handleFailuresUsing[B >: A](failureParser: ResponseParser[B]) =
		usingStatusHandlers[B] { status => if (status.isFailure) Some(failureParser) else None }
	/**
	  * Creates a copy of this parser, which uses a different processing logic to handle failed requests (4XX-5XX)
	  * @param f A function which accepts 3 values:
	  *             1. Response status
	  *             1. Response headers
	  *             1. Response body as a stream, if the response had a body
	  *
	  *          Returns a parsed (buffered) response value
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which uses the specified function to handle failure responses
	  *         instead of handling them directly
	  */
	def handleFailuresWith[B >: A](f: (Status, Headers, Option[InputStream]) => B) =
		handleFailuresUsing(ResponseParser.blocking(f))
	
	/**
	  * @param failureParser A parser used for processing failure responses
	  * @tparam F Type of parse results on failures
	  * @return A response parser which uses this parser for successes and the specified parser for failures.
	  *         Returns the values as Either where Left is failure and Right is success.
	  */
	def toRight[F](failureParser: ResponseParser[F]) =
		ResponseParser.eitherSuccessOrFailure(this, failureParser)
	/**
	  * @param f A parser function used for processing failure responses.
	  *          Accepts 3 values:
	  *             1. Response status
	  *             1. Response headers
	  *             1. Response body as a stream, if the response had a body
	  * @tparam F Type of parse results on failures
	  * @return A response parser which uses this parser for successes and the specified parser for failures.
	  *         Returns the values as Either where Left is failure and Right is success.
	  */
	def toRightWith[F](f: (Status, Headers, Option[InputStream]) => F) =
		toRight(ResponseParser.blocking(f))
	
	/**
	  * @param successParser A parser used for processing successful responses
	  * @tparam S Type of parse results on successes
	  * @return A response parser which uses this parser for failures and the specified parser for successes.
	  *         Returns the values as Either where Left is failure and Right is success.
	  */
	def toLeft[S](successParser: ResponseParser[S]) =
		ResponseParser.eitherSuccessOrFailure(successParser, this)
	
	/**
	  * Creates a copy of this parser, which may delegate parsing to another parser instead,
	  * based on the response content type.
	  * @param f A function which accepts the response content type and yields
	  *          a response parser to utilize for that type, or None, in case this parser should take on that response.
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which may delegate parsing to other parsers, using the specified function to
	  *         determine which parser will be used.
	  */
	def usingContentTypeHandlers[B >: A](f: ContentType => Option[ResponseParser[B]]) =
		or { (_, headers) => headers.contentType.flatMap(f) }
	/**
	  * Creates a copy of this parser, which delegates a certain content type category (such as audio or image) to
	  * another response parser.
	  * @param category Content type category which is delegated to the specified parser
	  * @param parser Response parser used to handle that content type
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which delegates responses with the specified content type to the specified parser
	  *         instead of handling them directly
	  */
	def handleContentCategoryUsing[B >: A](category: ContentCategory, parser: ResponseParser[B]) =
		usingContentTypeHandlers { t => if (t.category == category) Some(parser) else None }
	/**
	  * Creates a copy of this parser, which delegates a certain content type (e.g. json, xml, pdf, etc) to
	  * another response parser.
	  * @param contentType Content type which is delegated to the specified parser
	  * @param parser Response parser used to handle that content type
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which delegates responses with the specified content type to the specified parser
	  *         instead of handling them directly
	  */
	def handleContentTypeUsing[B >: A](contentType: ContentType, parser: ResponseParser[B]) =
		usingContentTypeHandlers { t => if (t == contentType) Some(parser) else None }
	
	/**
	  * Creates a copy of this parser, which handles xml responses using the specified function
	  * @param f A function which accepts 3 values:
	  *             1. Response status
	  *             1. Response headers
	  *             1. Response body XML contents (or a parse failure)
	  *
	  *          And yields the final response value to return
	  * @param defaultEncoding Encoding to assume in case the response doesn't specify character encoding (implicit)
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which delegates xml responses to the specified function
	  *         instead of handling them directly
	  */
	def handleXmlWith[B >: A](f: (Status, Headers, Try[XmlElement]) => B)(implicit defaultEncoding: Codec) =
		handleContentTypeUsing(Application.xml, ResponseParser.encodedXml(defaultEncoding.charSet).mapContextually(f))
	/**
	  * Creates a copy of this parser, which handles json responses using the specified function
	  * @param f A function which accepts 3 values:
	  *             1. Response status
	  *             1. Response headers
	  *             1. Response body contents as a value (or a parse failure)
	  *
	  *          And yields the final response value to return
	  * @param jsonParser Json parser to utilize (implicit)
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which delegates json responses to the specified function
	  *         instead of handling them directly
	  */
	def handleJsonWith[B >: A](f: (Status, Headers, Try[Value]) => B)(implicit jsonParser: JsonParser) =
		handleContentTypeUsing(Application.json, ResponseParser.value.mapContextually(f))
	/**
	  * Creates a copy of this parser, which handles plain text responses using the specified function
	  * @param f A function which accepts 3 values:
	  *             1. Response status
	  *             1. Response headers
	  *             1. Response body contents as a String (or a parse failure)
	  *
	  *          And yields the final response value to return
	  * @param defaultEncoding Encoding to assume in case the response doesn't specify character encoding (implicit)
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which delegates plain text responses to the specified function
	  *         instead of handling them directly
	  */
	def handlePlainTextWith[B >: A](f: (Status, Headers, Try[String]) => B)(implicit defaultEncoding: Codec) =
		handleContentTypeUsing(Text.plain, ResponseParser.encodedString(defaultEncoding).mapContextually(f))
}
