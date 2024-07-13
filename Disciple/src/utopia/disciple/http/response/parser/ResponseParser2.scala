package utopia.disciple.http.response.parser

import utopia.access.http.ContentCategory.{Application, Text}
import utopia.access.http.StatusGroup.{ClientError, Redirect, ServerError}
import utopia.access.http.{ContentCategory, ContentType, Headers, Status, StatusGroup}
import utopia.disciple.http.response.parser.ResponseParser2.{DelegateEmptyResponseParser, EnhancingResponseParser, MappingResponseParser}
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.parse.AutoClose._
import utopia.flow.parse.json.{JsonParser, JsonReader}
import utopia.flow.parse.xml.{XmlElement, XmlReader}

import java.io.InputStream
import java.nio.charset.{Charset, StandardCharsets}
import scala.concurrent.ExecutionContext
import scala.io.{Codec, Source}
import scala.util.{Failure, Success, Try}

object ResponseParser2
{
	// ATTRIBUTES   ------------------
	
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
	  * @param jsonParser Implicit json parser to use
	  * @return A response parser which parses response contents into values.
	  *         Expects json responses, but also functions with XML and text-based responses.
	  */
	def value(implicit jsonParser: JsonParser) = jsonUsing(jsonParser)
	/**
	  * @param jsonParser Implicit json parser to use
	  * @return A response parser which parses response contents into 0-n values.
	  *         Expects json responses, but also functions with XML and text-based responses.
	  */
	def valueVector(implicit jsonParser: JsonParser) =
		value.map { _.flatMap { _.tryVector } }
	
	/**
	  * @param jsonParser Implicit json parser to use
	  * @return A response parser which parses response contents into models.
	  *         Expects json responses, but also functions with XML responses.
	  */
	def model(implicit jsonParser: JsonParser) = value.map { _.flatMap { _.tryModel } }
	/**
	  * @param jsonParser Implicit json parser to use
	  * @return A response parser which parses response contents into 0-n models.
	  */
	def modelVector(implicit jsonParser: JsonParser) =
		value.map { _.flatMap { _.tryVectorWith { _.tryModel } } }
	
	
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
	def apply[A](f: (Status, Headers, Option[InputStream]) => ResponseParseResult[A]): ResponseParser2[A] =
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
	def delegating[A](f: (Status, Headers) => ResponseParser2[A]): ResponseParser2[A] =
		new DivergingResponseParser[A](f)
	
	/**
	  * @param defaultEncoding Encoding to assume when no encoding has been specified within the response
	  * @return A response parser which parses the response body into a string
	  */
	def encodedString(defaultEncoding: Codec): ResponseParser2[Try[String]] = new ParseString(defaultEncoding)
	
	/**
	  * @param primaryParser Primary json parser to utilize
	  * @param moreParsers Additional json parsers to use for different encodings
	  * @param defaultEncoding Implicit encoding to assume when no encoding information
	  *                        has been specified in the response
	  * @return A response parser which parses response bodies into [[Value]]s.
	  *         Expects json content, but also functions with xml and text-based content.
	  */
	def jsonUsing(primaryParser: JsonParser, moreParsers: JsonParser*)
	             (implicit defaultEncoding: Codec): ResponseParser2[Try[Value]] =
		new ParseValue(primaryParser +: moreParsers)
	
	/**
	  * @param defaultCharset Character set to assume when no encoding information is present in the response headers
	  * @return A response parser which parses the response bodies into XML
	  */
	def encodedXml(defaultCharset: Charset): ResponseParser2[Try[XmlElement]] = new ParseXml(defaultCharset)
	
	/**
	  * Creates a response parser which converts read data to models (presumably from json)
	  * and parses those models using the specified parser.
	  * @param parser A parser which converts models into other data types
	  * @param jsonParser Implicit json parser to use
	  * @tparam A Type of the parsed values
	  * @return A response parser which yields items parsed from individual models
	  */
	def apply[A](parser: FromModelFactory[A])(implicit jsonParser: JsonParser): ResponseParser2[Try[A]] =
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
	
	
	// NESTED   ----------------------
	
	private class _ResponseParser[+A](f: (Status, Headers, Option[InputStream]) => ResponseParseResult[A])
		extends ResponseParser2[A]
	{
		override def apply(status: Status, headers: Headers, stream: Option[InputStream]) =
			f(status, headers, stream)
	}
	
	private class MappingResponseParser[A, B](original: ResponseParser2[A], f: A => B) extends ResponseParser2[B]
	{
		override def apply(status: Status, headers: Headers, stream: Option[InputStream]) =
			original(status, headers, stream).map(f)
	}
	
	private class EnhancingResponseParser[A, B](original: ResponseParser2[A], f: (Status, Headers, A) => B)
		extends ResponseParser2[B]
	{
		override def apply(status: Status, headers: Headers, stream: Option[InputStream]) =
			original(status, headers, stream).map { a => f(status, headers, a) }
	}
	
	private class DivergingResponseParser[A](f: (Status, Headers) => ResponseParser2[A]) extends ResponseParser2[A]
	{
		override def apply(status: Status, headers: Headers, stream: Option[InputStream]) =
			f(status, headers)(status, headers, stream)
	}
	
	private class DelegateEmptyResponseParser[A](defaultParser: ResponseParser2[A],
	                                             handleEmpty: (Status, Headers) => A)
		extends ResponseParser2[A]
	{
		override def apply(status: Status, headers: Headers, stream: Option[InputStream]) = {
			if (stream.isEmpty)
				ResponseParseResult.buffered(handleEmpty(status, headers))
			else
				defaultParser(status, headers, stream)
		}
	}
	
	private class ParseString(defaultEncoding: Codec) extends ResponseParser2[Try[String]]
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
		extends ResponseParser2[Try[Value]]
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
	
	private class ParseXml(defaultCharset: Charset) extends ResponseParser2[Try[XmlElement]]
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
trait ResponseParser2[+A]
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
	def map[B](f: A => B): ResponseParser2[B] = new MappingResponseParser[A, B](this, f)
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
	def mapContextually[B](f: (Status, Headers, A) => B): ResponseParser2[B] =
		new EnhancingResponseParser[A, B](this, f)
	
	/**
	  * Creates a copy of this parser which delegates empty responses to the specified function
	  * @param f A function for processing empty responses
	  * @tparam B Type of the function results
	  * @return Copy of this parser which, in case of an empty response, utilizes the specified function 'f'
	  */
	def handleEmptyWith[B >: A](f: (Status, Headers) => B): ResponseParser2[B] =
		new DelegateEmptyResponseParser[B](this, f)
	/**
	  * Creates a copy of this parser which yields a default value in case of an empty response
	  * @param default Default value to yield in case of an empty response (call-by-name)
	  * @tparam B Type of the default value
	  * @return Copy of this parser which, in case of an empty response, yields the specified default value
	  */
	def withDefaultOnEmpty[B >: A](default: => B): ResponseParser2[B] = handleEmptyWith { (_, _) => default }
	
	/**
	  * Creates a copy of this parser, which may delegate parsing to another parser instead,
	  * based on the response status and headers received.
	  * @param f A function which accepts the response status + headers and yields
	  *          a response parser to utilize or None, in case this parser should take on that response.
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which may delegate parsing to other parsers, using the specified function to
	  *         determine which parser will be used.
	  */
	def or[B >: A](f: (Status, Headers) => Option[ResponseParser2[B]]): ResponseParser2[B] =
		ResponseParser2.delegating { (status, headers) => f(status, headers).getOrElse(this) }
	
	/**
	  * Creates a copy of this parser, which may delegate parsing to another parser instead,
	  * based on the response status.
	  * @param f A function which accepts the response status and yields
	  *          a response parser to utilize or None, in case this parser should take on that response.
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which may delegate parsing to other parsers, using the specified function to
	  *         determine which parser will be used.
	  */
	def usingStatusHandlers[B >: A](f: Status => Option[ResponseParser2[B]]) =
		or { (status, _) => f(status) }
	
	/**
	  * Creates a copy of this parser, which delegates a certain response status group to a different parser instead
	  * @param statusGroup Http status group, for which the specified parser will be utilized
	  * @param parser Parser to use for responses with that status
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which delegates responses with the specified status group to the specified
	  *         parser instead of handling them directly
	  */
	def handleStatusGroupUsing[B >: A](statusGroup: StatusGroup, parser: ResponseParser2[B]) =
		usingStatusHandlers { status => if (status.group == statusGroup) Some(parser) else None }
	/**
	  * Creates a copy of this parser, which delegates server-side error responses (5XX) to a different parser instead
	  * @param failureParser Parser to use for server-side error responses
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which delegates server-side error responses to the specified
	  *         parser instead of handling them directly
	  */
	def handleServerErrorsUsing[B >: A](failureParser: ResponseParser2[B]) =
		handleStatusGroupUsing(ServerError, failureParser)
	/**
	  * Creates a copy of this parser, which delegates client-side error responses (4XX) to a different parser instead
	  * @param failureParser Parser to use for server-side error responses
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which delegates client-side error responses to the specified
	  *         parser instead of handling them directly
	  */
	def handleClientSideFailuresUsing[B >: A](failureParser: ResponseParser2[B]) =
		handleStatusGroupUsing(ClientError, failureParser)
	/**
	  * Creates a copy of this parser, which delegates redirect responses (3XX) to a different parser instead
	  * @param redirectParser Parser to use for handling redirect responses
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which delegates redirect responses to the specified
	  *         parser instead of handling them directly
	  */
	def handleRedirectsUsing[B >: A](redirectParser: ResponseParser2[B]) =
		handleStatusGroupUsing(Redirect, redirectParser)
	
	/**
	  * Creates a copy of this parser, which delegates responses with a specific status to a different parser instead
	  * @param status Http status, for which the specified parser will be utilized
	  * @param parser Parser to use for responses with that status
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which delegates responses with the specified status to the specified
	  *         parser instead of handling them directly
	  */
	def handleStatusUsing[B >: A](status: Status, parser: ResponseParser2[B]) =
		usingStatusHandlers { s => if (s == status) Some(parser) else None }
	
	/**
	  * Creates a copy of this parser, which delegates failed requests (4XX-5XX) to a different parser instead
	  * @param failureParser Parser to use for failure responses
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which delegates failure responses to the specified
	  *         parser instead of handling them directly
	  */
	def handleFailuresUsing[B >: A](failureParser: ResponseParser2[B]) =
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
		handleFailuresUsing(ResponseParser2.blocking(f))
	
	/**
	  * Creates a copy of this parser, which may delegate parsing to another parser instead,
	  * based on the response content type.
	  * @param f A function which accepts the response content type and yields
	  *          a response parser to utilize for that type, or None, in case this parser should take on that response.
	  * @tparam B Type of the resulting parsing results
	  * @return Copy of this parser which may delegate parsing to other parsers, using the specified function to
	  *         determine which parser will be used.
	  */
	def usingContentTypeHandlers[B >: A](f: ContentType => Option[ResponseParser2[B]]) =
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
	def handleContentCategoryUsing[B >: A](category: ContentCategory, parser: ResponseParser2[B]) =
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
	def handleContentTypeUsing[B >: A](contentType: ContentType, parser: ResponseParser2[B]) =
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
		handleContentTypeUsing(Application.xml, ResponseParser2.encodedXml(defaultEncoding.charSet).mapContextually(f))
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
		handleContentTypeUsing(Application.json, ResponseParser2.value.mapContextually(f))
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
		handleContentTypeUsing(Text.plain, ResponseParser2.encodedString(defaultEncoding).mapContextually(f))
}
